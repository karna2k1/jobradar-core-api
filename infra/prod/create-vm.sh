#!/usr/bin/env bash
# Creates the JobRadar VM on Oracle Cloud (Always Free: Ampere A1, Ubuntu 24.04) with the OCI CLI:
# VCN + internet gateway + public subnet (ports 22/80/443 open) + VM. Safe to re-run: reuses what exists.
# Retries while the region reports "Out of host capacity" (common for free A1 shapes).
#
# Prereq (one browser login):  oci session authenticate --region ap-hyderabad-1 --profile-name jobradar
# Usage:  bash create-vm.sh [ssh-public-key-file]     env: OCI_PROFILE, OCPUS (4), MEM_GB (24), RETRY_SECONDS (60)
set -euo pipefail

PROFILE=${OCI_PROFILE:-jobradar}
SSH_KEY=${1:-$HOME/.ssh/jobradar_oracle.pub}
OCPUS=${OCPUS:-4}
MEM_GB=${MEM_GB:-24}
RETRY_SECONDS=${RETRY_SECONDS:-60}
NAME=jobradar

OCI_BIN=$(command -v oci || echo "/c/Program Files (x86)/Oracle/oci_cli/oci.exe")
oci() { "$OCI_BIN" --profile "$PROFILE" --auth security_token "$@" | tr -d '\r'; }
# Single value from a JMESPath query; empty string when nothing matches.
val() { oci "$@" --raw-output 2>/dev/null || true; }

[ -f "$SSH_KEY" ] || { echo "SSH public key not found: $SSH_KEY"; exit 1; }
C=$(awk -v p="[$PROFILE]" '$0==p{f=1;next} /^\[/{f=0} f&&/^tenancy=/{sub(/^tenancy=/,"");print}' ~/.oci/config | tr -d '\r')
[ -n "$C" ] || { echo "Profile $PROFILE not found in ~/.oci/config"; exit 1; }
AD=$(val iam availability-domain list -c "$C" --query 'data[0].name')
echo "Compartment: ${C:0:32}...  AD: $AD"

echo "== VCN"
VCN=$(val network vcn list -c "$C" --display-name "$NAME-vcn" --lifecycle-state AVAILABLE --query 'data[0].id')
if [ -z "$VCN" ]; then
  VCN=$(val network vcn create -c "$C" --display-name "$NAME-vcn" --cidr-blocks '["10.0.0.0/16"]' \
        --dns-label jobradar --wait-for-state AVAILABLE --query 'data.id')
fi

echo "== Internet gateway + default route"
IGW=$(val network internet-gateway list -c "$C" --vcn-id "$VCN" --display-name "$NAME-igw" --query 'data[0].id')
if [ -z "$IGW" ]; then
  IGW=$(val network internet-gateway create -c "$C" --vcn-id "$VCN" --display-name "$NAME-igw" \
        --is-enabled true --wait-for-state AVAILABLE --query 'data.id')
fi
RT=$(val network vcn get --vcn-id "$VCN" --query 'data."default-route-table-id"')
oci network route-table update --rt-id "$RT" --force --wait-for-state AVAILABLE \
  --route-rules "[{\"destination\":\"0.0.0.0/0\",\"destinationType\":\"CIDR_BLOCK\",\"networkEntityId\":\"$IGW\"}]" >/dev/null

echo "== Security list: SSH, HTTP, HTTPS in; everything out"
INGRESS='[
 {"source":"0.0.0.0/0","protocol":"6","tcpOptions":{"destinationPortRange":{"min":22,"max":22}}},
 {"source":"0.0.0.0/0","protocol":"6","tcpOptions":{"destinationPortRange":{"min":80,"max":80}}},
 {"source":"0.0.0.0/0","protocol":"6","tcpOptions":{"destinationPortRange":{"min":443,"max":443}}},
 {"source":"0.0.0.0/0","protocol":"1","icmpOptions":{"type":3,"code":4}}]'
EGRESS='[{"destination":"0.0.0.0/0","protocol":"all"}]'
SL=$(val network security-list list -c "$C" --vcn-id "$VCN" --display-name "$NAME-sl" --query 'data[0].id')
if [ -z "$SL" ]; then
  SL=$(val network security-list create -c "$C" --vcn-id "$VCN" --display-name "$NAME-sl" \
       --ingress-security-rules "$INGRESS" --egress-security-rules "$EGRESS" --wait-for-state AVAILABLE --query 'data.id')
fi

echo "== Public subnet"
SUBNET=$(val network subnet list -c "$C" --vcn-id "$VCN" --display-name "$NAME-public" --query 'data[0].id')
if [ -z "$SUBNET" ]; then
  SUBNET=$(val network subnet create -c "$C" --vcn-id "$VCN" --display-name "$NAME-public" --cidr-block 10.0.0.0/24 \
           --dns-label public --security-list-ids "[\"$SL\"]" --route-table-id "$RT" \
           --wait-for-state AVAILABLE --query 'data.id')
fi

echo "== VM ($OCPUS OCPU / $MEM_GB GB, Ubuntu 24.04 aarch64)"
VM=$(val compute instance list -c "$C" --display-name "$NAME" \
     --query 'data[?"lifecycle-state"!=`TERMINATED` && "lifecycle-state"!=`TERMINATING`] | [0].id')
if [ -z "$VM" ]; then
  IMAGE=$(val compute image list -c "$C" --operating-system "Canonical Ubuntu" --operating-system-version 24.04 \
          --shape VM.Standard.A1.Flex --sort-by TIMECREATED --query 'data[0].id')
  attempt=1
  while :; do
    echo "  launch attempt $attempt"
    if OUT=$("$OCI_BIN" --profile "$PROFILE" --auth security_token compute instance launch -c "$C" \
          --availability-domain "$AD" --display-name "$NAME" --shape VM.Standard.A1.Flex \
          --shape-config "{\"ocpus\":$OCPUS,\"memoryInGBs\":$MEM_GB}" --image-id "$IMAGE" \
          --subnet-id "$SUBNET" --assign-public-ip true --boot-volume-size-in-gbs 50 \
          --ssh-authorized-keys-file "$SSH_KEY" --wait-for-state RUNNING \
          --query 'data.id' --raw-output 2>&1); then
      VM=$(echo "$OUT" | tr -d '\r' | grep -o 'ocid1\.instance[^ ]*' | tail -1); break
    fi
    # Free A1 launches fail with "Out of host capacity" (500) or "TooManyRequests" (429) when the region is full.
    if echo "$OUT" | grep -qiE 'capacity|TooManyRequests|"status": 429'; then
      echo "  $(date +%H:%M) no free A1 capacity yet, retrying in ${RETRY_SECONDS}s"
      sleep "$RETRY_SECONDS"; attempt=$((attempt+1))
      "$OCI_BIN" session refresh --profile "$PROFILE" >/dev/null 2>&1 || true   # keep the 1h login alive
    else
      echo "$OUT"; exit 1
    fi
  done
fi

IP=$(val compute instance list-vnics --instance-id "$VM" --query 'data[0]."public-ip"')
echo
echo "VM ready: $IP   (DOMAIN_SUFFIX=${IP//./-}.sslip.io)"
echo "  ssh -i ${SSH_KEY%.pub} ubuntu@$IP"

#!/usr/bin/env bash
# One-time setup of a fresh Oracle Cloud Ubuntu VM: Docker, firewall, repos.
# Run as the default "ubuntu" user:  bash setup-vm.sh
set -euo pipefail

echo "== Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo "== Firewall: Oracle's Ubuntu image blocks everything but SSH in iptables; open 80/443"
sudo iptables -C INPUT -p tcp --dport 80 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT 6 -p tcp --dport 80 -j ACCEPT
sudo iptables -C INPUT -p tcp --dport 443 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT 6 -p tcp --dport 443 -j ACCEPT
sudo apt-get install -y -q iptables-persistent >/dev/null
sudo netfilter-persistent save

echo "== Swap (helps Maven builds on small shapes)"
if ! swapon --show | grep -q swapfile; then
  sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo "== Repos"
mkdir -p ~/jobradar && cd ~/jobradar
for r in jobradar-core-api job-ingest-service match-notify-service; do
  [ -d "$r" ] || git clone "https://github.com/karna2k1/$r.git"
done

echo
echo "Done. Next: log out and back in (docker group), then:"
echo "  cd ~/jobradar/jobradar-core-api/infra/prod && cp .env.prod.example .env.prod && nano .env.prod && ./deploy.sh"

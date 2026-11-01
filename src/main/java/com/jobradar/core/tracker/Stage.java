package com.jobradar.core.tracker;

/** Kanban columns, in board order. SUGGESTED cards come from match-notify-service (job.matched). */
public enum Stage {
    SUGGESTED, SAVED, APPLIED, INTERVIEW, OFFER, REJECTED
}

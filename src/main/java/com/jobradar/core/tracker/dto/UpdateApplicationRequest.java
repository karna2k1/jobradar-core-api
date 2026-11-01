package com.jobradar.core.tracker.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Partial update: null fields are left unchanged. Stage changes go through /move. */
public record UpdateApplicationRequest(
        @Size(min = 1, max = 160) String company,
        @Size(min = 1, max = 200) String title,
        @Size(max = 160) String location,
        @Size(max = 1000) String jobUrl,
        @Size(max = 5000) String notes,
        Instant followUpAt) {
}

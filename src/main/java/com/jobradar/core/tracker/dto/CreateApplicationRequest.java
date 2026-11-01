package com.jobradar.core.tracker.dto;

import com.jobradar.core.tracker.Stage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** stage defaults to SAVED when null. */
public record CreateApplicationRequest(
        @NotBlank @Size(max = 160) String company,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 160) String location,
        @Size(max = 1000) String jobUrl,
        Stage stage,
        @Size(max = 5000) String notes,
        Instant followUpAt) {
}

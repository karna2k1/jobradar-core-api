package com.jobradar.core.tracker.dto;

import com.jobradar.core.tracker.Application;
import com.jobradar.core.tracker.Stage;
import java.time.Instant;

public record ApplicationDto(Long id, String company, String title, String location, String jobUrl, String source,
                             Integer matchScore, Stage stage, double position, String notes, Instant appliedAt,
                             Instant followUpAt, Instant createdAt, Instant updatedAt) {

    public static ApplicationDto from(Application a) {
        return new ApplicationDto(a.getId(), a.getCompany(), a.getTitle(), a.getLocation(), a.getJobUrl(),
                a.getSource(), a.getMatchScore(), a.getStage(), a.getPosition(), a.getNotes(), a.getAppliedAt(),
                a.getFollowUpAt(), a.getCreatedAt(), a.getUpdatedAt());
    }
}

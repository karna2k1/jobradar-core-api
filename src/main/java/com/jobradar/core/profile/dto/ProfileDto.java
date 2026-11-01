package com.jobradar.core.profile.dto;

import com.jobradar.core.profile.Profile;
import com.jobradar.core.profile.WorkMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Used for both GET and PUT /profile. */
public record ProfileDto(
        @Size(max = 160) String headline,
        @DecimalMin("0") @DecimalMax("50") BigDecimal experienceYears,
        @DecimalMin("0") @DecimalMax("9999") BigDecimal currentCtcLpa,
        @DecimalMin("0") @DecimalMax("9999") BigDecimal expectedCtcLpa,
        @Min(0) @Max(365) Integer noticePeriodDays,
        @NotNull WorkMode workMode,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 60) String> skills,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> targetRoles,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> locations,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 160) String> blacklistCompanies,
        @Min(0) @Max(100) int minMatchScore,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd) {

    public static ProfileDto from(Profile p) {
        return new ProfileDto(p.getHeadline(), p.getExperienceYears(), p.getCurrentCtcLpa(), p.getExpectedCtcLpa(),
                p.getNoticePeriodDays(), p.getWorkMode(), p.getSkills(), p.getTargetRoles(), p.getLocations(),
                p.getBlacklistCompanies(), p.getMinMatchScore(), p.getQuietHoursStart(), p.getQuietHoursEnd());
    }

    public void applyTo(Profile p) {
        p.setHeadline(headline);
        p.setExperienceYears(experienceYears);
        p.setCurrentCtcLpa(currentCtcLpa);
        p.setExpectedCtcLpa(expectedCtcLpa);
        p.setNoticePeriodDays(noticePeriodDays);
        p.setWorkMode(workMode);
        p.setSkills(new ArrayList<>(skills));
        p.setTargetRoles(new ArrayList<>(targetRoles));
        p.setLocations(new ArrayList<>(locations));
        p.setBlacklistCompanies(new ArrayList<>(blacklistCompanies));
        p.setMinMatchScore(minMatchScore);
        p.setQuietHoursStart(quietHoursStart);
        p.setQuietHoursEnd(quietHoursEnd);
    }
}

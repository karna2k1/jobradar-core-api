package com.jobradar.core.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 160)
    private String headline;

    @Column(name = "experience_years", precision = 4, scale = 1)
    private BigDecimal experienceYears;

    @Column(name = "current_ctc_lpa", precision = 6, scale = 2)
    private BigDecimal currentCtcLpa;

    @Column(name = "expected_ctc_lpa", precision = 6, scale = 2)
    private BigDecimal expectedCtcLpa;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 10)
    private WorkMode workMode = WorkMode.ANY;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> skills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_roles", nullable = false, columnDefinition = "jsonb")
    private List<String> targetRoles = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> locations = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blacklist_companies", nullable = false, columnDefinition = "jsonb")
    private List<String> blacklistCompanies = new ArrayList<>();

    @Column(name = "min_match_score", nullable = false)
    private int minMatchScore = 70;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Profile(Long userId) {
        this.userId = userId;
    }
}

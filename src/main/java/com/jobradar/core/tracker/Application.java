package com.jobradar.core.tracker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A job card on the user's kanban board. */
@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 160)
    private String company;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 160)
    private String location;

    @Column(name = "job_url", length = 1000)
    private String jobUrl;

    @Column(nullable = false, length = 40)
    private String source = "MANUAL";

    @Column(name = "external_job_id", length = 100)
    private String externalJobId;

    @Column(name = "match_score")
    private Integer matchScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Stage stage;

    /** Sort key within a column; the client sends the midpoint between neighbours when dragging. */
    @Column(nullable = false)
    private double position;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "follow_up_at")
    private Instant followUpAt;

    @Column(name = "follow_up_sent", nullable = false)
    private boolean followUpSent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

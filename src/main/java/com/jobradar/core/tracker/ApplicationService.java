package com.jobradar.core.tracker;

import com.jobradar.core.common.ApiException;
import com.jobradar.core.tracker.dto.ApplicationDto;
import com.jobradar.core.tracker.dto.CreateApplicationRequest;
import com.jobradar.core.tracker.dto.MoveRequest;
import com.jobradar.core.tracker.dto.UpdateApplicationRequest;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    static final double POSITION_GAP = 1024;

    private final ApplicationRepository apps;
    private final ApplicationEventRepository events;

    @Transactional(readOnly = true)
    public List<ApplicationDto> list(Long userId) {
        return apps.findByUserIdOrderByPositionAsc(userId).stream().map(ApplicationDto::from).toList();
    }

    @Transactional
    public ApplicationDto create(Long userId, CreateApplicationRequest req) {
        Stage stage = req.stage() == null ? Stage.SAVED : req.stage();
        Application a = new Application();
        a.setUserId(userId);
        a.setCompany(req.company().strip());
        a.setTitle(req.title().strip());
        a.setLocation(req.location());
        a.setJobUrl(req.jobUrl());
        a.setNotes(req.notes());
        a.setFollowUpAt(req.followUpAt());
        a.setStage(stage);
        a.setPosition(endOf(userId, stage));
        if (stage == Stage.APPLIED) a.setAppliedAt(Instant.now());
        apps.save(a);
        events.save(new ApplicationEvent(a, null, stage));
        return ApplicationDto.from(a);
    }

    @Transactional
    public ApplicationDto update(Long userId, Long id, UpdateApplicationRequest req) {
        Application a = find(userId, id);
        if (req.company() != null) a.setCompany(req.company().strip());
        if (req.title() != null) a.setTitle(req.title().strip());
        if (req.location() != null) a.setLocation(req.location());
        if (req.jobUrl() != null) a.setJobUrl(req.jobUrl());
        if (req.notes() != null) a.setNotes(req.notes());
        if (req.followUpAt() != null && !req.followUpAt().equals(a.getFollowUpAt())) {
            a.setFollowUpAt(req.followUpAt());
            a.setFollowUpSent(false);
        }
        return ApplicationDto.from(apps.saveAndFlush(a));
    }

    @Transactional
    public ApplicationDto move(Long userId, Long id, MoveRequest req) {
        Application a = find(userId, id);
        Stage from = a.getStage();
        if (from != req.stage()) {
            a.setStage(req.stage());
            if (req.stage() == Stage.APPLIED && a.getAppliedAt() == null) a.setAppliedAt(Instant.now());
            events.save(new ApplicationEvent(a, from, req.stage()));
        }
        a.setPosition(req.position() != null ? req.position() : endOf(userId, req.stage()));
        return ApplicationDto.from(apps.saveAndFlush(a));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        apps.delete(find(userId, id));
    }

    /** Upserts the card for a match: new suggestion, saved (moves to SAVED) or ignored (removes a suggestion). */
    @Transactional
    public void applyMatch(JobMatchedListener.JobMatchedEvent e) {
        if (e.userId() == null || e.jobId() == null) return;
        var existing = apps.findByUserIdAndExternalJobId(e.userId(), e.jobId());
        String status = e.status() == null ? "MATCHED" : e.status();

        if (existing.isEmpty()) {
            if ("IGNORED".equals(status)) return;
            Stage stage = "SAVED".equals(status) ? Stage.SAVED : Stage.SUGGESTED;
            Application a = new Application();
            a.setUserId(e.userId());
            a.setExternalJobId(e.jobId());
            a.setCompany(truncate(e.company(), 160, "Unknown"));
            a.setTitle(truncate(e.title(), 200, "Untitled"));
            a.setLocation(truncate(e.location(), 160, null));
            a.setJobUrl(truncate(e.url(), 1000, null));
            a.setSource(truncate(e.source(), 40, "MATCH"));
            a.setMatchScore(e.score());
            a.setStage(stage);
            a.setPosition(endOf(e.userId(), stage));
            apps.save(a);
            events.save(new ApplicationEvent(a, null, stage));
            return;
        }

        Application a = existing.get();
        if (a.getStage() != Stage.SUGGESTED) return; // user already took it further; never override
        if ("IGNORED".equals(status)) {
            apps.delete(a);
        } else if ("SAVED".equals(status)) {
            move(e.userId(), a.getId(), new MoveRequest(Stage.SAVED, null));
        } else if (e.score() != null) {
            a.setMatchScore(e.score());
        }
    }

    private static String truncate(String v, int max, String fallback) {
        if (v == null || v.isBlank()) return fallback;
        return v.length() <= max ? v : v.substring(0, max);
    }

    private Application find(Long userId, Long id) {
        return apps.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private double endOf(Long userId, Stage stage) {
        return apps.maxPosition(userId, stage).orElse(0d) + POSITION_GAP;
    }
}

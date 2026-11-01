package com.jobradar.core.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes job.matched from match-notify-service and keeps "Suggested" cards in sync. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobMatchedListener {
    private final ObjectMapper mapper;
    private final ApplicationService service;

    /** status: MATCHED (new suggestion), SAVED or IGNORED (user feedback from email/feed). */
    public record JobMatchedEvent(Long userId, String jobId, String company, String title, String location,
                                  String url, String source, Integer score, String status) {}

    @KafkaListener(topics = "job.matched", autoStartup = "${app.kafka.enabled}")
    public void onMessage(String payload) {
        try {
            service.applyMatch(mapper.readValue(payload, JobMatchedEvent.class));
        } catch (Exception e) {
            log.warn("Skipping bad job.matched message: {}", e.getMessage());
        }
    }
}

package com.jobradar.core.tracker;

import com.jobradar.core.auth.User;
import com.jobradar.core.auth.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Emails a follow-up reminder for every card whose follow_up_at has passed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpReminderJob {
    private final ApplicationRepository apps;
    private final UserRepository users;
    private final JavaMailSender mailSender;

    @Value("${app.mail-from}")
    private String from;

    @Scheduled(cron = "${app.reminders.cron}", zone = "${app.reminders.zone}")
    @Transactional
    public void sendDueReminders() {
        List<Application> due = apps.findTop100ByFollowUpSentFalseAndFollowUpAtLessThanEqualOrderByFollowUpAtAsc(Instant.now());
        if (due.isEmpty()) return;

        Map<Long, User> owners = users.findAllById(due.stream().map(Application::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        int sent = 0;
        for (Application a : due) {
            User u = owners.get(a.getUserId());
            if (u == null) continue;
            try {
                mailSender.send(message(u, a));
                a.setFollowUpSent(true);
                sent++;
            } catch (MailException e) {
                log.warn("Follow-up reminder failed for application {}: {}", a.getId(), e.getMessage());
            }
        }
        log.info("Follow-up reminders sent: {}/{}", sent, due.size());
    }

    private SimpleMailMessage message(User u, Application a) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(u.getEmail());
        m.setSubject("Follow up: " + a.getTitle() + " at " + a.getCompany());
        m.setText("Hi " + u.getFullName() + ",\n\n"
                + "Time to follow up on your " + a.getStage().name().toLowerCase() + " application for "
                + a.getTitle() + " at " + a.getCompany() + "."
                + (a.getJobUrl() != null ? "\n\nJob link: " + a.getJobUrl() : "")
                + (a.getNotes() != null ? "\n\nYour notes:\n" + a.getNotes() : "")
                + "\n\n- JobRadar");
        return m;
    }
}

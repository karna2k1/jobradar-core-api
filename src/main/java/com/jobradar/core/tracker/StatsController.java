package com.jobradar.core.tracker;

import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Application funnel for insights-mfe. */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {
    private final JdbcTemplate jdbc;

    /** reached = cards that were ever in a stage; current = cards in it now. */
    public record Funnel(Map<Stage, Long> reached, Map<Stage, Long> current) {}

    @GetMapping("/funnel")
    public Funnel funnel(@AuthenticationPrincipal Long userId) {
        Map<Stage, Long> reached = zeroed();
        jdbc.query("select to_stage, count(distinct application_id) from application_events where user_id = ? group by to_stage",
                rs -> { reached.put(Stage.valueOf(rs.getString(1)), rs.getLong(2)); }, userId);
        Map<Stage, Long> current = zeroed();
        jdbc.query("select stage, count(*) from applications where user_id = ? group by stage",
                rs -> { current.put(Stage.valueOf(rs.getString(1)), rs.getLong(2)); }, userId);
        return new Funnel(reached, current);
    }

    private static Map<Stage, Long> zeroed() {
        Map<Stage, Long> m = new EnumMap<>(Stage.class);
        for (Stage s : Stage.values()) m.put(s, 0L);
        return m;
    }
}

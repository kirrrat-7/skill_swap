package com.skillswap.service;

import com.skillswap.dto.DashboardData;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DashboardData forStudent(long userId) {
        long skills = count("SELECT COUNT(*) FROM user_skills WHERE user_id = ?", userId);
        long teaching = count("SELECT COUNT(*) FROM user_skills WHERE user_id = ? AND type = 'TEACH'", userId);
        long learning = count("SELECT COUNT(*) FROM user_skills WHERE user_id = ? AND type = 'LEARN'", userId);
        long upcoming = count("SELECT COUNT(*) FROM sessions WHERE (teacher_id = ? OR learner_id = ?) AND status = 'SCHEDULED' AND starts_at > UTC_TIMESTAMP(6)", userId, userId);
        long completed = count("SELECT COUNT(*) FROM sessions WHERE (teacher_id = ? OR learner_id = ?) AND status = 'COMPLETED'", userId, userId);
        long credits = jdbc.queryForObject("SELECT COALESCE((SELECT balance FROM wallets WHERE user_id = ?), 0)", Long.class, userId);
        Double hours = jdbc.queryForObject("SELECT COALESCE(SUM(minutes_learned), 0) / 60.0 FROM learning_progress WHERE user_id = ?", Double.class, userId);
        long achievements = count("SELECT COUNT(*) FROM user_achievements WHERE user_id = ?", userId);
        long unread = count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL", userId);

        var next = jdbc.query("""
            SELECT s.id, s.topic, sk.name AS skill_name, s.mode, s.starts_at,
                   CASE WHEN s.teacher_id = ? THEN learner.full_name ELSE teacher.full_name END AS other_student,
                   CASE WHEN s.teacher_id = ? THEN learner.timezone ELSE teacher.timezone END AS other_timezone
            FROM sessions s
            JOIN skills sk ON sk.id = s.skill_id
            JOIN users teacher ON teacher.id = s.teacher_id
            JOIN users learner ON learner.id = s.learner_id
            WHERE (s.teacher_id = ? OR s.learner_id = ?)
              AND s.status = 'SCHEDULED' AND s.starts_at > UTC_TIMESTAMP(6)
            ORDER BY s.starts_at LIMIT 1
            """, (rs, row) -> new DashboardData.UpcomingSession(rs.getLong("id"),
                rs.getString("topic"), rs.getString("skill_name"), rs.getString("mode"),
                rs.getTimestamp("starts_at").toInstant(), rs.getString("other_student"),
                rs.getString("other_timezone")), userId, userId, userId, userId).stream().findFirst().orElse(null);

        List<DashboardData.RecentNotification> recent = jdbc.query("""
            SELECT id, title, message, link, created_at, read_at
            FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 5
            """, (rs, row) -> new DashboardData.RecentNotification(rs.getLong("id"),
                rs.getString("title"), rs.getString("message"), rs.getString("link"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("read_at") == null), userId);

        return new DashboardData(skills, teaching, learning, upcoming, completed, credits,
            hours == null ? 0.0 : Math.round(hours * 10.0) / 10.0, achievements, unread, next, recent);
    }

    private long count(String sql, Object... params) {
        Long result = jdbc.queryForObject(sql, Long.class, params);
        return result == null ? 0 : result;
    }
}

package com.skillswap.service;

import com.skillswap.dto.CategorySummary;
import com.skillswap.dto.PublicMetrics;
import com.skillswap.dto.PublicTestimonial;
import com.skillswap.dto.SkillSummary;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PublicCatalogService {
    private final JdbcTemplate jdbc;

    public PublicCatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CategorySummary> categories() {
        return jdbc.query("""
                SELECT c.id, c.name, c.slug, c.icon, COUNT(s.id) AS skill_count
                FROM skill_categories c
                LEFT JOIN skills s ON s.category_id = c.id
                WHERE c.active = 1
                GROUP BY c.id, c.name, c.slug, c.icon, c.display_order
                ORDER BY c.display_order, c.name
                """, (rs, row) -> new CategorySummary(rs.getLong("id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("icon"), rs.getLong("skill_count")));
    }

    public List<SkillSummary> popularSkills(int limit) {
        return jdbc.query("""
                SELECT s.id, s.name, c.name AS category_name, c.slug AS category_slug,
                       COALESCE(SUM(us.type = 'TEACH'), 0) AS teacher_count,
                       COALESCE(SUM(us.type = 'LEARN'), 0) AS learner_count
                FROM skills s
                JOIN skill_categories c ON c.id = s.category_id AND c.active = 1
                LEFT JOIN user_skills us ON us.skill_id = s.id
                GROUP BY s.id, s.name, c.name, c.slug
                ORDER BY (COUNT(us.id) > 0) DESC, COUNT(us.id) DESC, s.name
                LIMIT ?
                """, (rs, row) -> new SkillSummary(rs.getLong("id"), rs.getString("name"),
                rs.getString("category_name"), rs.getString("category_slug"),
                rs.getLong("teacher_count"), rs.getLong("learner_count")), limit);
    }

    public List<SkillSummary> searchSkills(String query, String categorySlug) {
        String search = StringUtils.hasText(query) ? "%" + query.strip().replace("%", "\\%").replace("_", "\\_") + "%" : "%";
        String category = StringUtils.hasText(categorySlug) ? categorySlug.strip() : "%";
        return jdbc.query("""
                SELECT s.id, s.name, c.name AS category_name, c.slug AS category_slug,
                       COALESCE(SUM(us.type = 'TEACH'), 0) AS teacher_count,
                       COALESCE(SUM(us.type = 'LEARN'), 0) AS learner_count
                FROM skills s
                JOIN skill_categories c ON c.id = s.category_id AND c.active = 1
                LEFT JOIN user_skills us ON us.skill_id = s.id
                WHERE s.name LIKE ? ESCAPE '\\\\' AND c.slug LIKE ?
                GROUP BY s.id, s.name, c.name, c.slug
                ORDER BY COUNT(us.id) DESC, s.name
                LIMIT 100
                """, (rs, row) -> new SkillSummary(rs.getLong("id"), rs.getString("name"),
                rs.getString("category_name"), rs.getString("category_slug"),
                rs.getLong("teacher_count"), rs.getLong("learner_count")), search, category);
    }

    public List<PublicTestimonial> testimonials(int limit) {
        return jdbc.query("""
                SELECT r.id, reviewer.full_name AS reviewer_name, reviewee.full_name AS reviewee_name,
                       sk.name AS skill_name, r.rating, r.comment
                FROM reviews r
                JOIN users reviewer ON reviewer.id = r.reviewer_id AND reviewer.status = 'ACTIVE'
                JOIN users reviewee ON reviewee.id = r.reviewee_id AND reviewee.status = 'ACTIVE' AND reviewee.show_reviews = 1
                JOIN sessions s ON s.id = r.session_id AND s.status = 'COMPLETED' AND s.disputed = 0
                JOIN skills sk ON sk.id = s.skill_id
                WHERE r.hidden = 0 AND r.comment IS NOT NULL AND CHAR_LENGTH(TRIM(r.comment)) > 0
                ORDER BY r.created_at DESC
                LIMIT ?
                """, (rs, row) -> new PublicTestimonial(rs.getLong("id"),
                rs.getString("reviewer_name"), rs.getString("reviewee_name"),
                rs.getString("skill_name"), rs.getInt("rating"), rs.getString("comment")), limit);
    }

    public PublicMetrics metrics() {
        long students = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE' AND email_verified_at IS NOT NULL", Long.class);
        long skills = jdbc.queryForObject("SELECT COUNT(*) FROM user_skills us JOIN users u ON u.id = us.user_id WHERE u.status = 'ACTIVE' AND u.profile_public = 1", Long.class);
        long exchanges = jdbc.queryForObject("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED' AND disputed = 0", Long.class);
        return new PublicMetrics(students, skills, exchanges);
    }
}

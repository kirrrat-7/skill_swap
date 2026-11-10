package com.skillswap.dto;

public record SkillSummary(long id, String name, String categoryName, String categorySlug, long teacherCount, long learnerCount) {
}

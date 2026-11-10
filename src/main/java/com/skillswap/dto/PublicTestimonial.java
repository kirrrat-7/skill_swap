package com.skillswap.dto;

public record PublicTestimonial(long id, String reviewerName, String revieweeName,
                                String skillName, int rating, String comment) {
}

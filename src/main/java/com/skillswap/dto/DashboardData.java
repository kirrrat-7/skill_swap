package com.skillswap.dto;

import java.time.Instant;
import java.util.List;

public record DashboardData(
    long totalSkills,
    long teachingSkills,
    long learningSkills,
    long upcomingSessions,
    long completedSessions,
    long skillCredits,
    double learningHours,
    long achievements,
    long unreadNotifications,
    UpcomingSession nextSession,
    List<RecentNotification> recentNotifications
) {
    public record UpcomingSession(long id, String topic, String skillName, String mode,
                                  Instant startsAt, String otherStudent, String otherTimezone) { }
    public record RecentNotification(long id, String title, String message, String link,
                                     Instant createdAt, boolean unread) { }
}

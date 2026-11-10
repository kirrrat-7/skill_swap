package com.skillswap.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimitBucketCleanupJob {
    private final JdbcTemplate jdbc;

    public RateLimitBucketCleanupJob(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Scheduled(cron = "0 17 3 * * *", zone = "UTC")
    public void removeExpiredBuckets() {
        jdbc.update("DELETE FROM rate_limit_buckets WHERE updated_at < UTC_TIMESTAMP(6) - INTERVAL 2 DAY");
    }
}

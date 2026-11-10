package com.skillswap.repository;

import com.skillswap.entity.EmailVerificationTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, Long> {
    @Query("select t from EmailVerificationTokenEntity t join fetch t.user where t.tokenHash = :hash")
    Optional<EmailVerificationTokenEntity> findByHashWithUser(@Param("hash") String hash);

    @Modifying
    @Query("update EmailVerificationTokenEntity t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateActiveForUser(@Param("userId") long userId, @Param("now") Instant now);
}

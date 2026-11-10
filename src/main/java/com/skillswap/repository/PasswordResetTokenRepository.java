package com.skillswap.repository;

import com.skillswap.entity.PasswordResetTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetTokenEntity t join fetch t.user where t.tokenHash = :hash")
    Optional<PasswordResetTokenEntity> lockByHashWithUser(@Param("hash") String hash);

    @Query("select t from PasswordResetTokenEntity t where t.tokenHash = :hash")
    Optional<PasswordResetTokenEntity> findByHash(@Param("hash") String hash);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int invalidateActiveForUser(@Param("userId") long userId, @Param("now") Instant now);
}

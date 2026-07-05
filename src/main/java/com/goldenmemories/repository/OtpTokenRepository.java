package com.goldenmemories.repository;

import com.goldenmemories.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /** Find the most recent unused, unexpired token for an email. */
    @Query("""
        SELECT t FROM OtpToken t
        WHERE t.email = :email
          AND t.used = false
          AND t.expiresAt > CURRENT_TIMESTAMP
        ORDER BY t.expiresAt DESC
        LIMIT 1
        """)
    Optional<OtpToken> findActiveToken(String email);

    /** Invalidate all previous tokens for an email before issuing a new one. */
    @Modifying
    @Transactional
    @Query("UPDATE OtpToken t SET t.used = true WHERE t.email = :email")
    void invalidateAll(String email);
}

package com.SIGMA.USCO.Users.repository;


import com.SIGMA.USCO.Users.entity.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BlackListedTokenRepository extends JpaRepository<BlackListedToken, Long> {
    boolean existsByToken(String token);

    int deleteByExpiresAtBefore(LocalDateTime now);
}

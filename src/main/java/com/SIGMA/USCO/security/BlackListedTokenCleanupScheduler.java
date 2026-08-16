package com.SIGMA.USCO.security;

import com.SIGMA.USCO.Users.repository.BlackListedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlackListedTokenCleanupScheduler {

    private final BlackListedTokenRepository blackListedTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteExpiredTokens() {
        int deleted = blackListedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Tokens blacklisteados expirados eliminados: {}", deleted);
        }
    }
}
package com.jesuspartal.specforge.application.service;

import com.jesuspartal.specforge.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${specforge.rate-limit.capacity:100}")
    private int capacity;

    @Value("${specforge.rate-limit.tokens-per-refill:100}")
    private int refillTokens;

    @Value("${specforge.rate-limit.refill-period-minutes:60}")
    private int refillPeriodMinutes;

    public void checkAndConsume() {
        Bucket bucket = buckets.computeIfAbsent("github-api", k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillIntervally(refillTokens, Duration.ofMinutes(refillPeriodMinutes))
                                .build())
                        .build()
        );
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }
    }
}
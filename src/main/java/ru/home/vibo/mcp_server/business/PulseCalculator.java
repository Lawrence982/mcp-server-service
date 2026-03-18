package ru.home.vibo.mcp_server.business;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class PulseCalculator {

    private static final int MIN_PULSE = 1;
    private static final int MAX_PULSE = 100;

    public int getPulse() {
        return ThreadLocalRandom.current().nextInt(MAX_PULSE) + MIN_PULSE;
    }
}

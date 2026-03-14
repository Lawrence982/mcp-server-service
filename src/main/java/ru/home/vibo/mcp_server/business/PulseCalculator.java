package ru.home.vibo.mcp_server.business;

import java.util.Random;

public class PulseCalculator {

    public static final Random RANDOM = new Random();

    public static int getPulse(){
        return RANDOM.nextInt(100)+1;
    }
}

package ru.home.vibo.mcp_server.business;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PulseCalculatorTest {

    private final PulseCalculator calculator = new PulseCalculator();

    @Test
    void getPulse_returnsValueInRange() {
        for (int i = 0; i < 1000; i++) {
            int pulse = calculator.getPulse();
            assertThat(pulse).isBetween(1, 100);
        }
    }

    @RepeatedTest(50)
    void getPulse_neverReturnsZeroOrNegative() {
        assertThat(calculator.getPulse()).isGreaterThan(0);
    }

    @RepeatedTest(50)
    void getPulse_neverExceeds100() {
        assertThat(calculator.getPulse()).isLessThanOrEqualTo(100);
    }

    @Test
    void getPulse_returnsDistinctValues() {
        Set<Integer> values = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            values.add(calculator.getPulse());
        }
        assertThat(values.size()).isGreaterThan(1);
    }
}

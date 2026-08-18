package com.minitricount.expense;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EqualSplitCalculatorTest {

    @Test
    void tenEuros_threeParticipants_remainderGoesToSmallestIds() {
        Map<Long, BigDecimal> shares = EqualSplitCalculator.split(new BigDecimal("10.00"), List.of(3L, 1L, 2L));

        assertThat(shares.get(1L)).isEqualByComparingTo("3.34");
        assertThat(shares.get(2L)).isEqualByComparingTo("3.33");
        assertThat(shares.get(3L)).isEqualByComparingTo("3.33");
        assertThat(sum(shares)).isEqualByComparingTo("10.00");
    }

    @Test
    void tenEuros_twoParticipants_noRemainder() {
        Map<Long, BigDecimal> shares = EqualSplitCalculator.split(new BigDecimal("10.00"), List.of(1L, 2L));

        assertThat(shares.get(1L)).isEqualByComparingTo("5.00");
        assertThat(shares.get(2L)).isEqualByComparingTo("5.00");
        assertThat(sum(shares)).isEqualByComparingTo("10.00");
    }

    @Test
    void oneCent_fiveParticipants_onlySmallestIdGetsIt() {
        Map<Long, BigDecimal> shares = EqualSplitCalculator.split(new BigDecimal("0.01"), List.of(5L, 4L, 3L, 2L, 1L));

        assertThat(shares.get(1L)).isEqualByComparingTo("0.01");
        assertThat(shares.get(2L)).isEqualByComparingTo("0.00");
        assertThat(shares.get(3L)).isEqualByComparingTo("0.00");
        assertThat(shares.get(4L)).isEqualByComparingTo("0.00");
        assertThat(shares.get(5L)).isEqualByComparingTo("0.00");
        assertThat(sum(shares)).isEqualByComparingTo("0.01");
    }

    @Test
    void hundredEuros_singleParticipant_getsEverything() {
        Map<Long, BigDecimal> shares = EqualSplitCalculator.split(new BigDecimal("100.00"), List.of(1L));

        assertThat(shares.get(1L)).isEqualByComparingTo("100.00");
        assertThat(sum(shares)).isEqualByComparingTo("100.00");
    }

    @Test
    void integerScaleAmount_isNormalizedToScaleTwo() {
        Map<Long, BigDecimal> shares = EqualSplitCalculator.split(new BigDecimal("7"), List.of(1L, 2L, 3L));

        assertThat(sum(shares)).isEqualByComparingTo("7.00");
    }

    @Test
    void duplicateParticipantIds_throwIllegalArgumentException() {
        assertThatThrownBy(() -> EqualSplitCalculator.split(new BigDecimal("10.00"), List.of(1L, 2L, 1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EqualSplitCalculator.split(new BigDecimal("10.00"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullList_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EqualSplitCalculator.split(new BigDecimal("10.00"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal sum(Map<Long, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

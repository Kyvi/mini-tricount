package com.minitricount.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EqualSplitCalculator {

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private EqualSplitCalculator() {
    }

    public static Map<Long, BigDecimal> split(BigDecimal amount, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("La liste des bénéficiaires ne peut pas être vide");
        }
        Set<Long> distinctIds = new LinkedHashSet<>(participantIds);
        if (distinctIds.size() != participantIds.size()) {
            throw new IllegalArgumentException("La liste des bénéficiaires contient des doublons");
        }

        List<Long> sorted = distinctIds.stream().sorted().toList();
        int n = sorted.size();
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        int remainderCents = amount.subtract(base.multiply(BigDecimal.valueOf(n)))
                .movePointRight(2)
                .intValueExact();

        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            shares.put(sorted.get(i), i < remainderCents ? base.add(ONE_CENT) : base);
        }
        return shares;
    }
}

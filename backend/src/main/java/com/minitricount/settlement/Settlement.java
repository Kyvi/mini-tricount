package com.minitricount.settlement;

import java.math.BigDecimal;

public record Settlement(
        Long fromParticipantId,
        String fromParticipantName,
        Long toParticipantId,
        String toParticipantName,
        BigDecimal amount
) {
}

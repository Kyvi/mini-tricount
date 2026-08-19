package com.minitricount.settlement.dto;

import com.minitricount.settlement.Settlement;

import java.math.BigDecimal;

public record SettlementResponse(
        Long fromParticipantId,
        String fromParticipantName,
        Long toParticipantId,
        String toParticipantName,
        BigDecimal amount
) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.fromParticipantId(),
                settlement.fromParticipantName(),
                settlement.toParticipantId(),
                settlement.toParticipantName(),
                settlement.amount()
        );
    }
}

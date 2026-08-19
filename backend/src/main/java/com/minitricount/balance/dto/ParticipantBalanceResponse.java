package com.minitricount.balance.dto;

import com.minitricount.balance.ParticipantBalance;

import java.math.BigDecimal;

public record ParticipantBalanceResponse(Long participantId, String participantName, BigDecimal balance) {

    public static ParticipantBalanceResponse from(ParticipantBalance participantBalance) {
        return new ParticipantBalanceResponse(
                participantBalance.participantId(),
                participantBalance.participantName(),
                participantBalance.balance()
        );
    }
}

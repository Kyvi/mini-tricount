package com.minitricount.expense.dto;

import com.minitricount.expense.ExpenseShare;

import java.math.BigDecimal;

public record ExpenseShareResponse(Long participantId, String participantName, BigDecimal shareAmount) {

    public static ExpenseShareResponse from(ExpenseShare share) {
        return new ExpenseShareResponse(
                share.getParticipant().getId(),
                share.getParticipant().getName(),
                share.getShareAmount()
        );
    }
}

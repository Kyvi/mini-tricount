package com.minitricount.expense.dto;

import com.minitricount.expense.Expense;
import com.minitricount.participant.dto.ParticipantResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        Instant createdAt,
        ParticipantResponse paidBy,
        List<ExpenseShareResponse> shares
) {

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getCreatedAt(),
                ParticipantResponse.from(expense.getPaidBy()),
                expense.getShares().stream().map(ExpenseShareResponse::from).toList()
        );
    }
}

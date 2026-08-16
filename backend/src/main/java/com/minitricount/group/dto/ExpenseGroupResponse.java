package com.minitricount.group.dto;

import com.minitricount.group.ExpenseGroup;

import java.time.Instant;

public record ExpenseGroupResponse(Long id, String name, Instant createdAt) {

    public static ExpenseGroupResponse from(ExpenseGroup group) {
        return new ExpenseGroupResponse(group.getId(), group.getName(), group.getCreatedAt());
    }
}

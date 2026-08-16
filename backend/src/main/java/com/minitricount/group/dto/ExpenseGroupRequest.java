package com.minitricount.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpenseGroupRequest(@NotBlank @Size(max = 255) String name) {
}

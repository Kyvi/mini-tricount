package com.minitricount.participant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipantRequest(@NotBlank @Size(max = 255) String name) {
}

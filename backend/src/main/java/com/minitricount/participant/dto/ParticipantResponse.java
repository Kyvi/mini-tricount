package com.minitricount.participant.dto;

import com.minitricount.participant.Participant;

public record ParticipantResponse(Long id, String name) {

    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getName());
    }
}

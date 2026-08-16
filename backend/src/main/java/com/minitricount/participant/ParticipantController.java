package com.minitricount.participant;

import com.minitricount.participant.dto.ParticipantRequest;
import com.minitricount.participant.dto.ParticipantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    public ResponseEntity<ParticipantResponse> add(@PathVariable Long groupId,
                                                     @Valid @RequestBody ParticipantRequest request) {
        Participant created = participantService.addToGroup(groupId, request.name());
        ParticipantResponse body = ParticipantResponse.from(created);
        return ResponseEntity.created(
                URI.create("/api/groups/" + groupId + "/participants/" + created.getId())
        ).body(body);
    }

    @GetMapping
    public List<ParticipantResponse> findAll(@PathVariable Long groupId) {
        return participantService.findByGroup(groupId).stream()
                .map(ParticipantResponse::from)
                .toList();
    }
}

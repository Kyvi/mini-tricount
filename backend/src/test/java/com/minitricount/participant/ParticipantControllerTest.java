package com.minitricount.participant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.participant.dto.ParticipantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipantController.class)
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParticipantService participantService;

    private static Participant participantWithId(Long id, ExpenseGroup group, String name) {
        Participant participant = new Participant(group, name);
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    @Test
    void add_returns201WithLocationAndBody_whenNameValid() throws Exception {
        ExpenseGroup group = new ExpenseGroup("Colocation");
        when(participantService.addToGroup(1L, "Alice")).thenReturn(participantWithId(10L, group, "Alice"));

        mockMvc.perform(post("/api/groups/1/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParticipantRequest("Alice"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/groups/1/participants/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void add_returns400WithFieldErrors_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/groups/1/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParticipantRequest(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void add_returns400_whenNameExceeds255Characters() throws Exception {
        mockMvc.perform(post("/api/groups/1/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParticipantRequest("a".repeat(256)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void add_returns404_whenGroupMissing() throws Exception {
        when(participantService.addToGroup(99L, "Alice"))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(post("/api/groups/99/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ParticipantRequest("Alice"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findAll_returns200WithParticipantList() throws Exception {
        ExpenseGroup group = new ExpenseGroup("Colocation");
        when(participantService.findByGroup(1L))
                .thenReturn(List.of(participantWithId(10L, group, "Alice"), participantWithId(11L, group, "Bob")));

        mockMvc.perform(get("/api/groups/1/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void findAll_returns200WithEmptyList_whenGroupHasNoParticipants() throws Exception {
        when(participantService.findByGroup(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findAll_returns404_whenGroupMissing() throws Exception {
        when(participantService.findByGroup(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(get("/api/groups/99/participants"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findAll_returns400_whenGroupIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/groups/abc/participants"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void findAll_returns500WithGenericMessage_whenUnexpectedExceptionOccurs() throws Exception {
        when(participantService.findByGroup(1L)).thenThrow(new RuntimeException("boom, détail interne sensible"));

        mockMvc.perform(get("/api/groups/1/participants"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
    }
}

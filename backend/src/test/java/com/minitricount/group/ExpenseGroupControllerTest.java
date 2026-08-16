package com.minitricount.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.group.dto.ExpenseGroupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseGroupController.class)
class ExpenseGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseGroupService expenseGroupService;

    private static ExpenseGroup groupWithId(Long id, String name) {
        ExpenseGroup group = new ExpenseGroup(name);
        ReflectionTestUtils.setField(group, "id", id);
        ReflectionTestUtils.setField(group, "createdAt", Instant.parse("2026-08-16T10:00:00Z"));
        return group;
    }

    @Test
    void create_returns201WithLocationAndBody_whenNameValid() throws Exception {
        when(expenseGroupService.create("Weekend à Lyon")).thenReturn(groupWithId(1L, "Weekend à Lyon"));

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseGroupRequest("Weekend à Lyon"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/groups/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Weekend à Lyon"));
    }

    @Test
    void create_returns400WithFieldErrors_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseGroupRequest(" "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_returns400_whenNameExceeds255Characters() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExpenseGroupRequest("a".repeat(256)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void findAll_returns200WithGroupList() throws Exception {
        when(expenseGroupService.findAll())
                .thenReturn(List.of(groupWithId(1L, "A"), groupWithId(2L, "B")));

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"));
    }

    @Test
    void findById_returns200_whenGroupExists() throws Exception {
        when(expenseGroupService.findById(1L)).thenReturn(groupWithId(1L, "Colocation"));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Colocation"));
    }

    @Test
    void findById_returns404_whenGroupMissing() throws Exception {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(get("/api/groups/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Groupe introuvable : id=99"));
    }

    @Test
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/groups/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void findById_returns500WithGenericMessage_whenUnexpectedExceptionOccurs() throws Exception {
        when(expenseGroupService.findById(1L)).thenThrow(new RuntimeException("boom, détail interne sensible"));

        mockMvc.perform(get("/api/groups/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
    }
}

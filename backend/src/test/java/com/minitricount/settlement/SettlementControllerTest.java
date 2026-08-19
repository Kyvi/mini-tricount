package com.minitricount.settlement;

import com.minitricount.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettlementService settlementService;

    @Test
    void getSettlements_returns200WithExpectedStructure_forPromptExample() throws Exception {
        when(settlementService.getSettlements(1L)).thenReturn(List.of(
                new Settlement(2L, "Camille", 1L, "Vincent", new BigDecimal("4.00")),
                new Settlement(3L, "Alice", 1L, "Vincent", new BigDecimal("16.00"))
        ));

        mockMvc.perform(get("/api/groups/1/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fromParticipantId").value(2))
                .andExpect(jsonPath("$[0].fromParticipantName").value("Camille"))
                .andExpect(jsonPath("$[0].toParticipantId").value(1))
                .andExpect(jsonPath("$[0].toParticipantName").value("Vincent"))
                .andExpect(jsonPath("$[0].amount").value(4.00))
                .andExpect(jsonPath("$[1].amount").value(16.00));
    }

    @Test
    void getSettlements_returns200WithEmptyList_whenNoSettlementsNeeded() throws Exception {
        when(settlementService.getSettlements(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSettlements_returns404_whenGroupMissing() throws Exception {
        when(settlementService.getSettlements(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(get("/api/groups/99/settlements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getSettlements_returns400_whenGroupIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/groups/abc/settlements"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getSettlements_returns500WithGenericMessage_whenUnexpectedExceptionOccurs() throws Exception {
        when(settlementService.getSettlements(1L))
                .thenThrow(new IllegalStateException("Les balances du groupe 1 ne sont pas équilibrées"));

        mockMvc.perform(get("/api/groups/1/settlements"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
    }
}

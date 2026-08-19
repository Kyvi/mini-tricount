package com.minitricount.balance;

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

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BalanceService balanceService;

    @Test
    void getBalances_returns200WithExpectedStructure_forPromptExample() throws Exception {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                new ParticipantBalance(1L, "Vincent", new BigDecimal("20.00")),
                new ParticipantBalance(2L, "Camille", new BigDecimal("-10.00")),
                new ParticipantBalance(3L, "Alice", new BigDecimal("-10.00"))
        ));

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].participantId").value(1))
                .andExpect(jsonPath("$[0].participantName").value("Vincent"))
                .andExpect(jsonPath("$[0].balance").value(20.00))
                .andExpect(jsonPath("$[1].balance").value(-10.00))
                .andExpect(jsonPath("$[2].balance").value(-10.00));
    }

    @Test
    void getBalances_returns200WithEmptyList_whenGroupHasNoParticipants() throws Exception {
        when(balanceService.getBalances(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBalances_returns404_whenGroupMissing() throws Exception {
        when(balanceService.getBalances(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(get("/api/groups/99/balances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getBalances_returns400_whenGroupIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/groups/abc/balances"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getBalances_returns500WithGenericMessage_whenUnexpectedExceptionOccurs() throws Exception {
        when(balanceService.getBalances(1L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/groups/1/balances"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
    }
}

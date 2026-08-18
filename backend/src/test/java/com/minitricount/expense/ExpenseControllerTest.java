package com.minitricount.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitricount.common.exception.BusinessRuleException;
import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.expense.dto.ExpenseRequest;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.participant.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    private static ExpenseGroup groupWithId(Long id, String name) {
        ExpenseGroup group = new ExpenseGroup(name);
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private static Participant participantWithId(Long id, ExpenseGroup group, String name) {
        Participant participant = new Participant(group, name);
        ReflectionTestUtils.setField(participant, "id", id);
        return participant;
    }

    private static Expense expenseWithId(Long id, ExpenseGroup group, String description, BigDecimal amount,
                                          LocalDate date, Participant paidBy) {
        Expense expense = new Expense(group, description, amount, date, paidBy);
        ReflectionTestUtils.setField(expense, "id", id);
        ReflectionTestUtils.setField(expense, "createdAt", Instant.parse("2026-08-16T10:00:00Z"));
        return expense;
    }

    @Test
    void create_returns201WithoutLocation_whenValid() throws Exception {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");
        Expense expense = expenseWithId(10L, group, "Courses", new BigDecimal("10.00"), LocalDate.of(2026, 8, 16), alice);
        expense.addShare(alice, new BigDecimal("5.00"));
        expense.addShare(bob, new BigDecimal("5.00"));

        when(expenseService.create(eq(1L), any())).thenReturn(expense);

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.of(2026, 8, 16), 1L, List.of(1L, 2L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.description").value("Courses"))
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.paidBy.name").value("Alice"))
                .andExpect(jsonPath("$.shares.length()").value(2))
                .andExpect(jsonPath("$.shares[0].participantName").value("Alice"))
                .andExpect(jsonPath("$.shares[0].shareAmount").value(5.00))
                .andExpect(jsonPath("$.shares[1].participantName").value("Bob"));
    }

    @Test
    void create_returns400WithFieldErrors_whenDescriptionBlank() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                " ", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void create_returns400WithFieldErrors_whenAmountInvalid() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("0.00"), LocalDate.now(), 1L, List.of(1L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void create_returns400WithFieldErrors_whenBeneficiariesContainDuplicates() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L, 2L, 1L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.beneficiaryParticipantIds").exists());
    }

    @Test
    void create_returns404_whenGroupMissing() throws Exception {
        when(expenseService.create(eq(1L), any()))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=1"));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns400_whenBusinessRuleViolated() throws Exception {
        when(expenseService.create(eq(1L), any()))
                .thenThrow(new BusinessRuleException("Participant(s) hors du groupe 1 : [2]"));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(2L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void create_returns400_whenGroupIdNotNumeric() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        mockMvc.perform(post("/api/groups/abc/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns500WithGenericMessage_whenUnexpectedExceptionOccurs() throws Exception {
        when(expenseService.create(eq(1L), any())).thenThrow(new RuntimeException("boom"));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        mockMvc.perform(post("/api/groups/1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue"));
    }

    @Test
    void findAll_returns200WithExpenseList() throws Exception {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Expense expense = expenseWithId(10L, group, "Courses", new BigDecimal("10.00"), LocalDate.of(2026, 8, 16), alice);
        expense.addShare(alice, new BigDecimal("10.00"));

        when(expenseService.findByGroup(1L)).thenReturn(List.of(expense));

        mockMvc.perform(get("/api/groups/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].shares[0].participantName").value("Alice"))
                .andExpect(jsonPath("$[0].shares[0].shareAmount").value(10.00));
    }

    @Test
    void findAll_returns200WithEmptyList_whenNoExpenses() throws Exception {
        when(expenseService.findByGroup(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/groups/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findAll_returns404_whenGroupMissing() throws Exception {
        when(expenseService.findByGroup(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        mockMvc.perform(get("/api/groups/99/expenses"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_returns400_whenGroupIdNotNumeric() throws Exception {
        mockMvc.perform(get("/api/groups/abc/expenses"))
                .andExpect(status().isBadRequest());
    }
}

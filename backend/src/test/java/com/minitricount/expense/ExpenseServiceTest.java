package com.minitricount.expense;

import com.minitricount.common.exception.BusinessRuleException;
import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.expense.dto.ExpenseRequest;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.group.ExpenseGroupService;
import com.minitricount.participant.Participant;
import com.minitricount.participant.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseGroupService expenseGroupService;

    @Mock
    private ParticipantRepository participantRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository, expenseGroupService, participantRepository);
    }

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
        return expense;
    }

    @Test
    void create_savesExpenseWithCorrectSharesAndPayer_whenValid() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");
        Participant carol = participantWithId(3L, group, "Carol");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(alice, bob, carol));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.of(2026, 8, 16), 1L, List.of(3L, 1L, 2L));

        Expense created = expenseService.create(1L, request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();

        assertThat(saved.getDescription()).isEqualTo("Courses");
        assertThat(saved.getAmount()).isEqualByComparingTo("10.00");
        assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(saved.getPaidBy()).isEqualTo(alice);
        assertThat(saved.getShares()).hasSize(3);
        assertThat(shareFor(saved, 1L)).isEqualByComparingTo("3.34");
        assertThat(shareFor(saved, 2L)).isEqualByComparingTo("3.33");
        assertThat(shareFor(saved, 3L)).isEqualByComparingTo("3.33");
        assertThat(created).isSameAs(saved);
    }

    @Test
    void create_succeeds_whenPayerIsAlsoBeneficiary() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(alice, bob));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseRequest request = new ExpenseRequest(
                "Diner", new BigDecimal("20.00"), LocalDate.now(), 1L, List.of(1L, 2L));

        Expense created = expenseService.create(1L, request);

        assertThat(created.getShares()).hasSize(2);
        assertThat(shareFor(created, 1L)).isEqualByComparingTo("10.00");
        assertThat(shareFor(created, 2L)).isEqualByComparingTo("10.00");
    }

    @Test
    void create_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        assertThatThrownBy(() -> expenseService.create(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(participantRepository, expenseRepository);
    }

    @Test
    void create_throwsBusinessRuleException_whenPayerNotInGroup() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant bob = participantWithId(2L, group, "Bob");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(bob));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(2L));

        assertThatThrownBy(() -> expenseService.create(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1");

        verifyNoInteractions(expenseRepository);
    }

    @Test
    void create_throwsBusinessRuleException_listingAllMissingBeneficiaries() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(alice));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(2L, 3L));

        assertThatThrownBy(() -> expenseService.create(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2")
                .hasMessageContaining("3");

        verifyNoInteractions(expenseRepository);
    }

    @Test
    void update_replacesDetailsAndRecalculatesShares_whenValid() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");
        Participant carol = participantWithId(3L, group, "Carol");

        Expense existing = expenseWithId(10L, group, "Old description", new BigDecimal("10.00"),
                LocalDate.of(2026, 8, 16), alice);
        existing.addShare(alice, new BigDecimal("10.00"));

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(expenseRepository.findByIdAndExpenseGroupId(10L, 1L)).thenReturn(Optional.of(existing));
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(bob, carol));

        ExpenseRequest request = new ExpenseRequest(
                "New description", new BigDecimal("9.00"), LocalDate.of(2026, 8, 20), 2L, List.of(2L, 3L));

        Expense updated = expenseService.update(1L, 10L, request);

        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getAmount()).isEqualByComparingTo("9.00");
        assertThat(updated.getExpenseDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(updated.getPaidBy()).isEqualTo(bob);
        assertThat(updated.getShares()).hasSize(2);
        assertThat(shareFor(updated, 2L)).isEqualByComparingTo("4.50");
        assertThat(shareFor(updated, 3L)).isEqualByComparingTo("4.50");
        assertThat(updated.getShares().stream().anyMatch(s -> s.getParticipant().getId().equals(1L))).isFalse();
        verify(expenseRepository).flush();
    }

    @Test
    void update_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        assertThatThrownBy(() -> expenseService.update(99L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(expenseRepository, participantRepository);
    }

    @Test
    void update_throwsResourceNotFound_whenExpenseMissingOrBelongsToAnotherGroup() {
        when(expenseGroupService.findById(1L)).thenReturn(groupWithId(1L, "Trip"));
        when(expenseRepository.findByIdAndExpenseGroupId(10L, 1L)).thenReturn(Optional.empty());

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 1L, List.of(1L));

        assertThatThrownBy(() -> expenseService.update(1L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(participantRepository);
    }

    @Test
    void update_throwsBusinessRuleException_whenPayerNotInGroup() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Expense existing = expenseWithId(10L, group, "Courses", new BigDecimal("10.00"), LocalDate.now(), alice);

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(expenseRepository.findByIdAndExpenseGroupId(10L, 1L)).thenReturn(Optional.of(existing));
        when(participantRepository.findByIdInAndExpenseGroupId(anyCollection(), eq(1L)))
                .thenReturn(List.of(alice));

        ExpenseRequest request = new ExpenseRequest(
                "Courses", new BigDecimal("10.00"), LocalDate.now(), 2L, List.of(1L));

        assertThatThrownBy(() -> expenseService.update(1L, 10L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2");

        verify(expenseRepository, never()).flush();
    }

    @Test
    void delete_deletesExpense_whenValid() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Expense existing = expenseWithId(10L, group, "Courses", new BigDecimal("10.00"), LocalDate.now(), alice);

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(expenseRepository.findByIdAndExpenseGroupId(10L, 1L)).thenReturn(Optional.of(existing));

        expenseService.delete(1L, 10L);

        verify(expenseRepository).delete(existing);
    }

    @Test
    void delete_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> expenseService.delete(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(expenseRepository);
    }

    @Test
    void delete_throwsResourceNotFound_whenExpenseMissingOrBelongsToAnotherGroup() {
        when(expenseGroupService.findById(1L)).thenReturn(groupWithId(1L, "Trip"));
        when(expenseRepository.findByIdAndExpenseGroupId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void findByGroup_returnsExpenses_whenGroupExists() {
        when(expenseGroupService.findById(1L)).thenReturn(groupWithId(1L, "Trip"));
        when(expenseRepository.findByExpenseGroupIdWithShares(1L)).thenReturn(List.of());

        assertThat(expenseService.findByGroup(1L)).isEmpty();
    }

    @Test
    void findByGroup_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> expenseService.findByGroup(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(expenseRepository);
    }

    private static BigDecimal shareFor(Expense expense, Long participantId) {
        return expense.getShares().stream()
                .filter(s -> s.getParticipant().getId().equals(participantId))
                .findFirst()
                .orElseThrow()
                .getShareAmount();
    }
}

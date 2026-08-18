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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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

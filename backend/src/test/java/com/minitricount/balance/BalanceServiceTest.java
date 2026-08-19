package com.minitricount.balance;

import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.expense.ExpenseRepository;
import com.minitricount.expense.ParticipantAmount;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.group.ExpenseGroupService;
import com.minitricount.participant.Participant;
import com.minitricount.participant.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private ExpenseGroupService expenseGroupService;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(expenseGroupService, participantRepository, expenseRepository);
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

    private record ParticipantAmountFixture(Long participantId, BigDecimal amount) implements ParticipantAmount {
        @Override
        public Long getParticipantId() {
            return participantId;
        }

        @Override
        public BigDecimal getAmount() {
            return amount;
        }
    }

    private static ParticipantAmount amountOf(Long participantId, String amount) {
        return new ParticipantAmountFixture(participantId, new BigDecimal(amount));
    }

    private static BigDecimal balanceOf(List<ParticipantBalance> balances, Long participantId) {
        return balances.stream()
                .filter(b -> b.participantId().equals(participantId))
                .findFirst()
                .orElseThrow()
                .balance();
    }

    @Test
    void getBalances_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> balanceService.getBalances(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(participantRepository, expenseRepository);
    }

    @Test
    void getBalances_returnsEmptyList_whenGroupHasNoParticipants() {
        when(expenseGroupService.findById(1L)).thenReturn(groupWithId(1L, "Trip"));
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of());
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of());
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of());

        assertThat(balanceService.getBalances(1L)).isEmpty();
    }

    @Test
    void getBalances_returnsZeroWithScaleTwo_whenParticipantsExistButNoExpenses() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(alice, bob));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of());
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of());

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balances).hasSize(2);
        for (ParticipantBalance balance : balances) {
            assertThat(balance.balance()).isEqualByComparingTo("0.00");
            assertThat(balance.balance().scale()).isEqualTo(2);
        }
    }

    @Test
    void getBalances_matchesPromptExample_equalSplitThreeWays() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");
        Participant alice = participantWithId(3L, group, "Alice");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(vincent, camille, alice));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(amountOf(1L, "30.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "10.00"), amountOf(2L, "10.00"), amountOf(3L, "10.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balanceOf(balances, 1L)).isEqualByComparingTo("20.00");
        assertThat(balanceOf(balances, 2L)).isEqualByComparingTo("-10.00");
        assertThat(balanceOf(balances, 3L)).isEqualByComparingTo("-10.00");
    }

    @Test
    void getBalances_handlesParticipantWhoIsOnlyPayer() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(vincent, camille));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(amountOf(1L, "15.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(amountOf(2L, "15.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balanceOf(balances, 1L)).isEqualByComparingTo("15.00");
    }

    @Test
    void getBalances_handlesParticipantWhoIsOnlyBeneficiary() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(vincent, camille));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(amountOf(1L, "15.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(amountOf(2L, "15.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balanceOf(balances, 2L)).isEqualByComparingTo("-15.00");
    }

    @Test
    void getBalances_handlesParticipantNeverInvolvedAmongOthersWithActivity() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");
        Participant bystander = participantWithId(3L, group, "Bystander");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L))
                .thenReturn(List.of(vincent, camille, bystander));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(amountOf(1L, "20.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "10.00"), amountOf(2L, "10.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balanceOf(balances, 3L)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(balances, 3L).scale()).isEqualTo(2);
    }

    @Test
    void getBalances_aggregatesCorrectly_acrossMultipleExpenses() {
        // Expense A: 30.00 payé par Vincent(1), réparti 1/2/3 = 10/10/10
        // Expense B: 12.00 payé par Camille(2), réparti 2/3 = 6/6
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");
        Participant alice = participantWithId(3L, group, "Alice");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(vincent, camille, alice));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "30.00"), amountOf(2L, "12.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "10.00"), amountOf(2L, "16.00"), amountOf(3L, "16.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balanceOf(balances, 1L)).isEqualByComparingTo("20.00");
        assertThat(balanceOf(balances, 2L)).isEqualByComparingTo("-4.00");
        assertThat(balanceOf(balances, 3L)).isEqualByComparingTo("-16.00");
    }

    @Test
    void getBalances_sumOfAllBalancesEqualsZero_forMultiExpenseScenario() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant vincent = participantWithId(1L, group, "Vincent");
        Participant camille = participantWithId(2L, group, "Camille");
        Participant alice = participantWithId(3L, group, "Alice");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(vincent, camille, alice));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "30.00"), amountOf(2L, "12.00")));
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of(
                amountOf(1L, "10.00"), amountOf(2L, "16.00"), amountOf(3L, "16.00")));

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        BigDecimal sum = balances.stream()
                .map(ParticipantBalance::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo("0.00");
    }

    @Test
    void getBalances_returnsParticipantsSortedById() {
        ExpenseGroup group = groupWithId(1L, "Trip");
        Participant carol = participantWithId(3L, group, "Carol");
        Participant alice = participantWithId(1L, group, "Alice");
        Participant bob = participantWithId(2L, group, "Bob");

        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(List.of(carol, alice, bob));
        when(expenseRepository.sumPaidAmountsByGroup(1L)).thenReturn(List.of());
        when(expenseRepository.sumShareAmountsByGroup(1L)).thenReturn(List.of());

        List<ParticipantBalance> balances = balanceService.getBalances(1L);

        assertThat(balances).extracting(ParticipantBalance::participantId)
                .containsExactly(1L, 2L, 3L);
    }
}

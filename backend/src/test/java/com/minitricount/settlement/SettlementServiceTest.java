package com.minitricount.settlement;

import com.minitricount.balance.BalanceService;
import com.minitricount.balance.ParticipantBalance;
import com.minitricount.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BalanceService balanceService;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(balanceService);
    }

    private static ParticipantBalance balance(Long id, String name, String amount) {
        return new ParticipantBalance(id, name, new BigDecimal(amount));
    }

    private static Settlement settlementBetween(List<Settlement> settlements, Long fromId, Long toId) {
        return settlements.stream()
                .filter(s -> s.fromParticipantId().equals(fromId) && s.toParticipantId().equals(toId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void getSettlements_propagatesException_whenGroupMissing() {
        when(balanceService.getBalances(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> settlementService.getSettlements(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSettlements_returnsEmptyList_whenGroupHasNoParticipants() {
        when(balanceService.getBalances(1L)).thenReturn(List.of());

        assertThat(settlementService.getSettlements(1L)).isEmpty();
    }

    @Test
    void getSettlements_returnsEmptyList_whenAllBalancesAreZero() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Alice", "0.00"),
                balance(2L, "Bob", "0.00")));

        assertThat(settlementService.getSettlements(1L)).isEmpty();
    }

    @Test
    void getSettlements_returnsSingleSettlement_forOneDebtorAndOneCreditor() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "20.00"),
                balance(2L, "Camille", "-20.00")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(1);
        assertThat(settlements.get(0).fromParticipantId()).isEqualTo(2L);
        assertThat(settlements.get(0).toParticipantId()).isEqualTo(1L);
        assertThat(settlements.get(0).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void getSettlements_splitsAcrossDebtors_forSeveralDebtorsAndOneCreditor() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "30.00"),
                balance(2L, "Camille", "-10.00"),
                balance(3L, "Alice", "-20.00")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(2);
        assertThat(settlementBetween(settlements, 2L, 1L).amount()).isEqualByComparingTo("10.00");
        assertThat(settlementBetween(settlements, 3L, 1L).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void getSettlements_splitsAcrossCreditors_forOneDebtorAndSeveralCreditors() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "10.00"),
                balance(2L, "Camille", "20.00"),
                balance(3L, "Alice", "-30.00")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(2);
        assertThat(settlementBetween(settlements, 3L, 1L).amount()).isEqualByComparingTo("10.00");
        assertThat(settlementBetween(settlements, 3L, 2L).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void getSettlements_matchesPromptExample() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "20.00"),
                balance(2L, "Camille", "-4.00"),
                balance(3L, "Alice", "-16.00")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(2);
        assertThat(settlementBetween(settlements, 2L, 1L).amount()).isEqualByComparingTo("4.00");
        assertThat(settlementBetween(settlements, 3L, 1L).amount()).isEqualByComparingTo("16.00");
        for (Settlement settlement : settlements) {
            assertThat(settlement.amount()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Test
    void getSettlements_preservesCentsPrecision() {
        // Dépense de 10.00 payée par Vincent(1), répartie équitablement entre Vincent/Camille/Alice (3.34/3.33/3.33)
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "6.66"),
                balance(2L, "Camille", "-3.33"),
                balance(3L, "Alice", "-3.33")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(2);
        assertThat(settlementBetween(settlements, 2L, 1L).amount()).isEqualByComparingTo("3.33");
        assertThat(settlementBetween(settlements, 3L, 1L).amount()).isEqualByComparingTo("3.33");
    }

    @Test
    void getSettlements_satisfiesPerParticipantInvariants_forMultipleDebtorsAndCreditors() {
        // Créanciers : A(+15.00), B(+9.00) — Débiteurs : C(-10.00), D(-14.00)
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "A", "15.00"),
                balance(2L, "B", "9.00"),
                balance(3L, "C", "-10.00"),
                balance(4L, "D", "-14.00")));

        List<Settlement> settlements = settlementService.getSettlements(1L);

        assertThat(settlements).hasSize(3);

        BigDecimal sentByC = sumByFrom(settlements, 3L);
        BigDecimal sentByD = sumByFrom(settlements, 4L);
        BigDecimal receivedByA = sumByTo(settlements, 1L);
        BigDecimal receivedByB = sumByTo(settlements, 2L);

        assertThat(sentByC).isEqualByComparingTo("10.00");
        assertThat(sentByD).isEqualByComparingTo("14.00");
        assertThat(receivedByA).isEqualByComparingTo("15.00");
        assertThat(receivedByB).isEqualByComparingTo("9.00");
    }

    @Test
    void getSettlements_isDeterministic_regardlessOfInputOrder() {
        List<ParticipantBalance> canonicalOrder = List.of(
                balance(1L, "A", "15.00"),
                balance(2L, "B", "9.00"),
                balance(3L, "C", "-10.00"),
                balance(4L, "D", "-14.00"));
        List<ParticipantBalance> shuffledOrder = List.of(
                balance(4L, "D", "-14.00"),
                balance(2L, "B", "9.00"),
                balance(3L, "C", "-10.00"),
                balance(1L, "A", "15.00"));

        when(balanceService.getBalances(1L)).thenReturn(canonicalOrder);
        List<Settlement> firstResult = settlementService.getSettlements(1L);

        when(balanceService.getBalances(1L)).thenReturn(shuffledOrder);
        List<Settlement> secondResult = settlementService.getSettlements(1L);

        assertThat(firstResult).isEqualTo(secondResult);
    }

    @Test
    void getSettlements_throwsIllegalStateException_whenBalancesAreNotBalanced() {
        when(balanceService.getBalances(1L)).thenReturn(List.of(
                balance(1L, "Vincent", "20.00"),
                balance(2L, "Camille", "-5.00")));

        assertThatThrownBy(() -> settlementService.getSettlements(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    private static BigDecimal sumByFrom(List<Settlement> settlements, Long fromParticipantId) {
        return settlements.stream()
                .filter(s -> s.fromParticipantId().equals(fromParticipantId))
                .map(Settlement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumByTo(List<Settlement> settlements, Long toParticipantId) {
        return settlements.stream()
                .filter(s -> s.toParticipantId().equals(toParticipantId))
                .map(Settlement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

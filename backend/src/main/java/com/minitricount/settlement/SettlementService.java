package com.minitricount.settlement;

import com.minitricount.balance.BalanceService;
import com.minitricount.balance.ParticipantBalance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SettlementService {

    private final BalanceService balanceService;

    public SettlementService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    public List<Settlement> getSettlements(Long groupId) {
        List<ParticipantBalance> balances = balanceService.getBalances(groupId);

        assertBalanced(balances, groupId);

        List<ParticipantBalance> creditors = balances.stream()
                .filter(b -> b.balance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(ParticipantBalance::participantId))
                .toList();
        List<ParticipantBalance> debtors = balances.stream()
                .filter(b -> b.balance().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Comparator.comparing(ParticipantBalance::participantId))
                .toList();

        return sweep(creditors, debtors);
    }

    private static void assertBalanced(List<ParticipantBalance> balances, Long groupId) {
        BigDecimal total = balances.stream()
                .map(ParticipantBalance::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException(
                    "Les balances du groupe " + groupId + " ne sont pas équilibrées (somme = " + total + ")");
        }
    }

    private static List<Settlement> sweep(List<ParticipantBalance> creditors, List<ParticipantBalance> debtors) {
        List<Settlement> settlements = new ArrayList<>();

        int creditorIndex = 0;
        int debtorIndex = 0;
        BigDecimal creditRemaining = creditorIndex < creditors.size()
                ? creditors.get(creditorIndex).balance() : BigDecimal.ZERO;
        BigDecimal debtRemaining = debtorIndex < debtors.size()
                ? debtors.get(debtorIndex).balance().abs() : BigDecimal.ZERO;

        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            ParticipantBalance creditor = creditors.get(creditorIndex);
            ParticipantBalance debtor = debtors.get(debtorIndex);
            BigDecimal amount = creditRemaining.min(debtRemaining);

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                settlements.add(new Settlement(
                        debtor.participantId(), debtor.participantName(),
                        creditor.participantId(), creditor.participantName(),
                        amount));
            }

            creditRemaining = creditRemaining.subtract(amount);
            debtRemaining = debtRemaining.subtract(amount);

            if (creditRemaining.compareTo(BigDecimal.ZERO) == 0) {
                creditorIndex++;
                creditRemaining = creditorIndex < creditors.size()
                        ? creditors.get(creditorIndex).balance() : BigDecimal.ZERO;
            }
            if (debtRemaining.compareTo(BigDecimal.ZERO) == 0) {
                debtorIndex++;
                debtRemaining = debtorIndex < debtors.size()
                        ? debtors.get(debtorIndex).balance().abs() : BigDecimal.ZERO;
            }
        }

        return settlements;
    }
}

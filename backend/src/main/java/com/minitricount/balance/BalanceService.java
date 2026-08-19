package com.minitricount.balance;

import com.minitricount.expense.ExpenseRepository;
import com.minitricount.expense.ParticipantAmount;
import com.minitricount.group.ExpenseGroupService;
import com.minitricount.participant.Participant;
import com.minitricount.participant.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BalanceService {

    private final ExpenseGroupService expenseGroupService;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;

    public BalanceService(ExpenseGroupService expenseGroupService,
                           ParticipantRepository participantRepository,
                           ExpenseRepository expenseRepository) {
        this.expenseGroupService = expenseGroupService;
        this.participantRepository = participantRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<ParticipantBalance> getBalances(Long groupId) {
        expenseGroupService.findById(groupId);

        List<Participant> participants = participantRepository.findByExpenseGroupId(groupId);

        Map<Long, BigDecimal> paid = toMap(expenseRepository.sumPaidAmountsByGroup(groupId));
        Map<Long, BigDecimal> owed = toMap(expenseRepository.sumShareAmountsByGroup(groupId));

        return participants.stream()
                .sorted(Comparator.comparing(Participant::getId))
                .map(p -> new ParticipantBalance(p.getId(), p.getName(), balanceFor(p, paid, owed)))
                .toList();
    }

    private static BigDecimal balanceFor(Participant participant, Map<Long, BigDecimal> paid, Map<Long, BigDecimal> owed) {
        return paid.getOrDefault(participant.getId(), BigDecimal.ZERO)
                .subtract(owed.getOrDefault(participant.getId(), BigDecimal.ZERO))
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    private static Map<Long, BigDecimal> toMap(List<ParticipantAmount> amounts) {
        return amounts.stream()
                .collect(Collectors.toMap(ParticipantAmount::getParticipantId, ParticipantAmount::getAmount));
    }
}

package com.minitricount.expense;

import com.minitricount.common.exception.BusinessRuleException;
import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.expense.dto.ExpenseRequest;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.group.ExpenseGroupService;
import com.minitricount.participant.Participant;
import com.minitricount.participant.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseGroupService expenseGroupService;
    private final ParticipantRepository participantRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                           ExpenseGroupService expenseGroupService,
                           ParticipantRepository participantRepository) {
        this.expenseRepository = expenseRepository;
        this.expenseGroupService = expenseGroupService;
        this.participantRepository = participantRepository;
    }

    @Transactional
    public Expense create(Long groupId, ExpenseRequest request) {
        ExpenseGroup group = expenseGroupService.findById(groupId);

        Map<Long, Participant> byId = resolveParticipants(groupId, requestedParticipantIds(request));

        Map<Long, BigDecimal> sharesByParticipantId =
                EqualSplitCalculator.split(request.amount(), request.beneficiaryParticipantIds());

        Expense expense = new Expense(group, request.description(), request.amount(),
                request.expenseDate(), byId.get(request.paidByParticipantId()));
        sharesByParticipantId.forEach((id, shareAmount) -> expense.addShare(byId.get(id), shareAmount));

        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense update(Long groupId, Long expenseId, ExpenseRequest request) {
        expenseGroupService.findById(groupId);
        Expense expense = expenseRepository.findByIdAndExpenseGroupId(expenseId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dépense introuvable : id=" + expenseId + " dans le groupe " + groupId));

        Map<Long, Participant> byId = resolveParticipants(groupId, requestedParticipantIds(request));

        Map<Long, BigDecimal> sharesByParticipantId =
                EqualSplitCalculator.split(request.amount(), request.beneficiaryParticipantIds());

        expense.replaceDetails(request.description(), request.amount(), request.expenseDate(),
                byId.get(request.paidByParticipantId()));
        expense.clearShares();
        // Flush intermédiaire : force l'exécution des DELETE des anciennes shares avant les INSERT
        // des nouvelles, afin de ne pas dépendre de l'ordre implicite du flush Hibernate face à la
        // contrainte unique (expense_id, participant_id) lorsqu'un participant reste bénéficiaire
        // avant et après la modification.
        expenseRepository.flush();
        sharesByParticipantId.forEach((id, shareAmount) -> expense.addShare(byId.get(id), shareAmount));

        return expense;
    }

    @Transactional
    public void delete(Long groupId, Long expenseId) {
        expenseGroupService.findById(groupId);
        Expense expense = expenseRepository.findByIdAndExpenseGroupId(expenseId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dépense introuvable : id=" + expenseId + " dans le groupe " + groupId));

        expenseRepository.delete(expense);
    }

    public List<Expense> findByGroup(Long groupId) {
        expenseGroupService.findById(groupId);
        return expenseRepository.findByExpenseGroupIdWithShares(groupId);
    }

    private static Set<Long> requestedParticipantIds(ExpenseRequest request) {
        Set<Long> requestedIds = new LinkedHashSet<>();
        requestedIds.add(request.paidByParticipantId());
        requestedIds.addAll(request.beneficiaryParticipantIds());
        return requestedIds;
    }

    private Map<Long, Participant> resolveParticipants(Long groupId, Set<Long> requestedIds) {
        Map<Long, Participant> byId = participantRepository
                .findByIdInAndExpenseGroupId(requestedIds, groupId).stream()
                .collect(Collectors.toMap(Participant::getId, Function.identity()));

        if (byId.size() != requestedIds.size()) {
            Set<Long> missing = new LinkedHashSet<>(requestedIds);
            missing.removeAll(byId.keySet());
            throw new BusinessRuleException(
                    "Participant(s) hors du groupe " + groupId + " : " + missing);
        }

        return byId;
    }
}

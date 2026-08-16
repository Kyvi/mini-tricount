package com.minitricount.participant;

import com.minitricount.group.ExpenseGroup;
import com.minitricount.group.ExpenseGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ExpenseGroupService expenseGroupService;

    public ParticipantService(ParticipantRepository participantRepository, ExpenseGroupService expenseGroupService) {
        this.participantRepository = participantRepository;
        this.expenseGroupService = expenseGroupService;
    }

    public Participant addToGroup(Long groupId, String name) {
        ExpenseGroup group = expenseGroupService.findById(groupId);
        return participantRepository.save(new Participant(group, name));
    }

    public List<Participant> findByGroup(Long groupId) {
        expenseGroupService.findById(groupId);
        return participantRepository.findByExpenseGroupId(groupId);
    }
}

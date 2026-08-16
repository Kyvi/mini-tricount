package com.minitricount.group;

import com.minitricount.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseGroupService {

    private final ExpenseGroupRepository expenseGroupRepository;

    public ExpenseGroupService(ExpenseGroupRepository expenseGroupRepository) {
        this.expenseGroupRepository = expenseGroupRepository;
    }

    public ExpenseGroup create(String name) {
        return expenseGroupRepository.save(new ExpenseGroup(name));
    }

    public List<ExpenseGroup> findAll() {
        return expenseGroupRepository.findAll();
    }

    public ExpenseGroup findById(Long groupId) {
        return expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable : id=" + groupId));
    }
}

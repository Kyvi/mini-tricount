package com.minitricount.expense;

import com.minitricount.expense.dto.ExpenseRequest;
import com.minitricount.expense.dto.ExpenseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@PathVariable Long groupId,
                                                    @Valid @RequestBody ExpenseRequest request) {
        Expense created = expenseService.create(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseResponse.from(created));
    }

    @GetMapping
    public List<ExpenseResponse> findAll(@PathVariable Long groupId) {
        return expenseService.findByGroup(groupId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@PathVariable Long groupId, @PathVariable Long expenseId,
                                   @Valid @RequestBody ExpenseRequest request) {
        Expense updated = expenseService.update(groupId, expenseId, request);
        return ExpenseResponse.from(updated);
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.delete(groupId, expenseId);
        return ResponseEntity.noContent().build();
    }
}

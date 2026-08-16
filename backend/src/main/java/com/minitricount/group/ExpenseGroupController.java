package com.minitricount.group;

import com.minitricount.group.dto.ExpenseGroupRequest;
import com.minitricount.group.dto.ExpenseGroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class ExpenseGroupController {

    private final ExpenseGroupService expenseGroupService;

    public ExpenseGroupController(ExpenseGroupService expenseGroupService) {
        this.expenseGroupService = expenseGroupService;
    }

    @PostMapping
    public ResponseEntity<ExpenseGroupResponse> create(@Valid @RequestBody ExpenseGroupRequest request) {
        ExpenseGroup created = expenseGroupService.create(request.name());
        ExpenseGroupResponse body = ExpenseGroupResponse.from(created);
        return ResponseEntity.created(URI.create("/api/groups/" + created.getId())).body(body);
    }

    @GetMapping
    public List<ExpenseGroupResponse> findAll() {
        return expenseGroupService.findAll().stream()
                .map(ExpenseGroupResponse::from)
                .toList();
    }

    @GetMapping("/{groupId}")
    public ExpenseGroupResponse findById(@PathVariable Long groupId) {
        return ExpenseGroupResponse.from(expenseGroupService.findById(groupId));
    }
}

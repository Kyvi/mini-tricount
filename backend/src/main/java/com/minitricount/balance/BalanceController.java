package com.minitricount.balance;

import com.minitricount.balance.dto.ParticipantBalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public List<ParticipantBalanceResponse> getBalances(@PathVariable Long groupId) {
        return balanceService.getBalances(groupId).stream()
                .map(ParticipantBalanceResponse::from)
                .toList();
    }
}

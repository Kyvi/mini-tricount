package com.minitricount.settlement;

import com.minitricount.settlement.dto.SettlementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public List<SettlementResponse> getSettlements(@PathVariable Long groupId) {
        return settlementService.getSettlements(groupId).stream()
                .map(SettlementResponse::from)
                .toList();
    }
}

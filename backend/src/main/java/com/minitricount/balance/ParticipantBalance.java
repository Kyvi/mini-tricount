package com.minitricount.balance;

import java.math.BigDecimal;

public record ParticipantBalance(Long participantId, String participantName, BigDecimal balance) {
}

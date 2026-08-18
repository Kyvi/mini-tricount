package com.minitricount.expense;

import com.minitricount.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_share",
        uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "participant_id"}))
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "share_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal shareAmount;

    protected ExpenseShare() {
    }

    ExpenseShare(Expense expense, Participant participant, BigDecimal shareAmount) {
        this.expense = expense;
        this.participant = participant;
        this.shareAmount = shareAmount;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public Participant getParticipant() {
        return participant;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }
}

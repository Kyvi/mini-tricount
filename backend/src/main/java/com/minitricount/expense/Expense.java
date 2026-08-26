package com.minitricount.expense;

import com.minitricount.group.ExpenseGroup;
import com.minitricount.participant.Participant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_group_id", nullable = false)
    private ExpenseGroup expenseGroup;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_participant_id", nullable = false)
    private Participant paidBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("participant.id ASC")
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup expenseGroup, String description, BigDecimal amount,
                   LocalDate expenseDate, Participant paidBy) {
        this.expenseGroup = expenseGroup;
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.paidBy = paidBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addShare(Participant participant, BigDecimal shareAmount) {
        shares.add(new ExpenseShare(this, participant, shareAmount));
    }

    public void replaceDetails(String description, BigDecimal amount, LocalDate expenseDate, Participant paidBy) {
        this.description = description;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.paidBy = paidBy;
    }

    public void clearShares() {
        shares.clear();
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getExpenseGroup() {
        return expenseGroup;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public Participant getPaidBy() {
        return paidBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExpenseShare> getShares() {
        return shares;
    }
}

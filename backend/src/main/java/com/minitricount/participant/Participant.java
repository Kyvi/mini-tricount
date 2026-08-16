package com.minitricount.participant;

import com.minitricount.group.ExpenseGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_group_id", nullable = false)
    private ExpenseGroup expenseGroup;

    @Column(nullable = false)
    private String name;

    protected Participant() {
    }

    public Participant(ExpenseGroup expenseGroup, String name) {
        this.expenseGroup = expenseGroup;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getExpenseGroup() {
        return expenseGroup;
    }

    public String getName() {
        return name;
    }
}

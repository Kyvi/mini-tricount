package com.minitricount.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT DISTINCT e FROM Expense e
        JOIN FETCH e.paidBy
        LEFT JOIN FETCH e.shares s
        LEFT JOIN FETCH s.participant
        WHERE e.expenseGroup.id = :groupId
        ORDER BY e.id
        """)
    List<Expense> findByExpenseGroupIdWithShares(@Param("groupId") Long groupId);
}

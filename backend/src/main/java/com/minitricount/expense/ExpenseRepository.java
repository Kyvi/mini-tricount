package com.minitricount.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndExpenseGroupId(Long id, Long expenseGroupId);

    @Query("""
        SELECT DISTINCT e FROM Expense e
        JOIN FETCH e.paidBy
        LEFT JOIN FETCH e.shares s
        LEFT JOIN FETCH s.participant
        WHERE e.expenseGroup.id = :groupId
        ORDER BY e.id
        """)
    List<Expense> findByExpenseGroupIdWithShares(@Param("groupId") Long groupId);

    @Query("""
        SELECT e.paidBy.id AS participantId, SUM(e.amount) AS amount
        FROM Expense e
        WHERE e.expenseGroup.id = :groupId
        GROUP BY e.paidBy.id
        """)
    List<ParticipantAmount> sumPaidAmountsByGroup(@Param("groupId") Long groupId);

    @Query("""
        SELECT s.participant.id AS participantId, SUM(s.shareAmount) AS amount
        FROM Expense e JOIN e.shares s
        WHERE e.expenseGroup.id = :groupId
        GROUP BY s.participant.id
        """)
    List<ParticipantAmount> sumShareAmountsByGroup(@Param("groupId") Long groupId);
}

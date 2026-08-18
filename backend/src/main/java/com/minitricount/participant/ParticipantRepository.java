package com.minitricount.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByExpenseGroupId(Long expenseGroupId);

    List<Participant> findByIdInAndExpenseGroupId(Collection<Long> ids, Long expenseGroupId);
}

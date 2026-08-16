package com.minitricount.participant;

import com.minitricount.common.exception.ResourceNotFoundException;
import com.minitricount.group.ExpenseGroup;
import com.minitricount.group.ExpenseGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ExpenseGroupService expenseGroupService;

    private ParticipantService participantService;

    @BeforeEach
    void setUp() {
        participantService = new ParticipantService(participantRepository, expenseGroupService);
    }

    @Test
    void addToGroup_savesParticipant_whenGroupExists() {
        ExpenseGroup group = new ExpenseGroup("Colocation");
        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.save(any(Participant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Participant created = participantService.addToGroup(1L, "Alice");

        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
        assertThat(captor.getValue().getExpenseGroup()).isEqualTo(group);
        assertThat(created.getName()).isEqualTo("Alice");
    }

    @Test
    void addToGroup_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> participantService.addToGroup(99L, "Alice"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(participantRepository);
    }

    @Test
    void findByGroup_returnsParticipants_whenGroupExists() {
        ExpenseGroup group = new ExpenseGroup("Colocation");
        List<Participant> participants = List.of(new Participant(group, "Alice"), new Participant(group, "Bob"));
        when(expenseGroupService.findById(1L)).thenReturn(group);
        when(participantRepository.findByExpenseGroupId(1L)).thenReturn(participants);

        assertThat(participantService.findByGroup(1L)).containsExactlyElementsOf(participants);
    }

    @Test
    void findByGroup_throwsResourceNotFound_whenGroupMissing() {
        when(expenseGroupService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Groupe introuvable : id=99"));

        assertThatThrownBy(() -> participantService.findByGroup(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(participantRepository);
    }
}

package com.minitricount.group;

import com.minitricount.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseGroupServiceTest {

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    private ExpenseGroupService expenseGroupService;

    @BeforeEach
    void setUp() {
        expenseGroupService = new ExpenseGroupService(expenseGroupRepository);
    }

    @Test
    void create_savesGroupWithGivenName() {
        when(expenseGroupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseGroup created = expenseGroupService.create("Weekend à Lyon");

        ArgumentCaptor<ExpenseGroup> captor = ArgumentCaptor.forClass(ExpenseGroup.class);
        verify(expenseGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Weekend à Lyon");
        assertThat(created.getName()).isEqualTo("Weekend à Lyon");
    }

    @Test
    void findAll_returnsAllGroupsFromRepository() {
        List<ExpenseGroup> groups = List.of(new ExpenseGroup("A"), new ExpenseGroup("B"));
        when(expenseGroupRepository.findAll()).thenReturn(groups);

        assertThat(expenseGroupService.findAll()).containsExactlyElementsOf(groups);
    }

    @Test
    void findById_returnsGroup_whenPresent() {
        ExpenseGroup group = new ExpenseGroup("Colocation");
        when(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThat(expenseGroupService.findById(1L)).isEqualTo(group);
    }

    @Test
    void findById_throwsResourceNotFound_whenAbsent() {
        when(expenseGroupRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseGroupService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}

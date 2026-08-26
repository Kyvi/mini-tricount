import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiHttpError } from '../api/groups';
import type { Expense, Participant } from '../types/api';
import { ExpenseList } from './ExpenseList';

vi.mock('../api/groups', async () => {
  const actual = await vi.importActual<typeof import('../api/groups')>('../api/groups');
  return {
    ...actual,
    deleteExpense: vi.fn(),
  };
});

vi.mock('./ExpenseForm', () => ({
  ExpenseForm: ({ expense }: { expense?: Expense }) => (
    <div data-testid="expense-form">{expense?.description}</div>
  ),
}));

import { deleteExpense } from '../api/groups';

const participants: Participant[] = [
  { id: 1, name: 'Alice' },
  { id: 2, name: 'Bob' },
];

const expense: Expense = {
  id: 10,
  description: 'Restaurant',
  amount: 30,
  expenseDate: '2026-08-24',
  createdAt: '2026-08-24T10:00:00Z',
  paidBy: { id: 1, name: 'Alice' },
  shares: [
    { participantId: 1, participantName: 'Alice', shareAmount: 15 },
    { participantId: 2, participantName: 'Bob', shareAmount: 15 },
  ],
};

function renderList(overrides: Partial<Parameters<typeof ExpenseList>[0]> = {}) {
  return render(
    <ExpenseList
      groupId={3}
      participants={participants}
      expenses={[expense]}
      editingExpenseId={null}
      onEditRequested={vi.fn()}
      onEditSaved={vi.fn()}
      onEditCancelled={vi.fn()}
      onDeleted={vi.fn()}
      {...overrides}
    />,
  );
}

describe('ExpenseList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, 'confirm');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('supprime la dépense quand la confirmation est acceptée', async () => {
    const user = userEvent.setup();
    vi.mocked(window.confirm).mockReturnValue(true);
    vi.mocked(deleteExpense).mockResolvedValueOnce(undefined);
    const onDeleted = vi.fn();

    renderList({ onDeleted });

    await user.click(screen.getByRole('button', { name: 'Supprimer' }));

    expect(deleteExpense).toHaveBeenCalledWith(3, 10);
    expect(onDeleted).toHaveBeenCalledWith(10);
  });

  it("n'appelle pas l'API quand la confirmation est annulée", async () => {
    const user = userEvent.setup();
    vi.mocked(window.confirm).mockReturnValue(false);
    const onDeleted = vi.fn();

    renderList({ onDeleted });

    await user.click(screen.getByRole('button', { name: 'Supprimer' }));

    expect(deleteExpense).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("affiche un message d'erreur en ligne si la suppression échoue", async () => {
    const user = userEvent.setup();
    vi.mocked(window.confirm).mockReturnValue(true);
    vi.mocked(deleteExpense).mockRejectedValueOnce(new ApiHttpError(404, 'Dépense introuvable'));
    const onDeleted = vi.fn();

    renderList({ onDeleted });

    await user.click(screen.getByRole('button', { name: 'Supprimer' }));

    expect(await screen.findByText('Dépense introuvable')).toBeInTheDocument();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it('affiche le formulaire d’édition uniquement pour la ligne correspondant à editingExpenseId', () => {
    renderList({ editingExpenseId: 10 });

    expect(screen.getByTestId('expense-form')).toHaveTextContent('Restaurant');
    expect(screen.queryByRole('button', { name: 'Modifier' })).not.toBeInTheDocument();
  });
});

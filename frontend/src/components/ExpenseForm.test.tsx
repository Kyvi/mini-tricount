import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiHttpError } from '../api/groups';
import type { Expense, Participant } from '../types/api';
import { ExpenseForm } from './ExpenseForm';

vi.mock('../api/groups', async () => {
  const actual = await vi.importActual<typeof import('../api/groups')>('../api/groups');
  return {
    ...actual,
    createExpense: vi.fn(),
    updateExpense: vi.fn(),
  };
});

import { createExpense, updateExpense } from '../api/groups';

const participants: Participant[] = [
  { id: 1, name: 'Alice' },
  { id: 2, name: 'Bob' },
];

async function fillRequiredFields(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Description'), 'Restaurant');
  await user.type(screen.getByLabelText('Montant'), '30');
  await user.selectOptions(screen.getByLabelText('Payeur'), '1');
}

describe('ExpenseForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("bloque la soumission et n'appelle pas l'API si aucun bénéficiaire n'est coché", async () => {
    const user = userEvent.setup();
    const onSaved = vi.fn();
    render(
      <ExpenseForm groupId={3} participants={participants} onSaved={onSaved} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    await user.click(screen.getByLabelText('Alice'));
    await user.click(screen.getByLabelText('Bob'));
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(screen.getByText('Sélectionnez au moins un bénéficiaire')).toBeInTheDocument();
    expect(createExpense).not.toHaveBeenCalled();
    expect(onSaved).not.toHaveBeenCalled();
  });

  it('appelle onSaved avec la dépense retournée après un succès', async () => {
    const user = userEvent.setup();
    const created: Expense = {
      id: 42,
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
    vi.mocked(createExpense).mockResolvedValueOnce(created);
    const onSaved = vi.fn();

    render(
      <ExpenseForm groupId={3} participants={participants} onSaved={onSaved} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(onSaved).toHaveBeenCalledWith(created);
  });

  it('affiche le message de champ sous le bon input en cas de 400 avec fieldErrors', async () => {
    const user = userEvent.setup();
    vi.mocked(createExpense).mockRejectedValueOnce(
      new ApiHttpError(400, 'Validation échouée', { description: 'doit être renseigné' }),
    );

    render(
      <ExpenseForm groupId={3} participants={participants} onSaved={vi.fn()} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    const descriptionInput = screen.getByLabelText('Description');
    const errorMessage = await screen.findByText('doit être renseigné');
    expect(descriptionInput).toHaveAccessibleDescription('doit être renseigné');
    expect(errorMessage).toBeInTheDocument();
  });

  it("n'appelle createExpense qu'une seule fois si le formulaire est soumis pendant qu'une soumission est déjà en cours", async () => {
    const user = userEvent.setup();
    vi.mocked(createExpense).mockImplementationOnce(() => new Promise<Expense>(() => {}));

    const { container } = render(
      <ExpenseForm groupId={3} participants={participants} onSaved={vi.fn()} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    const form = container.querySelector('form');
    if (!form) throw new Error('form introuvable');

    fireEvent.submit(form);
    fireEvent.submit(form);

    expect(createExpense).toHaveBeenCalledTimes(1);
  });

  it('préremplit le formulaire à partir de la dépense fournie en mode édition', () => {
    const expense: Expense = {
      id: 7,
      description: 'Courses',
      amount: 12.5,
      expenseDate: '2026-08-20',
      createdAt: '2026-08-20T10:00:00Z',
      paidBy: { id: 2, name: 'Bob' },
      shares: [
        { participantId: 1, participantName: 'Alice', shareAmount: 6.25 },
        { participantId: 2, participantName: 'Bob', shareAmount: 6.25 },
      ],
    };

    render(
      <ExpenseForm
        groupId={3}
        participants={participants}
        expense={expense}
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('Description')).toHaveValue('Courses');
    expect(screen.getByLabelText('Montant')).toHaveValue(12.5);
    expect(screen.getByLabelText('Date')).toHaveValue('2026-08-20');
    expect(screen.getByLabelText('Payeur')).toHaveValue('2');
    expect(screen.getByLabelText('Alice')).toBeChecked();
    expect(screen.getByLabelText('Bob')).toBeChecked();
  });

  it('appelle updateExpense (et non createExpense) en mode édition', async () => {
    const user = userEvent.setup();
    const expense: Expense = {
      id: 7,
      description: 'Courses',
      amount: 12.5,
      expenseDate: '2026-08-20',
      createdAt: '2026-08-20T10:00:00Z',
      paidBy: { id: 1, name: 'Alice' },
      shares: [{ participantId: 1, participantName: 'Alice', shareAmount: 12.5 }],
    };
    const updated: Expense = { ...expense, description: 'Courses modifiées' };
    vi.mocked(updateExpense).mockResolvedValueOnce(updated);
    const onSaved = vi.fn();

    render(
      <ExpenseForm
        groupId={3}
        participants={participants}
        expense={expense}
        onSaved={onSaved}
        onCancel={vi.fn()}
      />,
    );

    const descriptionInput = screen.getByLabelText('Description');
    await user.clear(descriptionInput);
    await user.type(descriptionInput, 'Courses modifiées');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(updateExpense).toHaveBeenCalledWith(3, 7, {
      description: 'Courses modifiées',
      amount: 12.5,
      expenseDate: '2026-08-20',
      paidByParticipantId: 1,
      beneficiaryParticipantIds: [1],
    });
    expect(createExpense).not.toHaveBeenCalled();
    expect(onSaved).toHaveBeenCalledWith(updated);
  });
});

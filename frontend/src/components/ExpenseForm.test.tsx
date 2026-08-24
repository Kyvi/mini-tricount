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
  };
});

import { createExpense } from '../api/groups';

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
    const onCreated = vi.fn();
    render(
      <ExpenseForm groupId={3} participants={participants} onCreated={onCreated} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    await user.click(screen.getByLabelText('Alice'));
    await user.click(screen.getByLabelText('Bob'));
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(screen.getByText('Sélectionnez au moins un bénéficiaire')).toBeInTheDocument();
    expect(createExpense).not.toHaveBeenCalled();
    expect(onCreated).not.toHaveBeenCalled();
  });

  it('appelle onCreated avec la dépense retournée après un succès', async () => {
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
    const onCreated = vi.fn();

    render(
      <ExpenseForm groupId={3} participants={participants} onCreated={onCreated} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(onCreated).toHaveBeenCalledWith(created);
  });

  it('affiche le message de champ sous le bon input en cas de 400 avec fieldErrors', async () => {
    const user = userEvent.setup();
    vi.mocked(createExpense).mockRejectedValueOnce(
      new ApiHttpError(400, 'Validation échouée', { description: 'doit être renseigné' }),
    );

    render(
      <ExpenseForm groupId={3} participants={participants} onCreated={vi.fn()} onCancel={vi.fn()} />,
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
      <ExpenseForm groupId={3} participants={participants} onCreated={vi.fn()} onCancel={vi.fn()} />,
    );

    await fillRequiredFields(user);
    const form = container.querySelector('form');
    if (!form) throw new Error('form introuvable');

    fireEvent.submit(form);
    fireEvent.submit(form);

    expect(createExpense).toHaveBeenCalledTimes(1);
  });
});

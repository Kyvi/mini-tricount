import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Expense, Group, Participant, ParticipantBalance, Settlement } from '../types/api';
import { GroupView } from './GroupView';

vi.mock('../api/groups', async () => {
  const actual = await vi.importActual<typeof import('../api/groups')>('../api/groups');
  return {
    ...actual,
    fetchGroup: vi.fn(),
    fetchParticipants: vi.fn(),
    fetchExpenses: vi.fn(),
    fetchBalances: vi.fn(),
    fetchSettlements: vi.fn(),
    createExpense: vi.fn(),
    createParticipant: vi.fn(),
  };
});

import {
  createExpense,
  createParticipant,
  fetchBalances,
  fetchExpenses,
  fetchGroup,
  fetchParticipants,
  fetchSettlements,
} from '../api/groups';

const group: Group = { id: 3, name: 'Vacances', createdAt: '2026-08-01T00:00:00Z' };
const participants: Participant[] = [{ id: 1, name: 'Alice' }];
const balances: ParticipantBalance[] = [{ participantId: 1, participantName: 'Alice', balance: 0 }];
const settlements: Settlement[] = [];

describe('GroupView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(fetchGroup).mockResolvedValue(group);
    vi.mocked(fetchParticipants).mockResolvedValue(participants);
    vi.mocked(fetchExpenses).mockResolvedValue([]);
    vi.mocked(fetchBalances).mockResolvedValue(balances);
    vi.mocked(fetchSettlements).mockResolvedValue(settlements);
  });

  it('rappelle fetchBalances et fetchSettlements après la création réussie d\'une dépense', async () => {
    const user = userEvent.setup();
    const created: Expense = {
      id: 1,
      description: 'Restaurant',
      amount: 30,
      expenseDate: '2026-08-26',
      createdAt: '2026-08-26T10:00:00Z',
      paidBy: { id: 1, name: 'Alice' },
      shares: [{ participantId: 1, participantName: 'Alice', shareAmount: 30 }],
    };
    vi.mocked(createExpense).mockResolvedValueOnce(created);

    render(<GroupView groupId={3} />);

    await screen.findByText('Vacances');
    expect(fetchBalances).toHaveBeenCalledTimes(1);
    expect(fetchSettlements).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: 'Ajouter une dépense' }));
    await user.type(screen.getByLabelText('Description'), 'Restaurant');
    await user.type(screen.getByLabelText('Montant'), '30');
    await user.selectOptions(screen.getByLabelText('Payeur'), '1');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await screen.findByText('Restaurant');
    expect(screen.getByText('30.00 €')).toBeInTheDocument();
    expect(screen.getByText('2026-08-26 — payé par Alice')).toBeInTheDocument();

    expect(fetchBalances).toHaveBeenCalledTimes(2);
    expect(fetchSettlements).toHaveBeenCalledTimes(2);
  });

  it("ignore le résultat d'un rafraîchissement obsolète si un plus récent a déjà répondu", async () => {
    const user = userEvent.setup();

    let resolveFirstRefresh!: (value: ParticipantBalance[]) => void;
    let resolveSecondRefresh!: (value: ParticipantBalance[]) => void;
    const firstRefresh = new Promise<ParticipantBalance[]>((resolve) => {
      resolveFirstRefresh = resolve;
    });
    const secondRefresh = new Promise<ParticipantBalance[]>((resolve) => {
      resolveSecondRefresh = resolve;
    });

    vi.mocked(fetchBalances)
      .mockResolvedValueOnce(balances) // chargement initial
      .mockImplementationOnce(() => firstRefresh) // déclenché en premier, répond en dernier
      .mockImplementationOnce(() => secondRefresh); // déclenché en second, répond en premier

    vi.mocked(createParticipant)
      .mockResolvedValueOnce({ id: 2, name: 'Bob' })
      .mockResolvedValueOnce({ id: 3, name: 'Camille' });

    render(<GroupView groupId={3} />);
    await screen.findByText('Vacances');

    await user.click(screen.getByRole('button', { name: 'Participants' }));
    await user.click(screen.getByRole('button', { name: 'Ajouter un participant' }));
    await user.type(screen.getByLabelText('Nom'), 'Bob');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await screen.findByText('Bob');

    await user.click(screen.getByRole('button', { name: 'Ajouter un participant' }));
    await user.type(screen.getByLabelText('Nom'), 'Camille');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await screen.findByText('Camille');

    resolveSecondRefresh([{ participantId: 1, participantName: 'Alice', balance: 12 }]);
    await user.click(screen.getByRole('button', { name: 'Balances' }));
    await screen.findByText('Alice : 12.00 € (doit recevoir)');

    resolveFirstRefresh([{ participantId: 1, participantName: 'Alice', balance: -99 }]);
    await Promise.resolve();
    await Promise.resolve();

    expect(screen.queryByText(/-99/)).not.toBeInTheDocument();
    expect(screen.getByText('Alice : 12.00 € (doit recevoir)')).toBeInTheDocument();
  });

  it("affiche l'onglet Dépenses par défaut", async () => {
    render(<GroupView groupId={3} />);

    expect(await screen.findByRole('button', { name: 'Ajouter une dépense' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ajouter un participant' })).not.toBeInTheDocument();
  });

  it("le clic sur l'onglet Participants affiche les participants et masque les dépenses", async () => {
    const user = userEvent.setup();
    render(<GroupView groupId={3} />);
    await screen.findByRole('button', { name: 'Ajouter une dépense' });

    await user.click(screen.getByRole('button', { name: 'Participants' }));

    expect(screen.getByRole('button', { name: 'Ajouter un participant' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ajouter une dépense' })).not.toBeInTheDocument();
  });

  it("le clic sur l'onglet Balances affiche les balances et les remboursements", async () => {
    const user = userEvent.setup();
    render(<GroupView groupId={3} />);
    await screen.findByRole('button', { name: 'Ajouter une dépense' });

    await user.click(screen.getByRole('button', { name: 'Balances' }));

    expect(screen.getByText('Alice : 0.00 € (équilibré)')).toBeInTheDocument();
    expect(screen.getByText('Aucun remboursement nécessaire')).toBeInTheDocument();
  });

  it("ferme le formulaire actif lors d'un changement d'onglet", async () => {
    const user = userEvent.setup();
    render(<GroupView groupId={3} />);
    await screen.findByRole('button', { name: 'Ajouter une dépense' });

    await user.click(screen.getByRole('button', { name: 'Ajouter une dépense' }));
    expect(screen.getByLabelText('Description')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Participants' }));
    await user.click(screen.getByRole('button', { name: 'Dépenses' }));

    expect(screen.getByRole('button', { name: 'Ajouter une dépense' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Description')).not.toBeInTheDocument();
  });

  it("garde le formulaire actif et la saisie en cours lors d'un reclic sur l'onglet déjà actif", async () => {
    const user = userEvent.setup();
    render(<GroupView groupId={3} />);
    await screen.findByRole('button', { name: 'Ajouter une dépense' });

    await user.click(screen.getByRole('button', { name: 'Ajouter une dépense' }));
    await user.type(screen.getByLabelText('Description'), 'Restaurant');

    await user.click(screen.getByRole('button', { name: 'Dépenses' }));

    expect(screen.getByLabelText('Description')).toHaveValue('Restaurant');
  });

  it("ne déclenche aucun nouvel appel réseau lors d'un changement d'onglet", async () => {
    const user = userEvent.setup();
    render(<GroupView groupId={3} />);
    await screen.findByRole('button', { name: 'Ajouter une dépense' });

    await user.click(screen.getByRole('button', { name: 'Participants' }));
    await user.click(screen.getByRole('button', { name: 'Balances' }));
    await user.click(screen.getByRole('button', { name: 'Dépenses' }));

    expect(fetchGroup).toHaveBeenCalledTimes(1);
    expect(fetchParticipants).toHaveBeenCalledTimes(1);
    expect(fetchExpenses).toHaveBeenCalledTimes(1);
    expect(fetchBalances).toHaveBeenCalledTimes(1);
    expect(fetchSettlements).toHaveBeenCalledTimes(1);
  });
});

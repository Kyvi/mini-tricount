import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Group, Participant } from './types/api';
import App from './App';

vi.mock('./api/groups', async () => {
  const actual = await vi.importActual<typeof import('./api/groups')>('./api/groups');
  return {
    ...actual,
    fetchGroups: vi.fn(),
    fetchGroup: vi.fn(),
    fetchParticipants: vi.fn(),
    fetchExpenses: vi.fn(),
    fetchBalances: vi.fn(),
    fetchSettlements: vi.fn(),
  };
});

import {
  fetchBalances,
  fetchExpenses,
  fetchGroup,
  fetchGroups,
  fetchParticipants,
  fetchSettlements,
} from './api/groups';

const groups: Group[] = [{ id: 1, name: 'Vacances', createdAt: '2026-08-01T00:00:00Z' }];
const participants: Participant[] = [];

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(fetchGroups).mockResolvedValue(groups);
    vi.mocked(fetchGroup).mockResolvedValue(groups[0]);
    vi.mocked(fetchParticipants).mockResolvedValue(participants);
    vi.mocked(fetchExpenses).mockResolvedValue([]);
    vi.mocked(fetchBalances).mockResolvedValue([]);
    vi.mocked(fetchSettlements).mockResolvedValue([]);
  });

  it('affiche le GroupView du groupe sélectionné depuis la liste', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: 'Vacances' }));

    expect(await screen.findByRole('heading', { name: 'Vacances', level: 1 })).toBeInTheDocument();
  });

  it('revient à la liste des groupes via le bouton "← Retour aux groupes"', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: 'Vacances' }));
    await screen.findByRole('heading', { name: 'Vacances', level: 1 });

    await user.click(screen.getByRole('button', { name: '← Retour aux groupes' }));

    expect(await screen.findByRole('heading', { name: 'MiniTricount', level: 1 })).toBeInTheDocument();
  });
});

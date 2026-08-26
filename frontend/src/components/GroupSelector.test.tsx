import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiHttpError } from '../api/groups';
import type { Group } from '../types/api';
import { GroupSelector } from './GroupSelector';

vi.mock('../api/groups', async () => {
  const actual = await vi.importActual<typeof import('../api/groups')>('../api/groups');
  return {
    ...actual,
    fetchGroups: vi.fn(),
    createGroup: vi.fn(),
  };
});

import { createGroup, fetchGroups } from '../api/groups';

const groups: Group[] = [
  { id: 1, name: 'Vacances', createdAt: '2026-08-01T00:00:00Z' },
  { id: 2, name: 'Colocation', createdAt: '2026-08-02T00:00:00Z' },
];

describe('GroupSelector', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche la liste des groupes retournés par fetchGroups', async () => {
    vi.mocked(fetchGroups).mockResolvedValueOnce(groups);

    render(<GroupSelector onSelect={vi.fn()} />);

    expect(await screen.findByRole('button', { name: 'Vacances' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Colocation' })).toBeInTheDocument();
  });

  it('affiche un message clair et un bouton de création actionnable quand la liste est vide', async () => {
    vi.mocked(fetchGroups).mockResolvedValueOnce([]);

    render(<GroupSelector onSelect={vi.fn()} />);

    expect(await screen.findByText(/Aucun groupe pour l'instant/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Créer un groupe' })).toBeEnabled();
  });

  it('appelle onSelect avec le bon id au clic sur un groupe', async () => {
    const user = userEvent.setup();
    vi.mocked(fetchGroups).mockResolvedValueOnce(groups);
    const onSelect = vi.fn();

    render(<GroupSelector onSelect={onSelect} />);

    await user.click(await screen.findByRole('button', { name: 'Colocation' }));

    expect(onSelect).toHaveBeenCalledWith(2);
  });

  it('sélectionne automatiquement le groupe nouvellement créé', async () => {
    const user = userEvent.setup();
    vi.mocked(fetchGroups).mockResolvedValueOnce([]);
    const created: Group = { id: 3, name: 'Nouveau groupe', createdAt: '2026-08-26T00:00:00Z' };
    vi.mocked(createGroup).mockResolvedValueOnce(created);
    const onSelect = vi.fn();

    render(<GroupSelector onSelect={onSelect} />);

    await user.click(await screen.findByRole('button', { name: 'Créer un groupe' }));
    await user.type(screen.getByLabelText('Nom'), 'Nouveau groupe');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(onSelect).toHaveBeenCalledWith(3);
  });

  it("affiche une erreur de création sans appeler onSelect", async () => {
    const user = userEvent.setup();
    vi.mocked(fetchGroups).mockResolvedValueOnce([]);
    vi.mocked(createGroup).mockRejectedValueOnce(
      new ApiHttpError(400, 'Validation échouée', { name: 'ne doit pas être vide' }),
    );
    const onSelect = vi.fn();

    render(<GroupSelector onSelect={onSelect} />);

    await user.click(await screen.findByRole('button', { name: 'Créer un groupe' }));
    await user.type(screen.getByLabelText('Nom'), 'x');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(await screen.findByText('ne doit pas être vide')).toBeInTheDocument();
    expect(onSelect).not.toHaveBeenCalled();
  });
});

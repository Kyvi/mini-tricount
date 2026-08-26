import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiHttpError } from '../api/groups';
import type { Participant } from '../types/api';
import { ParticipantForm } from './ParticipantForm';

vi.mock('../api/groups', async () => {
  const actual = await vi.importActual<typeof import('../api/groups')>('../api/groups');
  return {
    ...actual,
    createParticipant: vi.fn(),
  };
});

import { createParticipant } from '../api/groups';

describe('ParticipantForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("bloque la soumission et n'appelle pas l'API si le nom est vide ou ne contient que des espaces", async () => {
    const user = userEvent.setup();
    const onCreated = vi.fn();
    render(<ParticipantForm groupId={3} onCreated={onCreated} onCancel={vi.fn()} />);

    await user.type(screen.getByLabelText('Nom'), '   ');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(screen.getByText('Le nom ne peut pas être vide')).toBeInTheDocument();
    expect(createParticipant).not.toHaveBeenCalled();
    expect(onCreated).not.toHaveBeenCalled();
  });

  it('appelle onCreated avec le participant retourné après un succès', async () => {
    const user = userEvent.setup();
    const created: Participant = { id: 10, name: 'Camille' };
    vi.mocked(createParticipant).mockResolvedValueOnce(created);
    const onCreated = vi.fn();

    render(<ParticipantForm groupId={3} onCreated={onCreated} onCancel={vi.fn()} />);

    await user.type(screen.getByLabelText('Nom'), 'Camille');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    expect(onCreated).toHaveBeenCalledWith(created);
  });

  it('affiche le message de champ sous le bon input en cas de 400 avec fieldErrors', async () => {
    const user = userEvent.setup();
    vi.mocked(createParticipant).mockRejectedValueOnce(
      new ApiHttpError(400, 'Validation échouée', { name: 'ne doit pas dépasser 255 caractères' }),
    );

    render(<ParticipantForm groupId={3} onCreated={vi.fn()} onCancel={vi.fn()} />);

    await user.type(screen.getByLabelText('Nom'), 'Camille');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));

    const nameInput = screen.getByLabelText('Nom');
    const errorMessage = await screen.findByText('ne doit pas dépasser 255 caractères');
    expect(nameInput).toHaveAccessibleDescription('ne doit pas dépasser 255 caractères');
    expect(errorMessage).toBeInTheDocument();
  });
});

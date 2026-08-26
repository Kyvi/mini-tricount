import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { ParticipantBalance } from '../types/api';
import { BalanceList } from './BalanceList';

describe('BalanceList', () => {
  it('affiche le bon qualificatif et le bon formatage selon le signe de chaque balance', () => {
    const balances: ParticipantBalance[] = [
      { participantId: 1, participantName: 'Vincent', balance: 21.7 },
      { participantId: 2, participantName: 'Camille', balance: -5.25 },
      { participantId: 3, participantName: 'Alice', balance: 0 },
    ];

    render(<BalanceList balances={balances} />);

    expect(screen.getByText('Vincent : 21.70 € (doit recevoir)')).toBeInTheDocument();
    expect(screen.getByText('Camille : -5.25 € (doit payer)')).toBeInTheDocument();
    expect(screen.getByText('Alice : 0.00 € (équilibré)')).toBeInTheDocument();
  });

  it('affiche un message si la liste est vide', () => {
    render(<BalanceList balances={[]} />);

    expect(screen.getByText('Aucune balance à afficher')).toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Settlement } from '../types/api';
import { SettlementList } from './SettlementList';

describe('SettlementList', () => {
  it('affiche chaque remboursement au format "de → vers : montant"', () => {
    const settlements: Settlement[] = [
      {
        fromParticipantId: 2,
        fromParticipantName: 'Camille',
        toParticipantId: 1,
        toParticipantName: 'Vincent',
        amount: 7,
      },
      {
        fromParticipantId: 3,
        fromParticipantName: 'Alice',
        toParticipantId: 1,
        toParticipantName: 'Vincent',
        amount: 16.5,
      },
    ];

    render(<SettlementList settlements={settlements} />);

    expect(screen.getByText('Camille → Vincent : 7.00 €')).toBeInTheDocument();
    expect(screen.getByText('Alice → Vincent : 16.50 €')).toBeInTheDocument();
  });

  it('affiche un message si aucun remboursement n\'est nécessaire', () => {
    render(<SettlementList settlements={[]} />);

    expect(screen.getByText('Aucun remboursement nécessaire')).toBeInTheDocument();
  });
});

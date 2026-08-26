import type { Settlement } from '../types/api';

interface SettlementListProps {
  settlements: Settlement[];
}

export function SettlementList({ settlements }: SettlementListProps) {
  if (settlements.length === 0) {
    return <p>Aucun remboursement nécessaire</p>;
  }

  return (
    <ul>
      {settlements.map((settlement) => (
        <li key={`${settlement.fromParticipantId}-${settlement.toParticipantId}`}>
          {settlement.fromParticipantName} → {settlement.toParticipantName} : {settlement.amount.toFixed(2)} €
        </li>
      ))}
    </ul>
  );
}

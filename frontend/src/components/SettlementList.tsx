import type { Settlement } from '../types/api';

interface SettlementListProps {
  settlements: Settlement[];
}

export function SettlementList({ settlements }: SettlementListProps) {
  if (settlements.length === 0) {
    return <p className="empty-message">Aucun remboursement nécessaire</p>;
  }

  return (
    <ul className="settlement-list" role="list">
      {settlements.map((settlement) => (
        <li key={`${settlement.fromParticipantId}-${settlement.toParticipantId}`}>
          {settlement.fromParticipantName} → {settlement.toParticipantName} : {settlement.amount.toFixed(2)} €
        </li>
      ))}
    </ul>
  );
}

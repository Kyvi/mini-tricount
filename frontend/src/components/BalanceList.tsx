import type { ParticipantBalance } from '../types/api';

interface BalanceListProps {
  balances: ParticipantBalance[];
}

function qualifierFor(balance: number): string {
  if (balance > 0) return 'doit recevoir';
  if (balance < 0) return 'doit payer';
  return 'équilibré';
}

export function BalanceList({ balances }: BalanceListProps) {
  if (balances.length === 0) {
    return <p>Aucune balance à afficher</p>;
  }

  return (
    <ul>
      {balances.map((balance) => (
        <li key={balance.participantId}>
          {balance.participantName} : {balance.balance.toFixed(2)} € ({qualifierFor(balance.balance)})
        </li>
      ))}
    </ul>
  );
}

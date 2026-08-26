import type { ParticipantBalance } from '../types/api';

interface BalanceListProps {
  balances: ParticipantBalance[];
}

function qualifierFor(balance: number): string {
  if (balance > 0) return 'doit recevoir';
  if (balance < 0) return 'doit payer';
  return 'équilibré';
}

function signClassFor(balance: number): string {
  if (balance > 0) return 'balance-positive';
  if (balance < 0) return 'balance-negative';
  return 'balance-zero';
}

export function BalanceList({ balances }: BalanceListProps) {
  if (balances.length === 0) {
    return <p className="empty-message">Aucune balance à afficher</p>;
  }

  return (
    <ul className="balance-list" role="list">
      {balances.map((balance) => (
        <li key={balance.participantId} className={signClassFor(balance.balance)}>
          {balance.participantName} : {balance.balance.toFixed(2)} € ({qualifierFor(balance.balance)})
        </li>
      ))}
    </ul>
  );
}

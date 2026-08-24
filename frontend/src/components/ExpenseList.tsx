import type { Expense } from '../types/api';

interface ExpenseListProps {
  expenses: Expense[];
}

export function ExpenseList({ expenses }: ExpenseListProps) {
  if (expenses.length === 0) {
    return <p>Aucune dépense</p>;
  }

  return (
    <ul>
      {expenses.map((expense) => (
        <li key={expense.id}>
          <p>
            {expense.description} — {expense.amount.toFixed(2)} € — {expense.expenseDate} — payé par{' '}
            {expense.paidBy.name}
          </p>
          <ul>
            {expense.shares.map((share) => (
              <li key={share.participantId}>
                {share.participantName} : {share.shareAmount.toFixed(2)} €
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ul>
  );
}

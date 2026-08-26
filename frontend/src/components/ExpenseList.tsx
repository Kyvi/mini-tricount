import { useState } from 'react';
import { ApiHttpError, deleteExpense } from '../api/groups';
import type { Expense, Participant } from '../types/api';
import { ExpenseForm } from './ExpenseForm';

interface ExpenseListProps {
  groupId: number;
  participants: Participant[];
  expenses: Expense[];
  editingExpenseId: number | null;
  onEditRequested: (expenseId: number) => void;
  onEditSaved: (expense: Expense) => void;
  onEditCancelled: () => void;
  onDeleted: (expenseId: number) => void;
}

export function ExpenseList({
  groupId,
  participants,
  expenses,
  editingExpenseId,
  onEditRequested,
  onEditSaved,
  onEditCancelled,
  onDeleted,
}: ExpenseListProps) {
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteError, setDeleteError] = useState<{ expenseId: number; message: string } | null>(null);

  async function handleDelete(expense: Expense) {
    if (!window.confirm(`Supprimer la dépense "${expense.description}" ?`)) return;

    setDeleteError(null);
    setDeletingId(expense.id);
    try {
      await deleteExpense(groupId, expense.id);
      onDeleted(expense.id);
    } catch (error: unknown) {
      const message =
        error instanceof ApiHttpError ? error.message : 'Une erreur est survenue, réessayez.';
      setDeleteError({ expenseId: expense.id, message });
    } finally {
      setDeletingId(null);
    }
  }

  if (expenses.length === 0) {
    return <p>Aucune dépense</p>;
  }

  return (
    <ul>
      {expenses.map((expense) => (
        <li key={expense.id}>
          {editingExpenseId === expense.id ? (
            <ExpenseForm
              groupId={groupId}
              participants={participants}
              expense={expense}
              onSaved={onEditSaved}
              onCancel={onEditCancelled}
            />
          ) : (
            <>
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
              <div className="actions">
                <button
                  type="button"
                  onClick={() => onEditRequested(expense.id)}
                  disabled={deletingId === expense.id}
                >
                  Modifier
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(expense)}
                  disabled={deletingId === expense.id}
                >
                  {deletingId === expense.id ? 'Suppression…' : 'Supprimer'}
                </button>
              </div>
              {deleteError?.expenseId === expense.id && (
                <p className="field-error">{deleteError.message}</p>
              )}
            </>
          )}
        </li>
      ))}
    </ul>
  );
}

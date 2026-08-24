import { useEffect, useState } from 'react';
import { ApiHttpError, fetchExpenses, fetchGroup, fetchParticipants } from '../api/groups';
import type { Expense, Group, Participant } from '../types/api';
import { ExpenseForm } from './ExpenseForm';
import { ExpenseList } from './ExpenseList';

interface GroupViewProps {
  groupId: number;
}

type ViewState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | { status: 'ready'; group: Group; participants: Participant[]; expenses: Expense[] };

export function GroupView({ groupId }: GroupViewProps) {
  const [state, setState] = useState<ViewState>({ status: 'loading' });
  const [isFormOpen, setIsFormOpen] = useState(false);

  useEffect(() => {
    let ignore = false;

    Promise.all([fetchGroup(groupId), fetchParticipants(groupId), fetchExpenses(groupId)])
      .then(([group, participants, expenses]) => {
        if (ignore) return;
        setState({ status: 'ready', group, participants, expenses });
      })
      .catch((error: unknown) => {
        if (ignore) return;
        if (error instanceof ApiHttpError && error.status === 404) {
          setState({ status: 'not-found' });
        } else {
          setState({ status: 'error' });
        }
      });

    return () => {
      ignore = true;
    };
  }, [groupId]);

  if (state.status === 'loading') {
    return <p>Chargement…</p>;
  }

  if (state.status === 'not-found') {
    return <p>Groupe introuvable</p>;
  }

  if (state.status === 'error') {
    return <p>Une erreur est survenue</p>;
  }

  const { group, participants, expenses } = state;

  return (
    <section>
      <h1>{group.name}</h1>
      <h2>Participants</h2>
      {participants.length === 0 ? (
        <p>Aucun participant</p>
      ) : (
        <ul>
          {participants.map((participant) => (
            <li key={participant.id}>{participant.name}</li>
          ))}
        </ul>
      )}
      <h2>Dépenses</h2>
      {isFormOpen ? (
        <ExpenseForm
          groupId={groupId}
          participants={participants}
          onCreated={(expense) => {
            setState((prev) =>
              prev.status === 'ready' ? { ...prev, expenses: [...prev.expenses, expense] } : prev,
            );
            setIsFormOpen(false);
          }}
          onCancel={() => setIsFormOpen(false)}
        />
      ) : (
        <button type="button" onClick={() => setIsFormOpen(true)}>
          Ajouter une dépense
        </button>
      )}
      <ExpenseList expenses={expenses} />
    </section>
  );
}

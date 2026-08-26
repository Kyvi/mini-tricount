import { useEffect, useRef, useState } from 'react';
import {
  ApiHttpError,
  fetchBalances,
  fetchExpenses,
  fetchGroup,
  fetchParticipants,
  fetchSettlements,
} from '../api/groups';
import type { Expense, Group, Participant, ParticipantBalance, Settlement } from '../types/api';
import { BalanceList } from './BalanceList';
import { ExpenseForm } from './ExpenseForm';
import { ExpenseList } from './ExpenseList';
import { ParticipantForm } from './ParticipantForm';
import { SettlementList } from './SettlementList';

interface GroupViewProps {
  groupId: number;
  onBack?: () => void;
}

type ViewState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | {
      status: 'ready';
      group: Group;
      participants: Participant[];
      expenses: Expense[];
      balances: ParticipantBalance[];
      settlements: Settlement[];
      financialRefreshError: string | null;
    };

type OpenForm =
  | { type: 'none' }
  | { type: 'create-expense' }
  | { type: 'create-participant' }
  | { type: 'edit-expense'; expenseId: number };

type GroupTab = 'expenses' | 'participants' | 'balances';

export function GroupView({ groupId, onBack }: GroupViewProps) {
  const [state, setState] = useState<ViewState>({ status: 'loading' });
  const [openForm, setOpenForm] = useState<OpenForm>({ type: 'none' });
  const [activeTab, setActiveTab] = useState<GroupTab>('expenses');
  const financialsRequestId = useRef(0);

  function handleTabChange(tab: GroupTab) {
    if (tab === activeTab) return;
    setActiveTab(tab);
    setOpenForm({ type: 'none' });
  }

  useEffect(() => {
    let ignore = false;

    Promise.all([
      fetchGroup(groupId),
      fetchParticipants(groupId),
      fetchExpenses(groupId),
      fetchBalances(groupId),
      fetchSettlements(groupId),
    ])
      .then(([group, participants, expenses, balances, settlements]) => {
        if (ignore) return;
        setState({
          status: 'ready',
          group,
          participants,
          expenses,
          balances,
          settlements,
          financialRefreshError: null,
        });
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

  async function refreshFinancials() {
    const requestId = ++financialsRequestId.current;
    try {
      const [balances, settlements] = await Promise.all([
        fetchBalances(groupId),
        fetchSettlements(groupId),
      ]);
      if (requestId !== financialsRequestId.current) return;
      setState((prev) =>
        prev.status === 'ready' ? { ...prev, balances, settlements, financialRefreshError: null } : prev,
      );
    } catch {
      if (requestId !== financialsRequestId.current) return;
      setState((prev) =>
        prev.status === 'ready'
          ? { ...prev, financialRefreshError: "Impossible d'actualiser les balances et remboursements." }
          : prev,
      );
    }
  }

  if (state.status === 'loading') {
    return <p className="status-message">Chargement…</p>;
  }

  if (state.status === 'not-found') {
    return <p className="status-message">Groupe introuvable</p>;
  }

  if (state.status === 'error') {
    return <p className="status-message">Une erreur est survenue</p>;
  }

  const { group, participants, expenses, balances, settlements, financialRefreshError } = state;
  const editingExpenseId = openForm.type === 'edit-expense' ? openForm.expenseId : null;

  return (
    <section className="page">
      {onBack && (
        <button type="button" className="btn btn-secondary" onClick={onBack}>
          ← Retour aux groupes
        </button>
      )}
      <h1>{group.name}</h1>

      <nav className="group-tabs" aria-label="Sections du groupe">
        <button
          type="button"
          className="group-tab"
          aria-current={activeTab === 'expenses' ? 'page' : undefined}
          onClick={() => handleTabChange('expenses')}
        >
          Dépenses
        </button>
        <button
          type="button"
          className="group-tab"
          aria-current={activeTab === 'participants' ? 'page' : undefined}
          onClick={() => handleTabChange('participants')}
        >
          Participants
        </button>
        <button
          type="button"
          className="group-tab"
          aria-current={activeTab === 'balances' ? 'page' : undefined}
          onClick={() => handleTabChange('balances')}
        >
          Balances
        </button>
      </nav>

      {activeTab === 'participants' && (
      <section className="panel">
        <h2>Participants</h2>
        {participants.length === 0 ? (
          <p className="empty-message">Aucun participant</p>
        ) : (
          <ul className="chip-list" role="list">
            {participants.map((participant) => (
              <li key={participant.id} className="chip">
                {participant.name}
              </li>
            ))}
          </ul>
        )}
        {openForm.type === 'create-participant' ? (
          <ParticipantForm
            groupId={groupId}
            onCreated={(participant) => {
              setState((prev) =>
                prev.status === 'ready'
                  ? { ...prev, participants: [...prev.participants, participant] }
                  : prev,
              );
              setOpenForm({ type: 'none' });
              void refreshFinancials();
            }}
            onCancel={() => setOpenForm({ type: 'none' })}
          />
        ) : (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setOpenForm({ type: 'create-participant' })}
          >
            Ajouter un participant
          </button>
        )}
      </section>
      )}

      {activeTab === 'expenses' && (
      <section className="panel">
        <h2>Dépenses</h2>
        {openForm.type === 'create-expense' ? (
          <ExpenseForm
            groupId={groupId}
            participants={participants}
            onSaved={(expense) => {
              setState((prev) =>
                prev.status === 'ready' ? { ...prev, expenses: [...prev.expenses, expense] } : prev,
              );
              setOpenForm({ type: 'none' });
              void refreshFinancials();
            }}
            onCancel={() => setOpenForm({ type: 'none' })}
          />
        ) : (
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => setOpenForm({ type: 'create-expense' })}
          >
            Ajouter une dépense
          </button>
        )}
        <ExpenseList
          groupId={groupId}
          participants={participants}
          expenses={expenses}
          editingExpenseId={editingExpenseId}
          onEditRequested={(expenseId) => setOpenForm({ type: 'edit-expense', expenseId })}
          onEditSaved={(expense) => {
            setState((prev) =>
              prev.status === 'ready'
                ? { ...prev, expenses: prev.expenses.map((e) => (e.id === expense.id ? expense : e)) }
                : prev,
            );
            setOpenForm({ type: 'none' });
            void refreshFinancials();
          }}
          onEditCancelled={() => setOpenForm({ type: 'none' })}
          onDeleted={(expenseId) => {
            setState((prev) =>
              prev.status === 'ready'
                ? { ...prev, expenses: prev.expenses.filter((e) => e.id !== expenseId) }
                : prev,
            );
            void refreshFinancials();
          }}
        />
      </section>
      )}

      {activeTab === 'balances' && (
        <>
          <section className="panel">
            <h2>Balances</h2>
            {financialRefreshError && <p className="form-error">{financialRefreshError}</p>}
            <BalanceList balances={balances} />
          </section>

          <section className="panel">
            <h2>Remboursements</h2>
            <SettlementList settlements={settlements} />
          </section>
        </>
      )}
    </section>
  );
}

import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiHttpError, createExpense } from '../api/groups';
import type { Expense, Participant } from '../types/api';

interface ExpenseFormProps {
  groupId: number;
  participants: Participant[];
  onCreated: (expense: Expense) => void;
  onCancel: () => void;
}

interface FormValues {
  description: string;
  amount: string;
  expenseDate: string;
  paidByParticipantId: string;
  beneficiaryParticipantIds: number[];
}

type SubmitState =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'error'; message: string; fieldErrors: Record<string, string> };

function todayIsoDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function ExpenseForm({ groupId, participants, onCreated, onCancel }: ExpenseFormProps) {
  const [values, setValues] = useState<FormValues>({
    description: '',
    amount: '',
    expenseDate: todayIsoDate(),
    paidByParticipantId: '',
    beneficiaryParticipantIds: participants.map((participant) => participant.id),
  });
  const [submitState, setSubmitState] = useState<SubmitState>({ status: 'idle' });
  const [beneficiariesError, setBeneficiariesError] = useState<string | null>(null);

  const isSubmitting = submitState.status === 'submitting';

  function clearFieldError(field: string) {
    setSubmitState((prev) => {
      if (prev.status !== 'error' || !(field in prev.fieldErrors)) return prev;
      const fieldErrors = Object.fromEntries(
        Object.entries(prev.fieldErrors).filter(([key]) => key !== field),
      );
      return { ...prev, fieldErrors };
    });
  }

  function toggleBeneficiary(participantId: number) {
    setValues((prev) => ({
      ...prev,
      beneficiaryParticipantIds: prev.beneficiaryParticipantIds.includes(participantId)
        ? prev.beneficiaryParticipantIds.filter((id) => id !== participantId)
        : [...prev.beneficiaryParticipantIds, participantId],
    }));
    setBeneficiariesError(null);
    clearFieldError('beneficiaryParticipantIds');
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) return;

    if (values.beneficiaryParticipantIds.length === 0) {
      setBeneficiariesError('Sélectionnez au moins un bénéficiaire');
      return;
    }
    setBeneficiariesError(null);

    setSubmitState({ status: 'submitting' });
    try {
      const expense = await createExpense(groupId, {
        description: values.description,
        amount: Number(values.amount),
        expenseDate: values.expenseDate,
        paidByParticipantId: Number(values.paidByParticipantId),
        beneficiaryParticipantIds: values.beneficiaryParticipantIds,
      });
      onCreated(expense);
    } catch (error: unknown) {
      if (error instanceof ApiHttpError) {
        setSubmitState({ status: 'error', message: error.message, fieldErrors: error.fieldErrors });
      } else {
        setSubmitState({ status: 'error', message: 'Une erreur est survenue, réessayez.', fieldErrors: {} });
      }
    }
  }

  const fieldErrors = submitState.status === 'error' ? submitState.fieldErrors : {};

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      {submitState.status === 'error' && <p className="form-error">{submitState.message}</p>}

      <div className="field">
        <label htmlFor="expense-description">Description</label>
        <input
          id="expense-description"
          type="text"
          required
          maxLength={255}
          value={values.description}
          onChange={(event) => {
            setValues((prev) => ({ ...prev, description: event.target.value }));
            clearFieldError('description');
          }}
          aria-describedby={fieldErrors.description ? 'expense-description-error' : undefined}
        />
        {fieldErrors.description && (
          <p id="expense-description-error" className="field-error">
            {fieldErrors.description}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="expense-amount">Montant</label>
        <input
          id="expense-amount"
          type="number"
          step="0.01"
          min="0.01"
          required
          value={values.amount}
          onChange={(event) => {
            setValues((prev) => ({ ...prev, amount: event.target.value }));
            clearFieldError('amount');
          }}
          aria-describedby={fieldErrors.amount ? 'expense-amount-error' : undefined}
        />
        {fieldErrors.amount && (
          <p id="expense-amount-error" className="field-error">
            {fieldErrors.amount}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="expense-date">Date</label>
        <input
          id="expense-date"
          type="date"
          required
          value={values.expenseDate}
          onChange={(event) => {
            setValues((prev) => ({ ...prev, expenseDate: event.target.value }));
            clearFieldError('expenseDate');
          }}
          aria-describedby={fieldErrors.expenseDate ? 'expense-date-error' : undefined}
        />
        {fieldErrors.expenseDate && (
          <p id="expense-date-error" className="field-error">
            {fieldErrors.expenseDate}
          </p>
        )}
      </div>

      <div className="field">
        <label htmlFor="expense-paid-by">Payeur</label>
        <select
          id="expense-paid-by"
          required
          value={values.paidByParticipantId}
          onChange={(event) => {
            setValues((prev) => ({ ...prev, paidByParticipantId: event.target.value }));
            clearFieldError('paidByParticipantId');
          }}
          aria-describedby={fieldErrors.paidByParticipantId ? 'expense-paid-by-error' : undefined}
        >
          <option value="">-- Choisir --</option>
          {participants.map((participant) => (
            <option key={participant.id} value={participant.id}>
              {participant.name}
            </option>
          ))}
        </select>
        {fieldErrors.paidByParticipantId && (
          <p id="expense-paid-by-error" className="field-error">
            {fieldErrors.paidByParticipantId}
          </p>
        )}
      </div>

      <fieldset className="field">
        <legend>Bénéficiaires</legend>
        {participants.map((participant) => (
          <label key={participant.id}>
            <input
              type="checkbox"
              checked={values.beneficiaryParticipantIds.includes(participant.id)}
              onChange={() => toggleBeneficiary(participant.id)}
            />
            {participant.name}
          </label>
        ))}
        {beneficiariesError && <p className="field-error">{beneficiariesError}</p>}
        {fieldErrors.beneficiaryParticipantIds && (
          <p className="field-error">{fieldErrors.beneficiaryParticipantIds}</p>
        )}
      </fieldset>

      <div className="actions">
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Enregistrement…' : 'Enregistrer'}
        </button>
        <button type="button" onClick={onCancel} disabled={isSubmitting}>
          Annuler
        </button>
      </div>
    </form>
  );
}

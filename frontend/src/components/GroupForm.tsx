import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiHttpError, createGroup } from '../api/groups';
import type { Group } from '../types/api';

interface GroupFormProps {
  onCreated: (group: Group) => void;
  onCancel: () => void;
}

type SubmitState =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'error'; message: string; fieldErrors: Record<string, string> };

export function GroupForm({ onCreated, onCancel }: GroupFormProps) {
  const [name, setName] = useState('');
  const [submitState, setSubmitState] = useState<SubmitState>({ status: 'idle' });
  const [nameError, setNameError] = useState<string | null>(null);

  const isSubmitting = submitState.status === 'submitting';
  const fieldErrors = submitState.status === 'error' ? submitState.fieldErrors : {};

  function handleNameChange(value: string) {
    setName(value);
    setNameError(null);
    if (submitState.status === 'error') {
      setSubmitState({ status: 'idle' });
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) return;

    if (name.trim().length === 0) {
      setNameError('Le nom ne peut pas être vide');
      return;
    }
    setNameError(null);

    setSubmitState({ status: 'submitting' });
    try {
      const group = await createGroup({ name: name.trim() });
      onCreated(group);
    } catch (error: unknown) {
      if (error instanceof ApiHttpError) {
        setSubmitState({ status: 'error', message: error.message, fieldErrors: error.fieldErrors });
      } else {
        setSubmitState({ status: 'error', message: 'Une erreur est survenue, réessayez.', fieldErrors: {} });
      }
    }
  }

  return (
    <form className="form-card" onSubmit={handleSubmit}>
      {submitState.status === 'error' && <p className="form-error">{submitState.message}</p>}

      <div className="field">
        <label htmlFor="group-name">Nom</label>
        <input
          id="group-name"
          type="text"
          required
          maxLength={255}
          value={name}
          onChange={(event) => handleNameChange(event.target.value)}
          aria-describedby={nameError || fieldErrors.name ? 'group-name-error' : undefined}
        />
        {(nameError || fieldErrors.name) && (
          <p id="group-name-error" className="field-error">
            {nameError ?? fieldErrors.name}
          </p>
        )}
      </div>

      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
          {isSubmitting ? 'Enregistrement…' : 'Enregistrer'}
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={isSubmitting}>
          Annuler
        </button>
      </div>
    </form>
  );
}

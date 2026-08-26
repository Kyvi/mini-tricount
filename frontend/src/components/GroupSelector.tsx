import { useEffect, useState } from 'react';
import { fetchGroups } from '../api/groups';
import type { Group } from '../types/api';
import { GroupForm } from './GroupForm';

interface GroupSelectorProps {
  onSelect: (groupId: number) => void;
}

type SelectorState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; groups: Group[] };

export function GroupSelector({ onSelect }: GroupSelectorProps) {
  const [state, setState] = useState<SelectorState>({ status: 'loading' });
  const [isCreateFormOpen, setIsCreateFormOpen] = useState(false);

  useEffect(() => {
    let ignore = false;

    fetchGroups()
      .then((groups) => {
        if (ignore) return;
        setState({ status: 'ready', groups });
      })
      .catch(() => {
        if (ignore) return;
        setState({ status: 'error' });
      });

    return () => {
      ignore = true;
    };
  }, []);

  if (state.status === 'loading') {
    return <p className="status-message">Chargement…</p>;
  }

  if (state.status === 'error') {
    return <p className="status-message">Une erreur est survenue</p>;
  }

  const { groups } = state;

  return (
    <section className="page">
      <h1>MiniTricount</h1>
      <section className="panel">
        <h2>Groupes</h2>
        {groups.length === 0 ? (
          <p className="empty-message">Aucun groupe pour l'instant. Créez le premier groupe pour commencer.</p>
        ) : (
          <ul className="group-list" role="list">
            {groups.map((group) => (
              <li key={group.id}>
                <button type="button" className="btn btn-secondary" onClick={() => onSelect(group.id)}>
                  {group.name}
                </button>
              </li>
            ))}
          </ul>
        )}
        {isCreateFormOpen ? (
          <GroupForm onCreated={(group) => onSelect(group.id)} onCancel={() => setIsCreateFormOpen(false)} />
        ) : (
          <button type="button" className="btn btn-primary" onClick={() => setIsCreateFormOpen(true)}>
            Créer un groupe
          </button>
        )}
      </section>
    </section>
  );
}

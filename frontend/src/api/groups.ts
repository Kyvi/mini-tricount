import type { ApiError, Expense, Group, Participant } from '../types/api';

export class ApiHttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`/api${path}`);
  if (!response.ok) {
    const body: ApiError = await response.json();
    throw new ApiHttpError(response.status, body.message);
  }
  return response.json();
}

export const fetchGroup = (groupId: number) => apiGet<Group>(`/groups/${groupId}`);

export const fetchParticipants = (groupId: number) =>
  apiGet<Participant[]>(`/groups/${groupId}/participants`);

export const fetchExpenses = (groupId: number) =>
  apiGet<Expense[]>(`/groups/${groupId}/expenses`);

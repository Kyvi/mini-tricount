import type {
  ApiError,
  CreateExpenseRequest,
  CreateParticipantRequest,
  Expense,
  Group,
  Participant,
  ParticipantBalance,
  Settlement,
} from '../types/api';

export class ApiHttpError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, init);
  if (!response.ok) {
    let message = 'Une erreur est survenue';
    let fieldErrors: Record<string, string> = {};
    try {
      const body: ApiError = await response.json();
      message = body.message;
      fieldErrors = body.fieldErrors ?? {};
    } catch {
      // corps de réponse absent ou non-JSON (ex. erreur renvoyée par le proxy Vite lui-même)
    }
    throw new ApiHttpError(response.status, message, fieldErrors);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

async function apiGet<T>(path: string): Promise<T> {
  return request<T>(path);
}

async function apiPost<T>(path: string, payload: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

async function apiPut<T>(path: string, payload: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export const fetchGroup = (groupId: number) => apiGet<Group>(`/groups/${groupId}`);

export const fetchParticipants = (groupId: number) =>
  apiGet<Participant[]>(`/groups/${groupId}/participants`);

export const fetchExpenses = (groupId: number) =>
  apiGet<Expense[]>(`/groups/${groupId}/expenses`);

export const createExpense = (groupId: number, payload: CreateExpenseRequest) =>
  apiPost<Expense>(`/groups/${groupId}/expenses`, payload);

export const createParticipant = (groupId: number, payload: CreateParticipantRequest) =>
  apiPost<Participant>(`/groups/${groupId}/participants`, payload);

export const updateExpense = (groupId: number, expenseId: number, payload: CreateExpenseRequest) =>
  apiPut<Expense>(`/groups/${groupId}/expenses/${expenseId}`, payload);

export const deleteExpense = (groupId: number, expenseId: number) =>
  request<void>(`/groups/${groupId}/expenses/${expenseId}`, { method: 'DELETE' });

export const fetchBalances = (groupId: number) =>
  apiGet<ParticipantBalance[]>(`/groups/${groupId}/balances`);

export const fetchSettlements = (groupId: number) =>
  apiGet<Settlement[]>(`/groups/${groupId}/settlements`);

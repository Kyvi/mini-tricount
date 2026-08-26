export interface Group {
  id: number;
  name: string;
  createdAt: string;
}

export interface Participant {
  id: number;
  name: string;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  fieldErrors: Record<string, string>;
}

export interface ExpenseShare {
  participantId: number;
  participantName: string;
  shareAmount: number;
}

export interface Expense {
  id: number;
  description: string;
  amount: number;
  expenseDate: string;
  createdAt: string;
  paidBy: Participant;
  shares: ExpenseShare[];
}

export interface CreateExpenseRequest {
  description: string;
  amount: number;
  expenseDate: string;
  paidByParticipantId: number;
  beneficiaryParticipantIds: number[];
}

export interface CreateParticipantRequest {
  name: string;
}

export interface CreateGroupRequest {
  name: string;
}

export interface ParticipantBalance {
  participantId: number;
  participantName: string;
  balance: number;
}

export interface Settlement {
  fromParticipantId: number;
  fromParticipantName: string;
  toParticipantId: number;
  toParticipantName: string;
  amount: number;
}

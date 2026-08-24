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

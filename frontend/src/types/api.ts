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

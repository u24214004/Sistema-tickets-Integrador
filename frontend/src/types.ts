export type Severity = "CRITICA" | "ALTA" | "MEDIA" | "BAJA";
export type TicketStatus = "ABIERTO" | "EN_PROCESO" | "EN_ESPERA" | "RESUELTO";
export interface User {
  id: number;
  name: string;
  email: string;
  role: "INTERNO" | "CLIENTE";
  companyId: number | null;
  companyName?: string | null;
}
export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}
export interface Ticket {
  id: number;
  code: string;
  title: string;
  description: string;
  companyId: number;
  companyName: string;
  requesterName: string;
  severity: Severity;
  status: TicketStatus;
  createdAt: string;
}
export interface TicketPage {
  content: Ticket[];
  totalElements: number;
  totalPages: number;
  number: number;
}
export interface Summary {
  total: number;
  open: number;
  inProgress: number;
  resolved: number;
}
export interface Company {
  id: number;
  name: string;
}
export interface CreateTicket {
  title: string;
  description: string;
  severity: Severity;
  companyId?: number;
}
export interface UpdateTicket {
  title: string;
  description: string;
  severity: Severity;
  status: TicketStatus;
  companyId?: number;
}
export const severityLabels: Record<Severity, string> = {
  CRITICA: "Crítica",
  ALTA: "Alta",
  MEDIA: "Media",
  BAJA: "Baja",
};
export const statusLabels: Record<TicketStatus, string> = {
  ABIERTO: "Abierto",
  EN_PROCESO: "En proceso",
  EN_ESPERA: "En espera",
  RESUELTO: "Resuelto",
};
export const formatDate = (value: string) =>
  new Intl.DateTimeFormat("es-PE", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "America/Lima",
  }).format(new Date(value));

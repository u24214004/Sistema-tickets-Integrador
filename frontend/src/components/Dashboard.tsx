import { useCallback, useEffect, useState } from "react";
import { ApiError, request } from "../api";
import {
  severityLabels,
  statusLabels,
  type Session,
  type Summary,
  type TicketPage,
  type Ticket,
  type TicketStatus,
  type Severity,
} from "../types";
import TicketForm from "./TicketForm";
import ReportDownloadButton from "./ReportDownloadButton";
import { Sidebar, Topbar } from "./WorkspaceChrome";
import TicketStats from "./TicketStats";
import TicketDetail from "./TicketDetail";
import TicketTable from "./TicketTable";
export default function Dashboard({
  session,
  onLogout,
  onExpired,
}: {
  session: Session;
  onLogout: () => void;
  onExpired: () => void;
}) {
  const [data, setData] = useState<TicketPage | null>(null);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [severity, setSeverity] = useState("");
  const [pending, setPending] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<Ticket | null>(null);
  const [detail, setDetail] = useState<Ticket | null>(null);
  const [refresh, setRefresh] = useState(0);
  useEffect(() => {
    let active = true;
    setPending(true);
    setError("");
    const query = new URLSearchParams({ page: String(page), size: "10" });
    if (status) query.set("status", status);
    if (severity) query.set("severity", severity);
    Promise.all([
      request<TicketPage>(`/tickets?${query}`, session.token),
      request<Summary>("/tickets/summary", session.token),
    ])
      .then(([tickets, stats]) => {
        if (active) {
          setData(tickets);
          setSummary(stats);
        }
      })
      .catch((failure) => {
        if (active) {
          setData(null);
          setSummary(null);
          if (failure.status === 401) onExpired();
          else setError(failure.message);
        }
      })
      .finally(() => {
        if (active) setPending(false);
      });
    return () => {
      active = false;
    };
  }, [session.token, page, status, severity, refresh, onExpired]);
  const openDetail = async (ticket: Ticket) => {
    setError("");
    try {
      setDetail(await request<Ticket>(`/tickets/${ticket.id}`, session.token));
    } catch (failure) {
      const problem = failure as ApiError;
      if (problem.status === 401) onExpired();
      else setError(problem.message);
    }
  };
  const saved = useCallback((ticket: Ticket, isEdit: boolean) => {
    setCreating(false);
    setEditing(null);
    setNotice(
      `Ticket ${ticket.code} ${isEdit ? "actualizado" : "creado"} correctamente.`,
    );
    setPage(0);
    setStatus("");
    setSeverity("");
    setRefresh((value) => value + 1);
    if (isEdit) setDetail(ticket);
  }, []);
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="workspace">
        <Topbar user={session.user} onLogout={onLogout} />
        <main className="dashboard">
          <div className="page-heading">
            <div>
              <span className="eyebrow">OPERACIÓN Y SOPORTE</span>
              <h1>
                Tickets de soporte<span className="heading-dot">.</span>
              </h1>
              <p className="muted">
                {session.user.role === "INTERNO"
                  ? "Todas las solicitudes, un solo lugar para darles seguimiento."
                  : `Solicitudes de ${session.user.companyName ?? "tu empresa"}.`}
              </p>
            </div>
            <button
              className="primary"
              onClick={() => {
                setNotice("");
                setCreating(true);
              }}
            >
              Nuevo ticket
            </button>
          </div>
          <TicketStats summary={summary} pending={pending} />
          {notice ? (
            <div className="success" role="status">
              {notice}
              <button aria-label="Cerrar aviso" onClick={() => setNotice("")}>
                ×
              </button>
            </div>
          ) : null}
          {error ? (
            <div className="error" role="alert">
              {error}
            </div>
          ) : null}
          <section className="tickets-panel">
            <div className="panel-title">
              <div>
                <h2>Bandeja de solicitudes</h2>
                <span className="muted">
                  Consulta el detalle de cada incidencia.
                </span>
              </div>
              <div className="panel-actions">
                <ReportDownloadButton
                  token={session.token}
                  status={status}
                  severity={severity}
                  onError={setError}
                  onExpired={onExpired}
                />
                <button
                  className="secondary"
                  onClick={() => setRefresh((value) => value + 1)}
                  disabled={pending}
                >
                  Actualizar
                </button>
              </div>
            </div>
            <div className="filters">
              <div>
                <label htmlFor="status-filter">Estado</label>
                <select
                  id="status-filter"
                  value={status}
                  onChange={(e) => {
                    setStatus(e.target.value as TicketStatus);
                    setPage(0);
                  }}
                >
                  <option value="">Todos los estados</option>
                  {Object.entries(statusLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="severity-filter">Severidad</label>
                <select
                  id="severity-filter"
                  value={severity}
                  onChange={(e) => {
                    setSeverity(e.target.value as Severity);
                    setPage(0);
                  }}
                >
                  <option value="">Todas las severidades</option>
                  {Object.entries(severityLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <span className="scope-label">
                {session.user.role === "INTERNO"
                  ? "Vista de todas las empresas"
                  : "Vista de tu empresa"}
              </span>
            </div>
            <TicketTable
              data={data}
              pending={pending}
              onOpen={openDetail}
              onPage={setPage}
            />
          </section>
          <footer className="dashboard-footer">
            TechConsulting Perú S.A.C.{" "}
            <span>Atención centralizada · Trazabilidad de solicitudes</span>
          </footer>
        </main>
      </div>
      {creating || editing ? (
        <TicketForm
          session={session}
          ticket={editing ?? undefined}
          onClose={() => {
            setCreating(false);
            setEditing(null);
          }}
          onSaved={saved}
          onExpired={onExpired}
        />
      ) : null}
      {detail && !editing ? (
        <TicketDetail
          ticket={detail}
          onClose={() => setDetail(null)}
          onEdit={(ticket) => {
            setDetail(null);
            setEditing(ticket);
          }}
        />
      ) : null}
    </div>
  );
}

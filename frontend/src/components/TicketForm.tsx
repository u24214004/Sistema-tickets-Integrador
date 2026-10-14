import { useEffect, useState, type FormEvent } from "react";
import { ApiError, request } from "../api";
import {
  severityLabels,
  type Company,
  type Session,
  type Ticket,
  type Severity,
  type CreateTicket,
} from "../types";
import Modal from "./Modal";
export default function TicketForm({
  session,
  onClose,
  onCreated,
  onExpired,
}: {
  session: Session;
  onClose: () => void;
  onCreated: (ticket: Ticket) => void;
  onExpired: () => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [severity, setSeverity] = useState<Severity>("MEDIA");
  const [companyId, setCompanyId] = useState("");
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loadingCompanies, setLoadingCompanies] = useState(
    session.user.role === "INTERNO",
  );
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  useEffect(() => {
    if (session.user.role !== "INTERNO") return;
    let active = true;
    request<Company[]>("/companies", session.token)
      .then((data) => {
        if (active) setCompanies(data);
      })
      .catch((failure) => {
        if (active) {
          if (failure.status === 401) onExpired();
          else setError(failure.message);
        }
      })
      .finally(() => {
        if (active) setLoadingCompanies(false);
      });
    return () => {
      active = false;
    };
  }, [session, onExpired]);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setFields({});
    if (!title.trim() || !description.trim()) {
      setError("Completa el asunto y la descripción.");
      return;
    }
    setPending(true);
    const data: CreateTicket = {
      title: title.trim(),
      description: description.trim(),
      severity,
    };
    if (session.user.role === "INTERNO") data.companyId = Number(companyId);
    try {
      onCreated(
        await request<Ticket>("/tickets", session.token, {
          method: "POST",
          body: JSON.stringify(data),
        }),
      );
    } catch (failure) {
      const problem = failure as ApiError;
      if (problem.status === 401) onExpired();
      else {
        setError(problem.message);
        setFields(problem.fields);
      }
    } finally {
      setPending(false);
    }
  };
  return (
    <Modal
      title="Nueva solicitud de soporte"
      onClose={() => {
        if (!pending) onClose();
      }}
    >
      <form className="ticket-form" onSubmit={submit}>
        <p className="muted">
          Describe la incidencia para que el equipo pueda atenderla.
        </p>
        {session.user.role === "INTERNO" ? (
          <>
            <label htmlFor="company">Empresa cliente</label>
            <select
              id="company"
              required
              disabled={loadingCompanies || pending}
              value={companyId}
              onChange={(e) => setCompanyId(e.target.value)}
            >
              <option value="">
                {loadingCompanies
                  ? "Cargando empresas…"
                  : "Selecciona una empresa"}
              </option>
              {companies.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </>
        ) : (
          <div className="company-context">
            Empresa · {session.user.companyName}
          </div>
        )}
        <label htmlFor="title">Asunto</label>
        <input
          id="title"
          required
          maxLength={150}
          value={title}
          disabled={pending}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ej. Error al contabilizar una factura"
          aria-invalid={!!fields.title}
        />
        {fields.title ? (
          <small className="field-error">{fields.title}</small>
        ) : null}
        <label htmlFor="description">Descripción</label>
        <textarea
          id="description"
          required
          maxLength={4000}
          rows={5}
          value={description}
          disabled={pending}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="¿Qué ocurrió? Incluye el módulo y los pasos para reproducirlo."
          aria-invalid={!!fields.description}
        />
        <small className="field-hint">
          {description.length} / 4000 caracteres
        </small>
        <label htmlFor="severity">Severidad</label>
        <select
          id="severity"
          value={severity}
          disabled={pending}
          onChange={(e) => setSeverity(e.target.value as Severity)}
        >
          {Object.entries(severityLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        {error ? (
          <div className="error" role="alert">
            {error}
          </div>
        ) : null}
        <div className="modal-actions">
          <button
            type="button"
            className="secondary"
            disabled={pending}
            onClick={onClose}
          >
            Cancelar
          </button>
          <button className="primary" disabled={pending || loadingCompanies}>
            {pending ? "Creando…" : "Crear ticket"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

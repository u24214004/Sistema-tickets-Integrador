import {
  formatDate,
  severityLabels,
  statusLabels,
  type Ticket,
} from "../types";
import Modal from "./Modal";
export default function TicketDetail({
  ticket,
  onClose,
  onEdit,
}: {
  ticket: Ticket;
  onClose: () => void;
  onEdit: (ticket: Ticket) => void;
}) {
  return (
    <Modal title={ticket.code} onClose={onClose}>
      <div className="ticket-detail">
        <div className="detail-badges">
          <span className={`badge severity-${ticket.severity}`}>
            {severityLabels[ticket.severity]}
          </span>
          <span className="badge status-badge">
            {statusLabels[ticket.status]}
          </span>
        </div>
        <h3>{ticket.title}</h3>
        <p className="detail-description">{ticket.description}</p>
        <dl>
          <div>
            <dt>Empresa</dt>
            <dd>{ticket.companyName}</dd>
          </div>
          <div>
            <dt>Solicitante</dt>
            <dd>{ticket.requesterName}</dd>
          </div>
          <div>
            <dt>Registrado</dt>
            <dd>{formatDate(ticket.createdAt)}</dd>
          </div>
        </dl>
        <div className="modal-actions">
          <button className="secondary" onClick={onClose}>
            Volver a la bandeja
          </button>
          <button className="primary" onClick={() => onEdit(ticket)}>
            Editar
          </button>
        </div>
      </div>
    </Modal>
  );
}

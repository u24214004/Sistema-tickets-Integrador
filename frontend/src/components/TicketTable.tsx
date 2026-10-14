import {
  formatDate,
  severityLabels,
  statusLabels,
  type TicketPage,
  type Ticket,
} from "../types";
export default function TicketTable({
  data,
  pending,
  onOpen,
  onPage,
}: {
  data: TicketPage | null;
  pending: boolean;
  onOpen: (ticket: Ticket) => void;
  onPage: (page: number) => void;
}) {
  if (pending)
    return (
      <div className="table-message" role="status">
        <span className="spinner" />
        Cargando solicitudes…
      </div>
    );
  if (!data)
    return (
      <div className="table-message">
        La bandeja no está disponible. Intenta actualizarla.
      </div>
    );
  if (!data.content.length)
    return (
      <div className="table-message">
        <span className="empty-symbol">◎</span>
        <h3>No hay tickets para mostrar</h3>
        <p>Registra una nueva solicitud o cambia los filtros.</p>
      </div>
    );
  return (
    <>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Ticket / asunto</th>
              <th>Empresa</th>
              <th>Severidad</th>
              <th>Estado</th>
              <th>Registro</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map((ticket) => (
              <tr key={ticket.id}>
                <td>
                  <button
                    className="ticket-link"
                    onClick={() => onOpen(ticket)}
                  >
                    <span className="ticket-code">{ticket.code}</span>
                    <span>{ticket.title}</span>
                  </button>
                </td>
                <td>{ticket.companyName}</td>
                <td>
                  <span className={`badge severity-${ticket.severity}`}>
                    {severityLabels[ticket.severity]}
                  </span>
                </td>
                <td>
                  <span className="status-dot" />
                  {statusLabels[ticket.status]}
                </td>
                <td className="date-cell">{formatDate(ticket.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pagination">
        <span>
          {data.totalElements} solicitudes · Página {data.number + 1} de{" "}
          {Math.max(1, data.totalPages)}
        </span>
        <div>
          <button
            className="secondary"
            disabled={data.number === 0}
            onClick={() => onPage(data.number - 1)}
          >
            Anterior
          </button>
          <button
            className="secondary"
            disabled={data.number + 1 >= data.totalPages}
            onClick={() => onPage(data.number + 1)}
          >
            Siguiente
          </button>
        </div>
      </div>
    </>
  );
}

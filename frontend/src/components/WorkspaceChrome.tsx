import type { User } from "../types";
export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark">tc</span>
        <span>
          TechConsulting<span className="brand-sub">MESA DE SERVICIO</span>
        </span>
      </div>
      <div className="nav-caption">ESPACIO DE TRABAJO</div>
      <div className="nav-item">
        <span>▤</span> Tickets de soporte
        <span className="nav-indicator" />
      </div>
      <div className="sidebar-bottom">
        <span className="connection-dot" /> Portal de atención ERP / TI
      </div>
    </aside>
  );
}
export function Topbar({
  user,
  onLogout,
}: {
  user: User;
  onLogout: () => void;
}) {
  return (
    <header className="topbar">
      <span>
        Mesa de servicio <span className="breadcrumb">/ Tickets</span>
      </span>
      <div className="user-menu">
        <span className="avatar">{user.name.slice(0, 1)}</span>
        <div>
          <strong>{user.name}</strong>
          <small>
            {user.role === "INTERNO"
              ? "Equipo de consultoría"
              : "Cliente corporativo"}
          </small>
        </div>
        <button className="logout" onClick={onLogout}>
          Salir
        </button>
      </div>
    </header>
  );
}

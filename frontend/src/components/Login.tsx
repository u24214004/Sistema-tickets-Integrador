import { useState, type FormEvent } from "react";
import { request } from "../api";
import type { Session } from "../types";
export default function Login({
  onLogin,
  notice,
}: {
  onLogin: (session: Session) => void;
  notice?: string;
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setPending(true);
    try {
      onLogin(
        await request<Session>("/auth/login", undefined, {
          method: "POST",
          body: JSON.stringify({ email: email.trim(), password }),
        }),
      );
    } catch (failure) {
      setError((failure as Error).message);
    } finally {
      setPending(false);
    }
  };
  return (
    <main className="login-layout">
      <section className="login-story">
        <div className="brand">
          <span className="brand-mark">tc</span>
          <span>
            TechConsulting
            <span className="brand-sub">PERÚ · SERVICIOS PROFESIONALES</span>
          </span>
        </div>
        <div className="story-copy">
          <span className="eyebrow">TU EQUIPO, SIEMPRE CONECTADO</span>
          <h1>
            El soporte empieza
            <br />
            con una conversación.
          </h1>
          <p>
            Centraliza tus solicitudes y sigue cada incidencia en un solo lugar.
          </p>
          <div className="story-line" />
          <div className="story-benefits">
            <span>01 · Registra tu solicitud</span>
            <span>02 · Sigue la atención</span>
            <span>03 · Conserva la trazabilidad</span>
          </div>
        </div>
        <div className="story-footer">Mesa de servicio ERP / TI</div>
      </section>
      <section className="login-access">
        <div className="login-card">
          <span className="eyebrow">BIENVENIDO A TU MESA DE SERVICIO</span>
          <h2>Inicia sesión</h2>
          <p className="muted">
            Ingresa con tu cuenta para consultar y registrar tickets.
          </p>
          {notice ? (
            <div className="notice" role="status">
              {notice}
            </div>
          ) : null}
          <form onSubmit={submit}>
            <label htmlFor="email">Correo electrónico</label>
            <input
              id="email"
              type="email"
              autoComplete="username"
              maxLength={150}
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="nombre@empresa.com"
              disabled={pending}
            />
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              maxLength={72}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Ingresa tu contraseña"
              disabled={pending}
            />
            {error ? (
              <div className="error" role="alert">
                {error}
              </div>
            ) : null}
            <button className="primary login-submit" disabled={pending}>
              {pending ? "Ingresando…" : "Iniciar sesión"}
            </button>
          </form>
          <p className="login-help">
            Si necesitas una cuenta, contacta al equipo de TechConsulting.
          </p>
        </div>
        <p className="access-footer">
          TechConsulting Perú · Portal de atención
        </p>
      </section>
    </main>
  );
}

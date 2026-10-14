import { useCallback, useEffect, useState } from "react";
import type { Session } from "./types";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import "./styles.css";
export default function App() {
  // Keep the bearer token in memory; a reload requires a fresh login.
  const [session, setSession] = useState<Session | null>(null);
  const [notice, setNotice] = useState("");
  const expired = useCallback(() => {
    setSession(null);
    setNotice("Tu sesión terminó. Inicia sesión nuevamente.");
  }, []);
  useEffect(() => {
    if (!session) return;
    const remaining = new Date(session.expiresAt).getTime() - Date.now();
    if (remaining <= 0) {
      expired();
      return;
    }
    const timer = window.setTimeout(expired, Math.min(remaining, 2147483647));
    return () => window.clearTimeout(timer);
  }, [session, expired]);
  if (!session)
    return (
      <Login
        notice={notice}
        onLogin={(value) => {
          setNotice("");
          setSession(value);
        }}
      />
    );
  return (
    <Dashboard
      session={session}
      onExpired={expired}
      onLogout={() => {
        setSession(null);
        setNotice("");
      }}
    />
  );
}

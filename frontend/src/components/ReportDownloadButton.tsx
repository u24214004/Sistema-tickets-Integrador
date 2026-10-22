import { useState } from "react";
import { ApiError, downloadReport } from "../api";

export default function ReportDownloadButton({
  token,
  status,
  severity,
  onError,
  onExpired,
}: {
  token: string;
  status: string;
  severity: string;
  onError: (message: string) => void;
  onExpired: () => void;
}) {
  const [pending, setPending] = useState(false);
  const download = async () => {
    setPending(true);
    onError("");
    const query = new URLSearchParams();
    if (status) query.set("status", status);
    if (severity) query.set("severity", severity);
    try {
      await downloadReport(
        `/reports/tickets.xlsx${query.size ? `?${query}` : ""}`,
        token,
      );
    } catch (failure) {
      const problem = failure as ApiError;
      if (problem.status === 401) onExpired();
      else onError(problem.message);
    } finally {
      setPending(false);
    }
  };
  return (
    <button
      className="secondary"
      onClick={download}
      disabled={pending}
      title="Exporta todos los tickets que coinciden con los filtros, no solo la página actual"
    >
      {pending ? "Generando Excel…" : "Exportar Excel"}
    </button>
  );
}

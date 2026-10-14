import type { Summary } from "../types";
export default function TicketStats({
  summary,
  pending,
}: {
  summary: Summary | null;
  pending: boolean;
}) {
  const stats = [
    {
      label: "Total de tickets",
      value: summary?.total,
      mark: "↗",
      className: "total",
    },
    { label: "Abiertos", value: summary?.open, mark: "◎", className: "open" },
    {
      label: "En proceso",
      value: summary?.inProgress,
      mark: "◷",
      className: "progress",
    },
    {
      label: "Resueltos",
      value: summary?.resolved,
      mark: "✓",
      className: "resolved",
    },
  ];

  return (
    <section className="stats-grid" aria-label="Resumen de tickets">
      {stats.map((stat) => (
        <article className={`stat-card ${stat.className}`} key={stat.label}>
          <div>
            <span>{stat.label}</span>
            <strong>{pending ? "…" : (stat.value ?? "—")}</strong>
          </div>
          <span className="stat-mark">{stat.mark}</span>
        </article>
      ))}
    </section>
  );
}

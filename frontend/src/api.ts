export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public fields: Record<string, string> = {},
  ) {
    super(message);
  }
}
export async function request<T>(
  path: string,
  token?: string,
  init: RequestInit = {},
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      ...init,
      headers: {
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new ApiError(
      "No pudimos conectar con el servicio. Inténtalo nuevamente.",
      0,
    );
  }
  let body;
  try {
    body = await response.json();
  } catch {
    throw new ApiError(
      "El servicio no devolvió una respuesta válida.",
      response.status,
    );
  }
  if (!response.ok)
    throw new ApiError(
      body.message ?? "No se pudo completar la operación.",
      response.status,
      body.fields,
    );
  return body as T;
}

export async function downloadReport(
  path: string,
  token: string,
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  } catch {
    throw new ApiError(
      "No pudimos conectar con el servicio. Inténtalo nuevamente.",
      0,
    );
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(
      problem.message ?? "No se pudo generar el reporte.",
      response.status,
    );
  }
  const file = await response.blob();
  if (
    file.type !==
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  ) {
    throw new ApiError(
      "El servicio no devolvió un archivo Excel válido.",
      response.status,
    );
  }
  const url = URL.createObjectURL(file);
  const link = document.createElement("a");
  link.href = url;
  link.download = "tickets.xlsx";
  document.body.append(link);
  try {
    link.click();
  } finally {
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }
}

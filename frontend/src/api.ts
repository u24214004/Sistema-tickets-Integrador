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

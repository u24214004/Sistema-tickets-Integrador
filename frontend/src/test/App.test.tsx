import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import App from "../App";

describe("Acceso a la mesa de servicio", () => {
  it("solicita credenciales antes de mostrar tickets", () => {
    render(<App />);
    expect(screen.getByLabelText("Correo electrónico")).toBeVisible();
    expect(screen.getByLabelText("Contraseña")).toBeVisible();
    expect(
      screen.getByRole("button", { name: "Iniciar sesión" }),
    ).toBeVisible();
    expect(
      screen.queryByRole("button", { name: "Nuevo ticket" }),
    ).not.toBeInTheDocument();
  });
  it("informa del rechazo de credenciales sin simular acceso", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        json: async () => ({ message: "Credenciales incorrectas" }),
      }),
    );
    const user = userEvent.setup();
    render(<App />);
    await user.type(
      screen.getByLabelText("Correo electrónico"),
      "cliente@demo.local",
    );
    await user.type(screen.getByLabelText("Contraseña"), "incorrecta");
    await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Credenciales incorrectas",
    );
    expect(
      screen.queryByRole("button", { name: "Nuevo ticket" }),
    ).not.toBeInTheDocument();
  });
  it("permite crear un ticket después de autenticarse", async () => {
    const ticket = {
      id: 9,
      code: "TC-2026-00009",
      title: "Error al guardar pedido",
      description: "No se registra la operación en SAP.",
      companyId: 1,
      companyName: "Andina",
      requesterName: "Cliente Demo",
      severity: "ALTA",
      status: "ABIERTO",
      createdAt: "2026-10-13T15:00:00Z",
    };
    const fetchMock = vi
      .fn()
      .mockImplementation(async (url: string, init?: RequestInit) => {
        let body;
        if (url.endsWith("/auth/login"))
          body = {
            token: "demo-token",
            expiresAt: "2099-01-01T00:00:00Z",
            user: {
              id: 2,
              name: "Cliente Demo",
              email: "cliente@demo.local",
              role: "CLIENTE",
              companyId: 1,
            },
          };
        else if (url.endsWith("/tickets/summary"))
          body = { total: 0, open: 0, inProgress: 0, resolved: 0 };
        else if (init?.method === "POST") body = ticket;
        else body = { content: [], totalElements: 0, totalPages: 0, number: 0 };
        return { ok: true, status: 200, json: async () => body };
      });
    vi.stubGlobal("fetch", fetchMock);
    const user = userEvent.setup();
    render(<App />);
    await user.type(
      screen.getByLabelText("Correo electrónico"),
      "cliente@demo.local",
    );
    await user.type(screen.getByLabelText("Contraseña"), "Demo2026!");
    await user.click(screen.getByRole("button", { name: "Iniciar sesión" }));
    await user.click(
      await screen.findByRole("button", { name: "Nuevo ticket" }),
    );
    await user.type(screen.getByLabelText("Asunto"), ticket.title);
    await user.type(screen.getByLabelText("Descripción"), ticket.description);
    await user.selectOptions(
      within(screen.getByRole("dialog")).getByLabelText("Severidad"),
      "ALTA",
    );
    await user.click(screen.getByRole("button", { name: "Crear ticket" }));
    expect(
      await screen.findByText("Ticket TC-2026-00009 creado correctamente."),
    ).toBeVisible();
    const postCall = fetchMock.mock.calls.find(
      ([url, init]) => url.endsWith("/tickets") && init?.method === "POST",
    );
    expect(JSON.parse(postCall![1].body)).toEqual({
      title: ticket.title,
      description: ticket.description,
      severity: "ALTA",
    });
  });
});

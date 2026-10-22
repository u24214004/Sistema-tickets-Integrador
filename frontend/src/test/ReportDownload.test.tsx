import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import Dashboard from "../components/Dashboard";
import type { Session } from "../types";

const session: Session = {
  token: "test-token",
  expiresAt: "2099-01-01T00:00:00Z",
  user: {
    id: 2,
    name: "Cliente Demo",
    email: "cliente@demo.local",
    role: "CLIENTE",
    companyId: 1,
    companyName: "Andina",
  },
};
function setup(reportOk = true) {
  const fetchMock = vi.fn().mockImplementation(async (url: string) => {
    if (url.includes("/reports/"))
      return {
        ok: reportOk,
        status: reportOk ? 200 : 500,
        blob: async () =>
          new Blob(["excel"], {
            type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          }),
        json: async () => ({ message: "No se pudo generar el reporte" }),
      };
    return {
      ok: true,
      status: 200,
      json: async () =>
        url.endsWith("/summary")
          ? { total: 0, open: 0, inProgress: 0, resolved: 0 }
          : { content: [], number: 0, totalElements: 0, totalPages: 0 },
    };
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}
afterEach(() => vi.unstubAllGlobals());
describe("Exportación de tickets", () => {
  it("descarga el Excel autenticado y aplica los filtros de la bandeja", async () => {
    const fetchMock = setup();
    const create = vi.fn().mockReturnValue("blob:reporte");
    const revoke = vi.fn();
    vi.stubGlobal(
      "URL",
      class extends URL {
        static createObjectURL = create;
        static revokeObjectURL = revoke;
      },
    );
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => {});
    const user = userEvent.setup();
    render(
      <Dashboard session={session} onLogout={vi.fn()} onExpired={vi.fn()} />,
    );
    await user.selectOptions(screen.getByLabelText("Severidad"), "ALTA");
    await user.click(
      await screen.findByRole("button", { name: "Exportar Excel" }),
    );
    await waitFor(() => expect(click).toHaveBeenCalledOnce());
    const call = fetchMock.mock.calls.find(([url]) =>
      url.includes("/reports/"),
    );
    expect(call?.[0]).toContain("severity=ALTA");
    expect(call?.[1].headers.Authorization).toBe("Bearer test-token");
    expect(create).toHaveBeenCalledOnce();
    await waitFor(() => expect(revoke).toHaveBeenCalledWith("blob:reporte"));
  });
  it("muestra el error sin descargar un archivo inválido", async () => {
    setup(false);
    const user = userEvent.setup();
    render(
      <Dashboard session={session} onLogout={vi.fn()} onExpired={vi.fn()} />,
    );
    await user.click(
      await screen.findByRole("button", { name: "Exportar Excel" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No se pudo generar el reporte",
    );
  });
});

package pe.edu.utp.tickets.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.utp.tickets.api.ApiException;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.Role;
import pe.edu.utp.tickets.company.Company;
import pe.edu.utp.tickets.ticket.Severity;
import pe.edu.utp.tickets.ticket.TicketDtos;
import pe.edu.utp.tickets.ticket.TicketService;
import pe.edu.utp.tickets.ticket.TicketStatus;

@ExtendWith(MockitoExtension.class)
class TicketReportServiceTest {

    private static final int LIMIT = 50;

    @Mock
    private TicketService tickets;

    @Mock
    private TicketExcelWriter writer;

    private TicketReportService service;

    private final Company acme = company(1L, "Acme SAC");
    private final AppUser client = user(10L, Role.CLIENTE, acme);
    private final AppUser agent = user(20L, Role.INTERNO, null);
    private final TicketDtos.View row = new TicketDtos.View(1L, "TC-2026-00001", "Asunto", "Detalle",
            1L, "Acme SAC", "Cliente", Severity.ALTA, TicketStatus.ABIERTO, Instant.now());

    @BeforeEach
    void setUp() {
        service = new TicketReportService(tickets, writer, LIMIT);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10001})
    void constructorRejectsLimitOutOfRange(int limit) {
        // When / Then
        assertThatThrownBy(() -> new TicketReportService(tickets, writer, limit))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void internalExportCoversAllCompaniesWithConfiguredLimit() throws IOException {
        // Given
        byte[] file = {1, 2, 3};
        when(tickets.reportRows(agent, TicketStatus.ABIERTO, null, LIMIT)).thenReturn(List.of(row));
        when(writer.write(ImmutableList.of(row), TicketStatus.ABIERTO, null, "Todas las empresas")).thenReturn(file);

        // When
        var result = service.export(agent, TicketStatus.ABIERTO, null);

        // Then
        assertThat(result).isSameAs(file);
    }

    @Test
    void clientExportIsScopedToOwnCompanyName() throws IOException {
        // Given
        when(tickets.reportRows(client, null, Severity.ALTA, LIMIT)).thenReturn(List.of(row));
        when(writer.write(any(), eq(null), eq(Severity.ALTA), eq("Acme SAC"))).thenReturn(new byte[0]);

        // When
        service.export(client, null, Severity.ALTA);

        // Then
        verify(writer).write(ImmutableList.of(row), null, Severity.ALTA, "Acme SAC");
    }

    @Test
    void serviceRejectionIsPropagatedWithoutWritingFile() {
        // Given
        var tooLarge = new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Demasiados tickets");
        when(tickets.reportRows(agent, null, null, LIMIT)).thenThrow(tooLarge);

        // When / Then
        assertThatThrownBy(() -> service.export(agent, null, null)).isSameAs(tooLarge);
        verifyNoInteractions(writer);
    }

    @Test
    void writerFailureBecomesInternalServerError() throws IOException {
        // Given
        when(tickets.reportRows(agent, null, null, LIMIT)).thenReturn(List.of(row));
        when(writer.write(any(), any(), any(), any())).thenThrow(new IOException("disk full"));

        // When / Then
        assertThatThrownBy(() -> service.export(agent, null, null))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(e.getMessage()).doesNotContain("disk full");
                });
    }

    private static Company company(long id, String name) {
        var company = new Company("20100000000", name);
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }

    private static AppUser user(long id, Role role, Company company) {
        var user = new AppUser("Usuario " + id, "user" + id + "@example.com", "hash", role, company);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

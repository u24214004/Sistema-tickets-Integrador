package pe.edu.utp.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.utp.tickets.api.ApiException;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.Role;
import pe.edu.utp.tickets.company.Company;
import pe.edu.utp.tickets.company.CompanyRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository tickets;

    @Mock
    private CompanyRepository companies;

    @Mock
    private EventRepository events;

    @InjectMocks
    private TicketService service;

    private final Company acme = company(1L, "Acme SAC");
    private final Company globex = company(2L, "Globex SA");
    private final AppUser client = user(10L, Role.CLIENTE, acme);
    private final AppUser agent = user(20L, Role.INTERNO, null);

    @Test
    void clientCreatesTicketForOwnCompanyAndRecordsEvent() {
        // Given
        var request = new TicketDtos.CreateRequest("  VPN   caída ", "Sin acceso", Severity.ALTA, null);
        when(companies.findById(1L)).thenReturn(Optional.of(acme));
        when(tickets.save(any(Ticket.class))).thenAnswer(call -> withId(call.getArgument(0), 100L));

        // When
        var view = service.create(client, request);

        // Then
        assertThat(view.id()).isEqualTo(100L);
        assertThat(view.companyId()).isEqualTo(1L);
        assertThat(view.title()).isEqualTo("VPN caída");
        assertThat(view.status()).isEqualTo(TicketStatus.ABIERTO);

        var event = ArgumentCaptor.forClass(TicketEvent.class);
        verify(events).save(event.capture());
        assertThat(event.getValue().getActor()).isSameAs(client);
        assertThat(event.getValue().getTicket().getId()).isEqualTo(100L);
    }

    @Test
    void clientCannotCreateTicketForAnotherCompany() {
        // Given
        var request = new TicketDtos.CreateRequest("Asunto", "Detalle", Severity.MEDIA, 2L);

        // When / Then
        assertThatThrownBy(() -> service.create(client, request))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(companies, tickets, events);
    }

    @Test
    void internalUserMustSelectCompany() {
        // Given
        var request = new TicketDtos.CreateRequest("Asunto", "Detalle", Severity.MEDIA, null);

        // When / Then
        assertThatThrownBy(() -> service.create(agent, request))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(companies, tickets, events);
    }

    @Test
    void unknownCompanyIsRejectedWithoutSaving() {
        // Given
        var request = new TicketDtos.CreateRequest("Asunto", "Detalle", Severity.BAJA, 99L);
        when(companies.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.create(agent, request))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(tickets, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void clientGetsNotFoundForAnotherCompanyTicket() {
        // Given
        when(tickets.findById(5L)).thenReturn(Optional.of(ticket(5L, globex)));

        // When / Then
        assertThatThrownBy(() -> service.detail(client, 5L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void missingTicketReturnsNotFound() {
        // Given
        when(tickets.findById(404L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.detail(agent, 404L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void internalUserMovesTicketToAnotherCompanyAndChangesStatus() {
        // Given
        var existing = ticket(7L, acme);
        var request = new TicketDtos.UpdateRequest("Nuevo asunto", "Nuevo detalle",
                Severity.CRITICA, TicketStatus.EN_PROCESO, 2L);
        when(tickets.findById(7L)).thenReturn(Optional.of(existing));
        when(companies.findById(2L)).thenReturn(Optional.of(globex));

        // When
        var view = service.update(agent, 7L, request);

        // Then
        assertThat(view.companyId()).isEqualTo(2L);
        assertThat(view.status()).isEqualTo(TicketStatus.EN_PROCESO);
        assertThat(view.severity()).isEqualTo(Severity.CRITICA);
        assertThat(existing.getCompany()).isSameAs(globex);
    }

    @Test
    void clientCannotMoveOwnTicketToAnotherCompany() {
        // Given
        var existing = ticket(8L, acme);
        var request = new TicketDtos.UpdateRequest("Asunto", "Detalle",
                Severity.MEDIA, TicketStatus.RESUELTO, 2L);
        when(tickets.findById(8L)).thenReturn(Optional.of(existing));

        // When / Then
        assertThatThrownBy(() -> service.update(client, 8L, request))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(existing.getCompany()).isSameAs(acme);
        assertThat(existing.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        verifyNoInteractions(companies);
    }

    @ParameterizedTest
    @CsvSource({"-1, 10", "0, 0", "0, 51"})
    void invalidPaginationIsRejectedBeforeQuerying(int page, int size) {
        // When / Then
        assertThatThrownBy(() -> service.list(agent, page, size, null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(tickets);
    }

    @Test
    void reportRowsRequestsOneExtraRowAndRejectsWhenLimitIsExceeded() {
        // Given
        var rows = List.of(ticket(1L, acme), ticket(2L, acme), ticket(3L, acme));
        when(tickets.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(rows));

        // When / Then
        assertThatThrownBy(() -> service.reportRows(agent, null, null, 2))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tickets).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void reportRowsReturnsAllRowsWithinLimit() {
        // Given
        var rows = List.of(ticket(1L, acme), ticket(2L, acme));
        when(tickets.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(rows));

        // When
        var result = service.reportRows(client, TicketStatus.ABIERTO, Severity.ALTA, 2);

        // Then
        assertThat(result).extracting(TicketDtos.View::id).containsExactly(1L, 2L);
    }

    @Test
    void summaryCountsTotalAndEachStatus() {
        // Given
        when(tickets.count(any(Specification.class))).thenReturn(6L, 3L, 2L, 1L);

        // When
        var summary = service.summary(client);

        // Then
        assertThat(summary).isEqualTo(new TicketDtos.Summary(6L, 3L, 2L, 1L));
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

    private Ticket ticket(long id, Company company) {
        return withId(new Ticket(company, client, "Asunto " + id, "Detalle", Severity.MEDIA), id);
    }

    private static Ticket withId(Ticket ticket, long id) {
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }
}

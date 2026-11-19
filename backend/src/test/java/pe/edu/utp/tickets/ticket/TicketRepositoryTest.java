package pe.edu.utp.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.Role;
import pe.edu.utp.tickets.auth.UserRepository;
import pe.edu.utp.tickets.company.Company;
import pe.edu.utp.tickets.company.CompanyRepository;

/**
 * Runs the Flyway schema on the H2 test database (PostgreSQL mode) so queries,
 * constraints and entity mappings are checked against the real migration.
 * It uses its own in-memory database so demo data loaded by other test
 * contexts never leaks into these assertions.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:repositorytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TicketRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private EventRepository events;

    @Autowired
    private UserRepository users;

    @Autowired
    private CompanyRepository companies;

    private Company acme;
    private Company globex;
    private AppUser client;

    @BeforeEach
    void setUp() {
        acme = companies.save(new Company("20111111111", "Acme SAC"));
        globex = companies.save(new Company("20222222222", "Globex SA"));
        client = users.save(new AppUser("Cliente Acme", "cliente@acme.test", "hash", Role.CLIENTE, acme));
    }

    @Test
    void savedTicketIsReloadedWithDefaultsAndGeneratedCode() {
        // Given
        var saved = tickets.save(new Ticket(acme, client, "  Impresora   sin red ", " Piso 3 ", Severity.MEDIA));
        entityManager.flush();
        entityManager.clear();

        // When
        var found = tickets.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getTitle()).isEqualTo("Impresora sin red");
        assertThat(found.getDescription()).isEqualTo("Piso 3");
        assertThat(found.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(found.getCompany().getName()).isEqualTo("Acme SAC");
        assertThat(found.getCode()).matches("TC-\\d{4}-\\d{5}");
    }

    @Test
    void specificationFiltersByCompanyAndStatusWithNewestFirst() {
        // Given
        var older = tickets.save(new Ticket(acme, client, "Primero", "Detalle", Severity.ALTA));
        var newer = tickets.save(new Ticket(acme, client, "Segundo", "Detalle", Severity.BAJA));
        var resolved = tickets.save(new Ticket(acme, client, "Resuelto", "Detalle", Severity.BAJA));
        resolved.update(acme, "Resuelto", "Detalle", Severity.BAJA, TicketStatus.RESUELTO);
        tickets.save(new Ticket(globex, client, "Otra empresa", "Detalle", Severity.ALTA));
        entityManager.flush();

        Specification<Ticket> openAtAcme = (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), acme.getId()),
                cb.equal(root.get("status"), TicketStatus.ABIERTO));
        var page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        // When
        var result = tickets.findAll(openAtAcme, page);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Ticket::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void countBySpecificationOnlyCountsMatchingSeverity() {
        // Given
        tickets.save(new Ticket(acme, client, "Uno", "Detalle", Severity.CRITICA));
        tickets.save(new Ticket(acme, client, "Dos", "Detalle", Severity.CRITICA));
        tickets.save(new Ticket(acme, client, "Tres", "Detalle", Severity.BAJA));
        entityManager.flush();

        Specification<Ticket> critical = (root, query, cb) -> cb.equal(root.get("severity"), Severity.CRITICA);

        // When
        long count = tickets.count(critical);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void pageSizeLimitsReturnedRowsButKeepsTotal() {
        // Given
        for (int i = 1; i <= 5; i++) {
            tickets.save(new Ticket(acme, client, "Ticket " + i, "Detalle", Severity.MEDIA));
        }
        entityManager.flush();

        // When
        var result = tickets.findAll(PageRequest.of(1, 2, Sort.by("id")));

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void creationEventIsStoredForTicketAndActor() {
        // Given
        var ticket = tickets.save(new Ticket(acme, client, "Con evento", "Detalle", Severity.ALTA));
        var saved = events.save(new TicketEvent(ticket, client));
        entityManager.flush();
        entityManager.clear();

        // When
        var found = events.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getAction()).isEqualTo("CREACION");
        assertThat(found.getTicket().getId()).isEqualTo(ticket.getId());
        assertThat(found.getActor().getEmail()).isEqualTo("cliente@acme.test");
        assertThat(found.getOccurredAt()).isNotNull();
    }

    @Test
    void findByEmailReturnsUserOrEmpty() {
        // When / Then
        assertThat(users.findByEmail("cliente@acme.test"))
                .hasValueSatisfying(user -> assertThat(user.getCompany().getId()).isEqualTo(acme.getId()));
        assertThat(users.findByEmail("nadie@acme.test")).isEmpty();
    }

    @Test
    void duplicateEmailIsRejectedByDatabase() {
        // Given
        var duplicate = new AppUser("Otro", "cliente@acme.test", "hash", Role.CLIENTE, globex);

        // When / Then
        assertThatThrownBy(() -> users.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void clientWithoutCompanyViolatesRoleConstraint() {
        // Given
        var orphan = new AppUser("Sin empresa", "huerfano@acme.test", "hash", Role.CLIENTE, null);

        // When / Then
        assertThatThrownBy(() -> users.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void companiesAreListedByName() {
        // When
        var names = companies.findAll(Sort.by("name")).stream().map(Company::getName).toList();

        // Then
        assertThat(names).containsExactly("Acme SAC", "Globex SA");
    }
}

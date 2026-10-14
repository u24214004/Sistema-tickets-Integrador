package pe.edu.utp.tickets.ticket;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRepository extends JpaRepository<TicketEvent,Long> {}

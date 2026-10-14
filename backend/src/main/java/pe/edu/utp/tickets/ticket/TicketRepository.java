package pe.edu.utp.tickets.ticket;
import org.springframework.data.jpa.repository.*;
public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {}

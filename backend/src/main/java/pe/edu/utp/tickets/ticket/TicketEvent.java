package pe.edu.utp.tickets.ticket;
import java.time.Instant;
import jakarta.persistence.*;
import pe.edu.utp.tickets.auth.AppUser;
@Entity @Table(name="ticket_events")
public class TicketEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ticket_id",nullable=false,updatable=false) private Ticket ticket;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id",nullable=false,updatable=false) private AppUser actor;
    @Column(nullable=false,updatable=false,length=50) private String action;
    @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
    protected TicketEvent() {}
    public TicketEvent(Ticket ticket, AppUser actor) {
        this.ticket=ticket; this.actor=actor; this.action="CREACION"; this.occurredAt=Instant.now();
    }
}

package pe.edu.utp.tickets.ticket;
import java.util.ArrayList;
import java.util.Objects;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.tickets.api.ApiException;
import pe.edu.utp.tickets.auth.*;
import pe.edu.utp.tickets.company.*;
@Service @Transactional(readOnly=true)
public class TicketService {
    private static final Logger log=LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository tickets;
    private final CompanyRepository companies;
    private final EventRepository events;
    public TicketService(TicketRepository tickets,CompanyRepository companies,EventRepository events) {
        this.tickets=tickets; this.companies=companies; this.events=events;
    }
    @Transactional
    public TicketDtos.View create(AppUser actor,TicketDtos.CreateRequest request) {
        Long companyId;
        if (actor.getRole()==Role.CLIENTE) {
            companyId=actor.getCompany().getId();
            if (request.companyId()!=null && !request.companyId().equals(companyId))
                throw new ApiException(HttpStatus.FORBIDDEN,"Solo puedes crear tickets para tu empresa");
        } else {
            companyId=request.companyId();
            if (companyId==null) throw new ApiException(HttpStatus.BAD_REQUEST,"Selecciona una empresa");
        }
        Company company=companies.findById(companyId)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,"Empresa no disponible"));
        Ticket ticket=tickets.save(new Ticket(company,actor,request.title(),request.description(),request.severity()));
        events.save(new TicketEvent(ticket,actor));
        log.info("Ticket creado id={} empresa_id={} actor_id={}",ticket.getId(),companyId,actor.getId());
        return TicketDtos.View.of(ticket);
    }
    @Transactional
    public TicketDtos.View update(AppUser actor,long id,TicketDtos.UpdateRequest request) {
        Ticket ticket=tickets.findById(id).orElseThrow(TicketService::notFound);
        if (actor.getRole()==Role.CLIENTE && !Objects.equals(actor.getCompany().getId(),ticket.getCompany().getId()))
            throw notFound();
        Long companyId = request.companyId() != null ? request.companyId() : ticket.getCompany().getId();
        if (actor.getRole()==Role.CLIENTE) {
            if (!Objects.equals(companyId, actor.getCompany().getId()))
                throw new ApiException(HttpStatus.FORBIDDEN,"Solo puedes editar tickets de tu empresa");
        } else {
            if (companyId==null) throw new ApiException(HttpStatus.BAD_REQUEST,"Selecciona una empresa");
        }
        Company company = ticket.getCompany();
        if (actor.getRole()!=Role.CLIENTE) {
            company = companies.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,"Empresa no disponible"));
        }
        ticket.update(company,request.title(),request.description(),request.severity(),request.status());
        log.info("Ticket actualizado id={} empresa_id={} actor_id={} status={}",ticket.getId(),company.getId(),actor.getId(),request.status());
        return TicketDtos.View.of(ticket);
    }
    public TicketDtos.View detail(AppUser actor,long id) {
        Ticket ticket=tickets.findById(id).orElseThrow(TicketService::notFound);
        if (actor.getRole()==Role.CLIENTE && !Objects.equals(actor.getCompany().getId(),ticket.getCompany().getId()))
            throw notFound();
        return TicketDtos.View.of(ticket);
    }
    public TicketDtos.PageView list(AppUser actor,int page,int size,TicketStatus status,Severity severity) {
        if (page<0 || size<1 || size>50) throw new ApiException(HttpStatus.BAD_REQUEST,"Paginación inválida");
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        return TicketDtos.PageView.of(tickets.findAll(scope(actor,status,severity),pageable));
    }
    public java.util.List<TicketDtos.View> reportRows(AppUser actor,TicketStatus status,Severity severity,int limit) {
        var page=PageRequest.of(0,limit+1,Sort.by(Sort.Direction.DESC,"createdAt","id"));
        var rows=tickets.findAll(scope(actor,status,severity),page).getContent();
        if(rows.size()>limit) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                "El reporte supera el límite de "+limit+" tickets. Aplica filtros para reducir los resultados.");
        }
        return rows.stream().map(TicketDtos.View::of).toList();
    }
    public TicketDtos.Summary summary(AppUser actor) {
        return new TicketDtos.Summary(tickets.count(scope(actor,null,null)),
            tickets.count(scope(actor,TicketStatus.ABIERTO,null)),
            tickets.count(scope(actor,TicketStatus.EN_PROCESO,null)),
            tickets.count(scope(actor,TicketStatus.RESUELTO,null)));
    }
    private static Specification<Ticket> scope(AppUser actor,TicketStatus status,Severity severity) {
        return (root,query,cb) -> {
            var filters=new ArrayList<Predicate>();
            if (actor.getRole()==Role.CLIENTE) filters.add(cb.equal(root.get("company").get("id"),actor.getCompany().getId()));
            if (status!=null) filters.add(cb.equal(root.get("status"),status));
            if (severity!=null) filters.add(cb.equal(root.get("severity"),severity));
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }
    private static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND,"Ticket no encontrado"); }
}

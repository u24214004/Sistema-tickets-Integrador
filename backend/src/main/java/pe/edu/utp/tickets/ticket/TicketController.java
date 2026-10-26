package pe.edu.utp.tickets.ticket;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.utp.tickets.auth.CurrentUser;
@RestController @RequestMapping("/api/tickets") @Transactional(readOnly=true)
public class TicketController {
    private final TicketService tickets;
    private final CurrentUser current;
    public TicketController(TicketService tickets,CurrentUser current) { this.tickets=tickets; this.current=current; }
    @GetMapping
    public TicketDtos.PageView list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size,@RequestParam(required=false) TicketStatus status,
        @RequestParam(required=false) Severity severity) {
        return tickets.list(current.resolve(jwt),page,size,status,severity);
    }
    @GetMapping("/summary")
    public TicketDtos.Summary summary(@AuthenticationPrincipal Jwt jwt) { return tickets.summary(current.resolve(jwt)); }
    @GetMapping("/{id}")
    public TicketDtos.View detail(@AuthenticationPrincipal Jwt jwt,@PathVariable long id) { return tickets.detail(current.resolve(jwt),id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public TicketDtos.View create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody TicketDtos.CreateRequest request) {
        return tickets.create(current.resolve(jwt),request);
    }
    @PutMapping("/{id}") @Transactional
    public TicketDtos.View update(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@Valid @RequestBody TicketDtos.UpdateRequest request) {
        return tickets.update(current.resolve(jwt),id,request);
    }
}

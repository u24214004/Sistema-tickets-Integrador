package pe.edu.utp.tickets.report;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.tickets.auth.CurrentUser;
import pe.edu.utp.tickets.ticket.Severity;
import pe.edu.utp.tickets.ticket.TicketStatus;

@RestController
@RequestMapping("/api/reports")
public class TicketReportController {

    private final TicketReportService reports;
    private final CurrentUser current;

    public TicketReportController(TicketReportService reports, CurrentUser current) {
        this.reports = reports;
        this.current = current;
    }

    @GetMapping("/tickets.xlsx")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Severity severity
    ) {
        byte[] file = reports.export(current.resolve(jwt), status, severity);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("tickets.xlsx").build().toString())
                .contentLength(file.length)
                .body(file);
    }
}

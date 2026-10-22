package pe.edu.utp.tickets.report;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.edu.utp.tickets.api.ApiException;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.Role;
import pe.edu.utp.tickets.ticket.*;

@Service
public class TicketReportService {
    private static final Logger log=LoggerFactory.getLogger(TicketReportService.class);
    private final TicketService tickets;
    private final TicketExcelWriter writer;
    private final int limit;
    public TicketReportService(TicketService tickets,TicketExcelWriter writer,@Value("${app.reports.max-rows:5000}") int limit) {
        if(limit<1 || limit>10000) throw new IllegalArgumentException("El límite de reportes debe estar entre 1 y 10000");
        this.tickets=tickets;this.writer=writer;this.limit=limit;
    }
    public byte[] export(AppUser actor,TicketStatus status,Severity severity) {
        Stopwatch stopwatch=Stopwatch.createStarted();
        try {
            var rows=ImmutableList.copyOf(tickets.reportRows(actor,status,severity,limit));
            String scope=actor.getRole()==Role.INTERNO ? "Todas las empresas" : actor.getCompany().getName();
            byte[] file=writer.write(rows,status,severity,scope);
            log.info("Reporte Excel generado actor_id={} cantidad={} duracion_ms={}",actor.getId(),rows.size(),stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return file;
        } catch(ApiException e) {
            log.warn("Exportación rechazada actor_id={} estado_http={}",actor.getId(),e.getStatus().value());
            throw e;
        } catch(IOException e) {
            log.error("Error al generar Excel actor_id={} tipo={}",actor.getId(),e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo generar el reporte de tickets");
        }
    }
}

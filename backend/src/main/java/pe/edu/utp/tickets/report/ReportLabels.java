package pe.edu.utp.tickets.report;
import pe.edu.utp.tickets.ticket.Severity;
import pe.edu.utp.tickets.ticket.TicketStatus;
final class ReportLabels {
    private ReportLabels() {}
    static String status(TicketStatus value) {
        return switch(value) {
            case ABIERTO -> "Abierto";
            case EN_PROCESO -> "En proceso";
            case EN_ESPERA -> "En espera";
            case RESUELTO -> "Resuelto";
        };
    }
    static String severity(Severity value) {
        return switch(value) {
            case CRITICA -> "Crítica";
            case ALTA -> "Alta";
            case MEDIA -> "Media";
            case BAJA -> "Baja";
        };
    }
}

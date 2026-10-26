package pe.edu.utp.tickets.ticket;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
public final class TicketDtos {
    private TicketDtos() {}
    public record CreateRequest(@NotBlank(message="Ingresa un asunto") @Size(max=150) String title,
        @NotBlank(message="Ingresa una descripción") @Size(max=4000) String description,
        @NotNull(message="Selecciona una severidad") Severity severity, @Positive Long companyId) {}
    public record UpdateRequest(@NotBlank(message="Ingresa un asunto") @Size(max=150) String title,
        @NotBlank(message="Ingresa una descripción") @Size(max=4000) String description,
        @NotNull(message="Selecciona una severidad") Severity severity,
        @NotNull(message="Selecciona un estado") TicketStatus status,
        @Positive Long companyId) {}
    public record View(Long id,String code,String title,String description,Long companyId,String companyName,
        String requesterName,Severity severity,TicketStatus status,Instant createdAt) {
        static View of(Ticket t) {
            return new View(t.getId(),t.getCode(),t.getTitle(),t.getDescription(),t.getCompany().getId(),
                t.getCompany().getName(),t.getRequester().getName(),t.getSeverity(),t.getStatus(),t.getCreatedAt());
        }
    }
    public record PageView(List<View> content,long totalElements,int totalPages,int number) {
        static PageView of(Page<Ticket> page) {
            return new PageView(page.getContent().stream().map(View::of).toList(),page.getTotalElements(),page.getTotalPages(),page.getNumber());
        }
    }
    public record Summary(long total,long open,long inProgress,long resolved) {}
}

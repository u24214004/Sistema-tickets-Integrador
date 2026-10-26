package pe.edu.utp.tickets.ticket;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.company.Company;
@Entity @Table(name="tickets")
public class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id",nullable=false) private Company company;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="requester_id",nullable=false) private AppUser requester;
    @Column(nullable=false,length=150) private String title;
    @Column(nullable=false,length=4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Severity severity;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TicketStatus status;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected Ticket() {}
    public Ticket(Company company, AppUser requester, String title, String description, Severity severity) {
        this.company=company; this.requester=requester;
        this.title=StringUtils.normalizeSpace(title); this.description=StringUtils.strip(description);
        this.severity=severity; this.status=TicketStatus.ABIERTO; this.createdAt=Instant.now();
    }
    public Long getId() { return id; }
    public String getCode() { return "TC-"+createdAt.atZone(ZoneId.of("America/Lima")).getYear()+"-"+String.format("%05d",id); }
    public Company getCompany() { return company; }
    public AppUser getRequester() { return requester; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(Company company, String title, String description, Severity severity, TicketStatus status) {
        this.company = company;
        this.title = StringUtils.normalizeSpace(title);
        this.description = StringUtils.strip(description);
        this.severity = severity;
        this.status = status;
    }
}

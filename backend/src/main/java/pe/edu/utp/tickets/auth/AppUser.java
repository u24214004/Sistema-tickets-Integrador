package pe.edu.utp.tickets.auth;
import jakarta.persistence.*;
import pe.edu.utp.tickets.company.Company;
@Entity @Table(name="app_users")
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=100) private String name;
    @Column(nullable=false, unique=true, length=150) private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") private Company company;
    protected AppUser() {}
    public AppUser(String name, String email, String passwordHash, Role role, Company company) {
        this.name=name; this.email=email; this.passwordHash=passwordHash; this.role=role; this.company=company;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Company getCompany() { return company; }
}

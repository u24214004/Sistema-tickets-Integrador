package pe.edu.utp.tickets.config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.tickets.auth.*;
import pe.edu.utp.tickets.company.*;
import pe.edu.utp.tickets.ticket.*;
@Component @ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final CompanyRepository companies;
    private final UserRepository users;
    private final TicketService tickets;
    private final PasswordEncoder passwords;
    public DemoData(CompanyRepository companies,UserRepository users,TicketService tickets,PasswordEncoder passwords) {
        this.companies=companies; this.users=users; this.tickets=tickets; this.passwords=passwords;
    }
    @Override @Transactional
    public void run(String... args) {
        if (users.count()>0) return;
        Company first=companies.save(new Company("20100000001","Andina Industrial S.A.C."));
        Company second=companies.save(new Company("20100000002","Comercial del Pacífico S.A.C."));
        String hash=passwords.encode("Demo2026!");
        users.save(new AppUser("Equipo TechConsulting","equipo@demo.local",hash,Role.INTERNO,null));
        AppUser client=users.save(new AppUser("Valeria Mendoza","cliente@demo.local",hash,Role.CLIENTE,first));
        AppUser other=users.save(new AppUser("Diego Torres","otrocliente@demo.local",hash,Role.CLIENTE,second));
        tickets.create(client,new TicketDtos.CreateRequest("Error al contabilizar una factura","SAP muestra un error al contabilizar la factura del proveedor.",Severity.ALTA,null));
        tickets.create(client,new TicketDtos.CreateRequest("Consulta sobre cierre mensual","Solicitamos orientación para ejecutar el cierre del módulo financiero.",Severity.MEDIA,null));
        tickets.create(other,new TicketDtos.CreateRequest("Acceso al módulo de inventario","El usuario no puede ingresar al módulo de inventario.",Severity.CRITICA,null));
    }
}

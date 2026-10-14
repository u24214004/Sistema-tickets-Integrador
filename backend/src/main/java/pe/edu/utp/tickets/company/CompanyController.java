package pe.edu.utp.tickets.company;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyRepository companies;
    public CompanyController(CompanyRepository companies) { this.companies=companies; }
    public record CompanyView(Long id,String name) {}
    @GetMapping @Transactional(readOnly=true)
    public List<CompanyView> list() {
        return companies.findAll(Sort.by("name")).stream().map(c -> new CompanyView(c.getId(),c.getName())).toList();
    }
}

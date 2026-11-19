package pe.edu.utp.tickets.company;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.tickets.config.SecurityConfig;

@WebMvcTest(controllers = CompanyController.class, properties = {
        "app.jwt-secret=webmvc-test-signing-key-not-for-production",
        "app.cors-origin=http://localhost:5173"
})
@Import(SecurityConfig.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CompanyRepository companies;

    @Test
    void internalUserGetsCompaniesSortedByName() throws Exception {
        // Given
        when(companies.findAll(Sort.by("name"))).thenReturn(List.of(company(1L, "Acme SAC"), company(2L, "Globex SA")));

        // When / Then
        mvc.perform(get("/api/companies").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INTERNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme SAC"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[0].ruc").doesNotExist());
    }

    @Test
    void clientIsForbiddenFromCompanyCatalog() throws Exception {
        // When / Then
        mvc.perform(get("/api/companies").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta operación"));
        verifyNoInteractions(companies);
    }

    private static Company company(long id, String name) {
        var company = new Company("20100000000", name);
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }
}

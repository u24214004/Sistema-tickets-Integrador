package pe.edu.utp.tickets;

import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.tickets.auth.AppUser;
import pe.edu.utp.tickets.auth.UserRepository;
import pe.edu.utp.tickets.ticket.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.reports.max-rows=20") @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class TicketReportApiTest {
    private static final String ENDPOINT="/api/reports/tickets.xlsx";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TicketService tickets;
    @Autowired TicketRepository ticketRepository;
    @Autowired EventRepository eventRepository;

    @BeforeEach void prepareKnownTicketsWithinRollbackTransaction() {
        eventRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        AppUser first=users.findByEmail("cliente@demo.local").orElseThrow();
        AppUser second=users.findByEmail("otrocliente@demo.local").orElseThrow();
        tickets.create(first,new TicketDtos.CreateRequest("Factura SAP","Incidencia de facturación",Severity.ALTA,null));
        tickets.create(first,new TicketDtos.CreateRequest("Consulta financiera","Orientación de cierre",Severity.MEDIA,null));
        tickets.create(second,new TicketDtos.CreateRequest("Inventario","Acceso al inventario",Severity.CRITICA,null));
    }

    private MockHttpServletRequestBuilder request(String email) {
        AppUser actor=users.findByEmail(email).orElseThrow();
        return get(ENDPOINT).with(jwt().jwt(token -> token.subject(actor.getId().toString())));
    }
    private XSSFWorkbook readReport(MockHttpServletRequestBuilder request) throws Exception {
        byte[] bytes=mvc.perform(request).andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"tickets.xlsx\""))
            .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }
    @Test void anonymousCannotExport() throws Exception {
        mvc.perform(get(ENDPOINT)).andExpect(status().isUnauthorized());
    }
    @Test void clientExportsAllOwnTicketsAndSummaryBeyondFirstPage() throws Exception {
        AppUser actor=users.findByEmail("cliente@demo.local").orElseThrow();
        for(int i=0;i<11;i++) tickets.create(actor,new TicketDtos.CreateRequest("Reporte "+i,"Prueba de reporte",Severity.ALTA,null));
        long expected=tickets.list(actor,0,50,null,null).totalElements();
        try(var book=readReport(request("cliente@demo.local"))) {
            assertThat(book.getNumberOfSheets()).isEqualTo(2);
            var detail=book.getSheet("Tickets");
            assertThat(detail.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Código");
            assertThat(detail.getLastRowNum()).isEqualTo((int)expected);
            for(int row=1;row<=detail.getLastRowNum();row++) {
                assertThat(detail.getRow(row).getCell(2).getStringCellValue()).isEqualTo(actor.getCompany().getName());
            }
            assertThat(detail.getPaneInformation().isFreezePane()).isTrue();
            var total=book.getSheet("Resumen").getRow(5).getCell(1);
            assertThat(total.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(total.getCellFormula()).isEqualTo("SUM(B2:B5)");
            assertThat(total.getNumericCellValue()).isEqualTo((double)expected);
        }
    }
    @Test void internalExportIncludesBothCompanies() throws Exception {
        try(var book=readReport(request("equipo@demo.local"))) {
            var detail=book.getSheet("Tickets");
            var names=new java.util.HashSet<String>();
            for(int row=1;row<=detail.getLastRowNum();row++) names.add(detail.getRow(row).getCell(2).getStringCellValue());
            assertThat(names).contains("Andina Industrial S.A.C.","Comercial del Pacífico S.A.C.");
        }
    }
    @Test void exportHonorsSeverityAndStatusFilters() throws Exception {
        try(var book=readReport(request("cliente@demo.local").param("severity","ALTA").param("status","ABIERTO"))) {
            var detail=book.getSheet("Tickets");
            assertThat(detail.getLastRowNum()).isPositive();
            for(int row=1;row<=detail.getLastRowNum();row++) {
                assertThat(detail.getRow(row).getCell(4).getStringCellValue()).isEqualTo("Alta");
                assertThat(detail.getRow(row).getCell(5).getStringCellValue()).isEqualTo("Abierto");
            }
        }
    }
    @Test void emptyReportHasHeadersAndZeroTotal() throws Exception {
        try(var book=readReport(request("cliente@demo.local").param("status","RESUELTO"))) {
            assertThat(book.getSheet("Tickets").getLastRowNum()).isZero();
            assertThat(book.getSheet("Resumen").getRow(5).getCell(1).getNumericCellValue()).isZero();
        }
    }
    @Test void userTextIsNotWrittenAsExcelFormula() throws Exception {
        AppUser actor=users.findByEmail("cliente@demo.local").orElseThrow();
        String title="=1+1";
        tickets.create(actor,new TicketDtos.CreateRequest(title,"Texto aportado por usuario",Severity.BAJA,null));
        try(var book=readReport(request("cliente@demo.local").param("severity","BAJA"))) {
            var detail=book.getSheet("Tickets");
            boolean found=false;
            for(int row=1;row<=detail.getLastRowNum();row++) {
                var cell=detail.getRow(row).getCell(1);
                assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
                if(cell.getStringCellValue().equals(title)) found=true;
            }
            assertThat(found).isTrue();
        }
    }
    @Test void invalidFiltersReturnBadRequest() throws Exception {
        mvc.perform(request("cliente@demo.local").param("severity","URGENTE")).andExpect(status().isBadRequest());
    }
    @Test void reportLimitRejectsInsteadOfTruncating() throws Exception {
        AppUser actor=users.findByEmail("cliente@demo.local").orElseThrow();
        for(int i=0;i<21;i++) tickets.create(actor,new TicketDtos.CreateRequest("Límite "+i,"Prueba del límite",Severity.BAJA,null));
        mvc.perform(request("cliente@demo.local"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.message").value("El reporte supera el límite de 20 tickets. Aplica filtros para reducir los resultados."));
    }
}

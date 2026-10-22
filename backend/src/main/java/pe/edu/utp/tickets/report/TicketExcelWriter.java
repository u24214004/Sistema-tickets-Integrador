package pe.edu.utp.tickets.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.edu.utp.tickets.ticket.*;

@Component
public class TicketExcelWriter {
    private static final ZoneId LIMA=ZoneId.of("America/Lima");
    private static final String[] HEADERS={"Código","Asunto","Empresa","Solicitante","Severidad","Estado","Fecha de creación (Lima)"};

    public byte[] write(ImmutableList<TicketDtos.View> tickets,TicketStatus status,Severity severity,String scope) throws IOException {
        // Guava builds stable, immutable groups used by both summary tables.
        var byStatus=Multimaps.index(tickets,TicketDtos.View::status);
        var bySeverity=Multimaps.index(tickets,TicketDtos.View::severity);
        try(var workbook=new XSSFWorkbook();var output=new ByteArrayOutputStream()) {
            CellStyle header=headerStyle(workbook);
            CellStyle date=workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            writeDetail(workbook,tickets,header,date);
            Sheet summary=workbook.createSheet("Resumen");
            summaryTable(summary,0,"Estado",TicketStatus.values(),byStatus,ReportLabels::status,header);
            summaryTable(summary,7,"Severidad",Severity.values(),bySeverity,ReportLabels::severity,header);
            metadata(summary,14,"Generado (America/Lima)",DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(LIMA).format(Instant.now()));
            metadata(summary,15,"Filtro de estado",status==null ? "Todos" : ReportLabels.status(status));
            metadata(summary,16,"Filtro de severidad",severity==null ? "Todas" : ReportLabels.severity(severity));
            metadata(summary,17,"Alcance",scope);
            summary.setColumnWidth(0,32*256);
            summary.setColumnWidth(1,48*256);
            summary.createFreezePane(0,1);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(output);
            return output.toByteArray();
        }
    }
    private void writeDetail(XSSFWorkbook workbook,ImmutableList<TicketDtos.View> tickets,CellStyle header,CellStyle date) {
        Sheet sheet=workbook.createSheet("Tickets");
        Row top=sheet.createRow(0);
        int[] widths={22,60,38,30,16,20,28};
        for(int i=0;i<HEADERS.length;i++) {
            Cell cell=top.createCell(i);cell.setCellValue(HEADERS[i]);cell.setCellStyle(header);
            sheet.setColumnWidth(i,widths[i]*256);
        }
        int index=1;
        for(var ticket:tickets) {
            Row row=sheet.createRow(index++);
            // Explicit string cells keep user-supplied text from becoming formulas.
            row.createCell(0).setCellValue(ticket.code());
            row.createCell(1).setCellValue(ticket.title());
            row.createCell(2).setCellValue(ticket.companyName());
            row.createCell(3).setCellValue(ticket.requesterName());
            row.createCell(4).setCellValue(ReportLabels.severity(ticket.severity()));
            row.createCell(5).setCellValue(ReportLabels.status(ticket.status()));
            Cell created=row.createCell(6);
            created.setCellValue(LocalDateTime.ofInstant(ticket.createdAt(),LIMA));created.setCellStyle(date);
        }
        sheet.createFreezePane(0,1);
        sheet.setAutoFilter(new CellRangeAddress(0,tickets.size(),0,HEADERS.length-1));
        sheet.getPrintSetup().setLandscape(true);
        sheet.setRepeatingRows(new CellRangeAddress(0,0,-1,-1));
    }
    private <E extends Enum<E>> void summaryTable(Sheet sheet,int start,String title,E[] keys,
            ImmutableListMultimap<E,TicketDtos.View> groups,Function<E,String> labels,CellStyle style) {
        Row header=sheet.createRow(start);
        header.createCell(0).setCellValue(title);header.getCell(0).setCellStyle(style);
        header.createCell(1).setCellValue("Cantidad");header.getCell(1).setCellStyle(style);
        int index=start+1;
        for(E key:keys) {
            Row row=sheet.createRow(index++);
            row.createCell(0).setCellValue(labels.apply(key));
            row.createCell(1).setCellValue(groups.get(key).size());
        }
        Row total=sheet.createRow(index);
        total.createCell(0).setCellValue("TOTAL");
        total.createCell(1).setCellFormula("SUM(B"+(start+2)+":B"+index+")");
    }
    private void metadata(Sheet sheet,int index,String label,String value) {
        Row row=sheet.createRow(index);row.createCell(0).setCellValue(label);row.createCell(1).setCellValue(value);
    }
    private CellStyle headerStyle(XSSFWorkbook workbook) {
        Font font=workbook.createFont();font.setBold(true);font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style=workbook.createCellStyle();style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}

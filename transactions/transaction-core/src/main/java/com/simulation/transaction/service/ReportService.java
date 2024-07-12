package com.simulation.transaction.service;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;
import com.simulation.transaction.entity.Cuenta;
import com.simulation.transaction.entity.Movimiento;
import com.simulation.transaction.repository.ICuentaRepository;
import com.simulation.transaction.repository.IMovimientosRepository;
import com.simulation.transaction.utils.BaseResponseVo;
import com.simulation.transaction.vo.CuentaVO;
import com.simulation.transaction.vo.MovimientoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
public class ReportService implements IReportService{

    @Autowired
    private ICuentaRepository cuentaRepository;

    @Autowired
    private IMovimientosRepository movimientoRepository;

    public BaseResponseVo generateAccountStatementReport(Long cuentaId, LocalDate startDate, LocalDate endDate) {
        Optional<Cuenta> cuentaOptional = cuentaRepository.findById(cuentaId);
        if (cuentaOptional.isEmpty()) {
            return BaseResponseVo.builder().message("Cuenta no encontrada.").build();
        }

        Cuenta cuenta = cuentaOptional.get();
        List<Movimiento> movimientos = movimientoRepository.findByCuentaAndFechaBetween(
            cuenta,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        List<MovimientoVO> movimientoVOs = movimientos.stream()
            .map(this::entityToVo)
            .collect(Collectors.toList());

        CuentaVO cuentaVO = CuentaVO.builder()
            .id(cuenta.getId())
            .numeroCuenta(cuenta.getNumeroCuenta())
            .tipoCuenta(cuenta.getTipoCuenta())
            .saldo(cuenta.getSaldo())
            .estado(cuenta.getEstado())
            .movimientos(movimientoVOs)
            .build();

        return BaseResponseVo.builder().data(cuentaVO).build();
    }

    @Override
    public ByteArrayInputStream generateAccountStatementPdf(Long cuentaId, LocalDate startDate,
        LocalDate endDate) throws IOException {
        Optional<Cuenta> cuentaOptional = cuentaRepository.findById(cuentaId);
        if (cuentaOptional.isEmpty()) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }

        Cuenta cuenta = cuentaOptional.get();
        List<Movimiento> movimientos = movimientoRepository.findByCuentaAndFechaBetween(
            cuenta,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(out));
        Document document = new Document(pdfDoc);

        // Usar fuente con soporte para UTF-8
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA, StandardCharsets.UTF_8.name());
        document.setFont(font);

        // Añadir título del reporte
        document.add(new Paragraph("Estado de Cuenta").setFont(font).setFontSize(18));
        document.add(new Paragraph("Numero de Cuenta: " + cuenta.getNumeroCuenta()).setFont(font));
        document.add(new Paragraph("Tipo de Cuenta: " + cuenta.getTipoCuenta()).setFont(font));
        document.add(new Paragraph("Saldo Inicial: " + cuenta.getSaldo()).setFont(font));
        document.add(new Paragraph("Estado: " + cuenta.getEstado()).setFont(font));
        document.add(new Paragraph(""));

        // Añadir tabla de movimientos
        float[] columnWidths = {1, 3, 2, 2, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.addHeaderCell("ID");
        table.addHeaderCell("Fecha");
        table.addHeaderCell("Tipo Movimiento");
        table.addHeaderCell("Valor");
        table.addHeaderCell("Saldo");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Movimiento movimiento : movimientos) {
            table.addCell(new Paragraph(movimiento.getId().toString()).setFont(font));
            table.addCell(new Paragraph(movimiento.getFecha().format(formatter)).setFont(font));
            table.addCell(new Paragraph(movimiento.getTipoMovimiento()).setFont(font));
            table.addCell(new Paragraph(movimiento.getValor().toString()).setFont(font));
            table.addCell(new Paragraph(movimiento.getSaldo().toString()).setFont(font));
        }

        document.add(table);
        document.close();

        return new ByteArrayInputStream(out.toByteArray());

    }

    private MovimientoVO entityToVo(Movimiento movimiento) {
        return MovimientoVO.builder()
            .id(movimiento.getId())
            .fecha(movimiento.getFecha())
            .tipoMovimiento(movimiento.getTipoMovimiento())
            .valor(movimiento.getValor())
            .saldo(movimiento.getSaldo())
            .build();
    }

}

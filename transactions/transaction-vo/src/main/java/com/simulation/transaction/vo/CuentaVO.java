package com.simulation.transaction.vo;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaVO {


    private Long id;

    @NotEmpty(message = "El dato número de cuenta es obligatorio llenar.")
    private String numeroCuenta;

    @NotEmpty(message = "El dato tipo de cuenta es obligatorio llenar.")
    private String tipoCuenta;

    @Builder.Default
    @DecimalMin(value = "0.00", inclusive = true, message = "El saldo inicial no puede ser negativo.")
    private BigDecimal saldo = BigDecimal.ZERO;

    @Builder.Default
    private String estado= "true";

    private List<MovimientoVO> movimientos;

    private Long clienteId;
}

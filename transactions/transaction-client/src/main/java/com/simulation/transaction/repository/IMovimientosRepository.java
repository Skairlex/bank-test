package com.simulation.transaction.repository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.simulation.transaction.entity.Cuenta;
import com.simulation.transaction.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IMovimientosRepository   extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByCuentaAndFechaBetween(Cuenta cuenta, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT SUM(m.valor) " +
        "FROM Movimiento m " +
        "WHERE m.cuenta.id = :cuentaId")
    BigDecimal sumOfValoresByCuentaId(@Param("cuentaId") Long cuentaId);

}

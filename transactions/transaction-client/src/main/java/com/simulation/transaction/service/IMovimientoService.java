package com.simulation.transaction.service;


import com.simulation.transaction.entity.Movimiento;
import com.simulation.transaction.request.MovimientoUpdateRequest;
import com.simulation.transaction.vo.MovimientoVO;
import com.simulation.transaction.utils.BaseResponseVo;

public interface IMovimientoService {

    BaseResponseVo getAllMovimientos();

    BaseResponseVo getMovimientoById(Long id);

    BaseResponseVo createMovimiento(MovimientoVO movimientos);

    BaseResponseVo updateMovimiento(MovimientoUpdateRequest movimientoDetails);

    BaseResponseVo deleteMovimiento(Long id);

    MovimientoVO entityToVo(Movimiento movimiento);
}

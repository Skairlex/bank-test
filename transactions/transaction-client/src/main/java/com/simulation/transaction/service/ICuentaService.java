package com.simulation.transaction.service;

import com.simulation.transaction.entity.Cuenta;
import com.simulation.transaction.vo.CuentaVO;
import com.simulation.transaction.utils.BaseResponseVo;

public interface ICuentaService {

    BaseResponseVo getAllCuentas();

    BaseResponseVo getCuentaById(Long id);

    BaseResponseVo getCuentaByAccountNumber(String accountNumber);

    BaseResponseVo createCuenta(CuentaVO cuenta);

    BaseResponseVo updateCuenta(CuentaVO cuentaDetails);

    BaseResponseVo deleteCuenta(Long id);

    CuentaVO entityToVo(Cuenta cuenta);
}

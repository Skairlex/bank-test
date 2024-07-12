package com.simulation.transaction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.simulation.transaction.common.TransactionsConstants;
import com.simulation.transaction.entity.Cuenta;
import com.simulation.transaction.entity.Movimiento;
import com.simulation.transaction.repository.ICuentaRepository;
import com.simulation.transaction.repository.IMovimientosRepository;
import com.simulation.transaction.request.MovimientoUpdateRequest;
import com.simulation.transaction.vo.CuentaVO;
import com.simulation.transaction.vo.MovimientoVO;
import com.simulation.transaction.utils.BaseResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Lazy
@Service
@Slf4j
public class MovimientoService implements IMovimientoService{


    @Autowired
    private IMovimientosRepository movimientosRepository;

    @Autowired
    private ICuentaRepository cuentaRepository;

    @Autowired ICuentaService cuentaService;

    @Override
    @Transactional(readOnly = true)
    public BaseResponseVo getAllMovimientos() {
        List<Movimiento> movimientoList= movimientosRepository.findAll();
        List<MovimientoVO> movimientoVOList= movimientoList.stream().map((mov->entityToVo(mov))).collect(
            Collectors.toList());
        return BaseResponseVo.builder().data(movimientoVOList).build();
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponseVo getMovimientoById(Long id) {
        Optional<Movimiento> movimientoOptional= movimientosRepository.findById(id);
        if(movimientoOptional.isPresent()){
            return BaseResponseVo.builder().data(entityToVo(movimientoOptional.get())).build();
        }else{
            return BaseResponseVo.builder().data(TransactionsConstants.TRANSACTION_NOT_FOUND_MESSAGE).build();
        }

    }

    @Override
    @Transactional
    public BaseResponseVo createMovimiento(MovimientoVO movimientoVO) {

        Optional<Cuenta> cuentaOpt = cuentaRepository.findByNumeroCuenta(movimientoVO.getCuenta().getNumeroCuenta());
        if (cuentaOpt.isPresent()) {
            Cuenta cuenta = cuentaOpt.get();
            if(!movimientoVO.getCuenta().getTipoCuenta().equals(cuenta.getTipoCuenta())){
                return BaseResponseVo.builder().message(TransactionsConstants.DIFFERENT_TYPE_ACCOUNT).build();
            }
            BigDecimal saldoActual = movimientosRepository.sumOfValoresByCuentaId(cuenta.getId()) ;
            if (saldoActual == null) {
                saldoActual = BigDecimal.ZERO;
            }
            BigDecimal valorMovimiento = movimientoVO.getValor();
            if (movimientoVO.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_WITHDRAWAL)) {//|| movimiento.getTipoMovimiento().equalsIgnoreCase("TRANSFERENCIA")) {
                if (saldoActual.compareTo(valorMovimiento) < 0) {
                    log.info("Saldo no disponible:"+cuenta.toString());
                    return BaseResponseVo.builder().message(TransactionsConstants.INSUFFICIENT_BALANCE_MESSAGE).build();

                }
                valorMovimiento=valorMovimiento.negate();
            }
            else if(!movimientoVO.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_DEPOSIT)){
                return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_TYPE_NOT_ALLOWED).build();
            }

            saldoActual=saldoActual.add(valorMovimiento);
            Movimiento movimiento=voToEntity(movimientoVO);//////
            movimiento.setSaldo(saldoActual);
            movimiento.setValor(valorMovimiento);
            movimiento.setFecha(LocalDateTime.now());
            cuenta.setSaldo(saldoActual);
            movimiento.setCuenta(cuenta);
            movimientosRepository.save(movimiento);
            cuentaRepository.save(cuenta);

            CuentaVO cuentaVOResponse= cuentaService.entityToVo(cuenta);
            MovimientoVO movimientoVOResponse= entityToVo(movimiento);
            movimientoVOResponse.setCuenta(cuentaVOResponse);
            return BaseResponseVo.builder().data(movimientoVOResponse).build();
        } else {
            log.info(TransactionsConstants.ACCOUNT_NOT_FOUND_MESSAGE+" : " +movimientoVO.toString());
            return BaseResponseVo.builder().message(TransactionsConstants.ACCOUNT_NOT_FOUND_MESSAGE).build();
        }
    }



    @Override
    @Transactional
    public BaseResponseVo updateMovimiento(MovimientoUpdateRequest movimientoDetails) {

        Optional<Movimiento> movimientoOptional = movimientosRepository.findById(movimientoDetails.getId());
        if(movimientoOptional.isEmpty()){
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_NOT_FOUND_MESSAGE)
                .build();
        }

        Movimiento movimientoToUpdate=movimientoOptional.get();
        Cuenta cuentaActual=movimientoToUpdate.getCuenta();
        BigDecimal saldoActual = movimientosRepository.sumOfValoresByCuentaId(cuentaActual.getId()) ;
        if (saldoActual == null) {
            saldoActual = BigDecimal.ZERO;
        }
        BigDecimal valorMovimiento = movimientoDetails.getValor();
        if(movimientoToUpdate.getSaldo().compareTo(cuentaActual.getSaldo()) != 0){
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_UPDATE_ERROR_MESSAGE).build();
        }


        BigDecimal saldoAnterior=saldoActual.subtract(movimientoToUpdate.getValor());
        /*
        //Regresa al anterior estado
        BigDecimal saldoActual = cuentaActual.getSaldo();
        if (movimientoToUpdate.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_WITHDRAWAL)) {
            saldoActual = saldoActual.add(movimientoToUpdate.getValor());
        } else if (movimientoToUpdate.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_DEPOSIT) ) {
            if (saldoActual.compareTo(movimientoToUpdate.getValor()) < 0) {
                log.info("Saldo no disponible:"+cuentaActual.toString());
                return BaseResponseVo.builder().message(TransactionsConstants.INSUFFICIENT_BALANCE_MESSAGE).build();
            }
            saldoActual = saldoActual.subtract(movimientoToUpdate.getValor());
        }else{
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_TYPE_NOT_ALLOWED).build();
        }
        //Actualiza con la nueva transacción
        // saldoActual = cuentaActual.getSaldoInicial();
        BigDecimal valorMovimiento = movimientoDetails.getValor();
        if (movimientoDetails.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_DEPOSIT)) {
            saldoActual = saldoActual.add(valorMovimiento);
        } else if (movimientoDetails.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_WITHDRAWAL) ) {
            if (saldoActual.compareTo(valorMovimiento) < 0) {
                log.info("Saldo no disponible:"+cuentaActual.toString());
                return BaseResponseVo.builder().message(TransactionsConstants.INSUFFICIENT_BALANCE_MESSAGE).build();
            }
            saldoActual = saldoActual.subtract(valorMovimiento);
        }else{
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_TYPE_NOT_ALLOWED).build();
        }*/

        //cuentaActual.setSaldo(saldoActual);
        //cuentaRepository.save(cuentaActual);
        if (movimientoDetails.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_WITHDRAWAL)) {//|| movimiento.getTipoMovimiento().equalsIgnoreCase("TRANSFERENCIA")) {
            if (saldoAnterior.compareTo(movimientoDetails.getValor()) < 0) {
                log.info("Saldo no disponible:"+cuentaActual.toString());
                return BaseResponseVo.builder().message(TransactionsConstants.INSUFFICIENT_BALANCE_MESSAGE).build();

            }
            valorMovimiento=valorMovimiento.negate();
        }
        else if(!movimientoDetails.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_DEPOSIT)){
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_TYPE_NOT_ALLOWED).build();
        }

        saldoActual=saldoAnterior.add(valorMovimiento);
        movimientoToUpdate.setTipoMovimiento(movimientoDetails.getTipoMovimiento());
        movimientoToUpdate.setFecha(movimientoDetails.getFecha());
        movimientoToUpdate.setValor(valorMovimiento);
        movimientoToUpdate.setSaldo(saldoActual);
        cuentaActual.setSaldo(saldoActual);
        movimientosRepository.save(movimientoToUpdate);
        cuentaRepository.save(cuentaActual);
        return BaseResponseVo.builder().data(entityToVo(movimientoToUpdate)).build();
    }

    @Override
    @Transactional
    public BaseResponseVo deleteMovimiento(Long id) {
        Optional<Movimiento> movimientoOptional = movimientosRepository.findById(id);
        if(movimientoOptional.isEmpty()){
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_NOT_FOUND_MESSAGE)
                .build();
        }

        Movimiento movimiento = movimientoOptional.get();
        Cuenta cuenta = movimiento.getCuenta();
        if(movimiento.getSaldo().compareTo(cuenta.getSaldo()) != 0){
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_DELETE_ERROR_MESSAGE).build();
        }
        if (null == cuenta) {
            return BaseResponseVo.builder()
                .message(TransactionsConstants.ACCOUNT_NOT_FOUND_MESSAGE)
                .build();
        }
        /*BigDecimal saldoActual = cuenta.getSaldo();
        if (movimiento.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_WITHDRAWAL)) {
            saldoActual = saldoActual.add(movimiento.getValor());
        } else if (movimiento.getTipoMovimiento().equalsIgnoreCase(TransactionsConstants.OPTION_DEPOSIT) ) {
            if (saldoActual.compareTo(movimiento.getValor()) < 0) {
                log.info("Saldo no disponible:"+cuenta.toString());
                return BaseResponseVo.builder().message(TransactionsConstants.INSUFFICIENT_BALANCE_MESSAGE).build();
            }
            saldoActual = saldoActual.subtract(movimiento.getValor());
        }else{
            return BaseResponseVo.builder().message(TransactionsConstants.TRANSACTION_TYPE_NOT_ALLOWED).build();
        }*/
        movimientosRepository.deleteById(id);
        BigDecimal saldoActual = movimientosRepository.sumOfValoresByCuentaId(cuenta.getId()) ;
        if (saldoActual == null) {
            saldoActual = BigDecimal.ZERO;
        }
        cuenta.setSaldo(saldoActual);
        cuentaRepository.save(cuenta);


        log.info(TransactionsConstants.TRANSACTION_DELETED_MESSAGE.format(id.toString()));
        return BaseResponseVo.builder().message(TransactionsConstants.SUCCESSFUL_TRANSACTION_DELETE)
            .build();
    }

    public MovimientoVO entityToVo(Movimiento movimiento){
        return MovimientoVO.builder()
            .id(movimiento.getId())
            .fecha(movimiento.getFecha())
            .tipoMovimiento(movimiento.getTipoMovimiento())
            .valor(movimiento.getValor())
            .saldo(movimiento.getSaldo())
            .build();
    }

    public Movimiento voToEntity(MovimientoVO movimiento){
        return Movimiento.builder()
            .id(movimiento.getId())
            .fecha(movimiento.getFecha())
            .tipoMovimiento(movimiento.getTipoMovimiento())
            .valor(movimiento.getValor())
            .saldo(movimiento.getSaldo())
            .build();
    }
}

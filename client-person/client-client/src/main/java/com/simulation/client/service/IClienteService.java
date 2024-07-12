package com.simulation.client.service;

import com.simulation.client.request.ClientRequest;
import com.simulation.client.utils.BaseResponseVo;
import com.simulation.client.vo.ClienteVO;

public interface IClienteService {

    BaseResponseVo getAllClientes() ;

    BaseResponseVo getClienteById(Long id) ;

    BaseResponseVo getClienteByIdentification(String identification);

    BaseResponseVo createCliente(ClienteVO cliente);

    BaseResponseVo updateCliente(ClientRequest clienteDetails);

    BaseResponseVo deleteCliente(String identification) ;
}

package com.example.service;

import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetail;
import com.example.entity.vo.request.ClientDetailVO;
import com.example.entity.vo.request.RuntimeDetailVO;

public interface ClientService {
    boolean verifyAndRegister(String token);
    String registerToken();
    Client findClientById(int id);
    Client findClientByToken(String token);
    void updateClientDetail(ClientDetailVO vo , Client client);
    void updateRuntimeDetail(RuntimeDetailVO vo, Client client);
}

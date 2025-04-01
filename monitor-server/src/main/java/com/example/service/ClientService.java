package com.example.service;

import com.example.entity.dto.Client;

public interface ClientService {
    boolean verifyAndRegister(String token);
    String registerToken();
    Client findClientById(int id);
    Client findClientByToken(String token);
}

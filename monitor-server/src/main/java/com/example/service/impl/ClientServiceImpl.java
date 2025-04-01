package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.mapper.ClientMapper;
import com.example.service.ClientService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    private String registerToken = this.generateNewToken(); //服务端 service 保存的token

    private final Map<Integer , Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String , Client> clientTokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initClientCache() {
        clientTokenCache.clear();
        clientIdCache.clear();
        this.list().forEach(this::addClientCache);
    }

    @Override
    public boolean verifyAndRegister(String token) {    //注册的时候就会将token存放在 Map中 方便后续拿取
        if (this.registerToken.equals(token)) {
            int id = this.randomClientId();
            Client client = new Client(id , "未命名主机" , token , new Date());
            if (this.save(client)) {
                registerToken = this.generateNewToken();
                this.addClientCache(client);
                return true;
            }
        }
        return false;
    }

    private void addClientCache(Client client) {
        clientIdCache.put(client.getId(), client);
        clientTokenCache.put(client.getToken(), client);
    }


    private int randomClientId() {
        return new Random().nextInt(90000000) + 10000000;
    }
    @Override
    public String registerToken() {
        return registerToken;
    }

    @Override
    public Client findClientById(int id) {
        return clientIdCache.get(id);
    }

    @Override
    public Client findClientByToken(String token) {
        return clientTokenCache.get(token);
    }
//随机生成token
    private String generateNewToken() {
        String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++)
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        System.out.println(sb);
        return sb.toString();
    }
}

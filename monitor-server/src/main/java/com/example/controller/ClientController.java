package com.example.controller;

import com.example.entity.RestBean;
import com.example.service.ClientService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
@Slf4j
public class ClientController {

    @Resource
    ClientService service;

    @GetMapping("/register")
    public RestBean<Void> registerClient(@RequestHeader("Authorization") String token) {
        log.info("服务端收到了客户端发送的token：{}", token);
        return service.verifyAndRegister(token) ?
                RestBean.success() : RestBean.failure(401, "客户端注册失败，请检查Token是否正确");
    }
}

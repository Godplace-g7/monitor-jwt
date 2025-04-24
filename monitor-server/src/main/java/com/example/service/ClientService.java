package com.example.service;

import com.example.entity.dto.Client;
import com.example.entity.vo.request.*;
import com.example.entity.vo.response.*;

import java.util.List;

public interface ClientService {
    boolean verifyAndRegister(String token);
    String registerToken();
    Client findClientById(int id);
    Client findClientByToken(String token);
    void updateClientDetail(ClientDetailVO vo , Client client);
    void updateRuntimeDetail(RuntimeDetailVO vo, Client client);

    List<ClientPreviewVO> listClients();   //发送服务器基本信息 运行时的相关数据信息 给前端页面

    ClientDetailsVO clientDetails(int clientId);

    void renameClient(RenameClientVO vo); //重命名服务器

    //重命名节点
    void renameNode(RenameNodeVO vo);

    RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId);

    RuntimeDetailVO clientRuntimeDetailsNow(int clientId);

    void deleteClient(int clientId);

    //获取一些基本信息 发给前端
    List<ClientSimpleVO> listSimpleList();


    //保存ssh连接信息
    void saveClientSshConnection(SshConnectionVO vo);

    //获取ssh所有设置
    SshSettingsVO sshSettings(int clientId);
}

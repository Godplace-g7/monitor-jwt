package com.example.service;

import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetail;
import com.example.entity.vo.request.ClientDetailVO;
import com.example.entity.vo.request.RenameClientVO;
import com.example.entity.vo.request.RenameNodeVO;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.response.ClientDetailsVO;
import com.example.entity.vo.response.ClientPreviewVO;
import com.example.entity.vo.response.ClientSimpleVO;
import com.example.entity.vo.response.RuntimeHistoryVO;

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
}

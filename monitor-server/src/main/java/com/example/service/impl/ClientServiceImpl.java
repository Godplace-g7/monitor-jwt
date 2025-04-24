package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Client;
import com.example.entity.dto.ClientDetail;
import com.example.entity.dto.ClientSsh;
import com.example.entity.vo.request.*;
import com.example.entity.vo.response.*;
import com.example.mapper.ClientDetailMapper;
import com.example.mapper.ClientMapper;
import com.example.mapper.ClientSshMapper;
import com.example.service.ClientService;
import com.example.utils.InfluxDbUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    @Resource
    ClientDetailMapper detailMapper;

    @Resource
    InfluxDbUtils influx;

    @Resource
    ClientSshMapper sshMapper;

    private String registerToken = this.generateNewToken(); //服务端 service 保存的token

    private final Map<Integer , Client> clientIdCache = new ConcurrentHashMap<>();
    private final Map<String , Client> clientTokenCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initClientCache() {
        clientTokenCache.clear();
        clientIdCache.clear();
        this.list().forEach(this::addClientCache); //调用当前类的list方法 返回一个集合 遍历集合中的每个元素 对每个元素调用 addClientCache 方法
    }

    @Override
    public boolean verifyAndRegister(String token) {    //注册的时候就会将token存放在 Map中 方便后续拿取
        if (this.registerToken.equals(token)) {
            int id = this.randomClientId();   //通过UUID随机生成一个ID
            Client client = new Client(id , "未命名主机" , token , "cn" , "未命名节点" , new Date());
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

    @Override
    public void updateClientDetail(ClientDetailVO vo, Client client) {
        ClientDetail detail = new ClientDetail();
        BeanUtils.copyProperties(vo, detail);//将从客户端传过来的vo 里面的除了id以外的复制给detail vo->dto
        detail.setId(client.getId());
        if(Objects.nonNull(detailMapper.selectById(detail.getId()))) {
            detailMapper.updateById(detail);
        }else  {
            detailMapper.insert(detail);
        }
    }

    private final Map<Integer, RuntimeDetailVO> currentRuntime = new ConcurrentHashMap<>();//用于存放最新的服务器运行监控数据

    //更新服务器运行时的数据
    @Override
    public void updateRuntimeDetail(RuntimeDetailVO vo, Client client) {
        currentRuntime.put(client.getId(), vo);//将RuntimeDetailVO运行的数据存放到对应的服务器id里面去 用map存放 每10s调用一次该接口
        influx.writeRuntimeData(client.getId(), vo);

    }
    //获取服务器注册信息 基本信息 运行时的数据 发送给前端的接口实现
    @Override
    public List<ClientPreviewVO> listClients() {
        return clientIdCache.values().stream().map(client -> {
            ClientPreviewVO vo = client.asViewObject(ClientPreviewVO.class); //转换复制对象 将client里面存放的注册信息复制给ClientPreviewVO
            BeanUtils.copyProperties(detailMapper.selectById(vo.getId()), vo); //将数据库里面存放的服务器基本信息复制给ClientPreviewVO
            RuntimeDetailVO runtime = currentRuntime.get(client.getId());//获取最新的服务器运行数据
            if(this.isOnline(runtime)) {
                BeanUtils.copyProperties(runtime, vo); //如果服务器正常运行 就把数据拷进ClientPreviewVO
                vo.setOnline(true);
            }
            return vo;
        }).toList();
    }

    @Override
    public ClientDetailsVO clientDetails(int clientId) {
        ClientDetailsVO vo = this.clientIdCache.get(clientId).asViewObject(ClientDetailsVO.class);//转换复制对象 将client里面存放的注册信息复制给ClientDetailsVO
        BeanUtils.copyProperties(detailMapper.selectById(clientId), vo); //将数据库里面存放的服务器基本信息复制给ClientDetailsVO
        vo.setOnline(this.isOnline(currentRuntime.get(clientId)));
        return vo;
    }

    //重命名
    @Override
    public void renameClient(RenameClientVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId()).set("name", vo.getName()));
        this.initClientCache();
    }

    //重命名节点
    @Override
    public void renameNode(RenameNodeVO vo) {
        this.update(Wrappers.<Client>update().eq("id", vo.getId())
                .set("node", vo.getNode()).set("location", vo.getLocation()));
        this.initClientCache();
    }

//获取历史的运行数据
    @Override
    public RuntimeHistoryVO clientRuntimeDetailsHistory(int clientId) {
        RuntimeHistoryVO vo = influx.readRuntimeData(clientId);
        ClientDetail detail = detailMapper.selectById(clientId);//把磁盘和内存拷进去
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }

//获取当前的运行数据
    @Override
    public RuntimeDetailVO clientRuntimeDetailsNow(int clientId) {
        return currentRuntime.get(clientId);
    }

//删除服务器接口
    @Override
    public void deleteClient(int clientId) {
        this.removeById(clientId);
        detailMapper.deleteById(clientId);
        this.initClientCache();
        currentRuntime.remove(clientId);
    }

//获取一些基本信息 发给前端
@Override
public List<ClientSimpleVO> listSimpleList() {
        return clientIdCache.values().stream().map(client -> {
            ClientSimpleVO vo = client.asViewObject(ClientSimpleVO.class);
            BeanUtils.copyProperties(detailMapper.selectById(vo.getId()), vo);
            return vo;
        }).toList();
    }


    //判断服务器是否离线   即能获取到服务器运行时数据并且获取到的最新数据是在1分钟以内  当前的系统时间-获取运行数据的时间
    private boolean isOnline(RuntimeDetailVO runtime) {
        return runtime != null && System.currentTimeMillis() - runtime.getTimestamp() < 60 * 1000;
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


    //保存ssh连接信息
    @Override
    public void saveClientSshConnection(SshConnectionVO vo) {
        Client client = clientIdCache.get(vo.getId());
        if(client == null) return;
        ClientSsh ssh = new ClientSsh();
        BeanUtils.copyProperties(vo, ssh); //vo->dto
        if(Objects.nonNull(sshMapper.selectById(client.getId()))) { //检查客户端是否已经存在ssh记录
            sshMapper.updateById(ssh);   //存在执行更新
        } else {
            sshMapper.insert(ssh);    //不存在执行插入
        }
    }

    //获取ssh所有设置
    @Override
    public SshSettingsVO sshSettings(int clientId) {
        ClientDetail detail = detailMapper.selectById(clientId);
        ClientSsh ssh = sshMapper.selectById(clientId);
        SshSettingsVO vo;
        if(ssh == null) {
            vo = new SshSettingsVO();//设置了端口ip
        } else {
            vo = ssh.asViewObject(SshSettingsVO.class);//dto->vo
        }
        vo.setIp(detail.getIp()); // 响应给客户端 port22 以及ip地址
        return vo;
    }


}

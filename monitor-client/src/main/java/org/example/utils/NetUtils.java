package org.example.utils;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BaseDetail;
import org.example.entity.ConnectionConfig;
import org.example.entity.Response;
import org.example.entity.RuntimeDetail;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class NetUtils {
    private final HttpClient client = HttpClient.newHttpClient();

    @Lazy
    @Resource
    ConnectionConfig config;

//向服务端发送注册请求
    public boolean registerToServer(String address, String token) {
        log.info("正在向服务端注册，请稍后...");
        Response response = this.doGet("/register", address, token);
        if(response.success()) {
            log.info("客户端注册已完成！");
        } else {
            log.error("客户端注册失败: {}", response.message());
        }
        return response.success();
    }

    private Response doGet(String url) {
        return this.doGet(url, config.getAddress(), config.getToken());
    }

    private Response doGet(String url, String address, String token) {   //get一般仅发送请求
        try {
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(new URI(address + "/monitor" + url))
                    .header("Authorization", token)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        } catch (Exception e) {
            log.error("请求失败，异常类型: {}, 错误详情: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return Response.errorResponse(e);
        }
    }


    private Response doPost(String url, Object data) {          //post中data即要发送的数据
        try {
            String rawData = JSONObject.from(data).toJSONString(); //一般发送json文本的数据
            HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(rawData))//将json数据转换为字符串
                    .uri(new URI(config.getAddress() + "/monitor" + url)) //存放在日志中的config 下次要用直接从config/server中读取出来
                    .header("Authorization", config.getToken())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        } catch (Exception e) {
            log.error("在发起服务端请求时出现问题", e);
            return Response.errorResponse(e);
        }
    }

    public void updateBaseDetails(BaseDetail detail) {
        Response response = this.doPost("/detail", detail);
        if(response.success()) {
            log.info("系统基本信息已更新完成");
        } else {
            log.error("系统基本信息更新失败: {}", response.message());
        }
    }

    public void updateRuntimeDetails(RuntimeDetail detail) {
        Response response = this.doPost("/runtime", detail);
        if(!response.success()) {
            log.warn("更新运行时状态时，接收到服务端的异常响应内容: {}", response.message());
        }
    }
}

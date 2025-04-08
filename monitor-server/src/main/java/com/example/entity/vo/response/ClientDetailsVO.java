package com.example.entity.vo.response;

import lombok.Data;
//响应给前端详情页面
@Data
public class ClientDetailsVO {
    int id;
    String name;
    boolean online;
    String node;
    String location;
    String ip;
    String cpuName;
    String osName;
    String osVersion;
    double memory;
    int cpuCore;
    double disk;
}

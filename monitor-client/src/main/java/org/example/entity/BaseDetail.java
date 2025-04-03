package org.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class BaseDetail {
    String osArch; //架构 centos ubuntu
    String osName; //名字
    String osVersion; //操作系统版本
    int osBit; //位数
    String cpuName;
    int cpuCore;
    double memory;//内存容量
    double disk;//硬盘容量
    String ip;
}

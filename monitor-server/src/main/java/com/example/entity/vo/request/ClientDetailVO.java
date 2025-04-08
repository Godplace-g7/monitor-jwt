package com.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
//服务器基本信息
@Data
public class ClientDetailVO {
    @NotNull
    String osArch;
    @NotNull
    String osName;
    @NotNull
    String osVersion;
    @NotNull
    int osBit;
    @NotNull
    String cpuName;
    @NotNull
    int cpuCore;
    @NotNull
    double memory;
    @NotNull
    double disk;
    @NotNull
    String ip;
}

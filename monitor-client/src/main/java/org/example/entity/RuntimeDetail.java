package org.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RuntimeDetail {
    long timestamp; //时间簇
    double cpuUsage; //cpu使用率
    double memoryUsage;//存储使用率
    double diskUsage;//磁盘使用率
    double networkUpload;//网络上传速度
    double networkDownload;//网络下载速度
    double diskRead;//磁盘读的速度
    double diskWrite;//磁盘写的速度
}

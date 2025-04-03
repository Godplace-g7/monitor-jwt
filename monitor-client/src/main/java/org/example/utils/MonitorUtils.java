package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.BaseDetail;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Component
public class MonitorUtils {

    private final SystemInfo info = new SystemInfo();//读取系统信息存放在info
    private final Properties properties = System.getProperties(); //Java本身jdk读取的操作系统的预设

    public BaseDetail monitorBaseDetail(){
        OperatingSystem os = info.getOperatingSystem();//获取从操作系统里面的信息 oshi依赖
        HardwareAbstractionLayer hardware = info.getHardware();//硬件信息
        double memory = hardware.getMemory().getTotal() /1024.0 /1024.0 /1024.0; //默认按字节来算(内存)
        //读取根目录每一个file sum()即为整个系统的硬盘容量 oshi只读单个磁盘的目录文件 无法计算整体磁盘容量
        double diskSize = Arrays.stream(File.listRoots()).mapToLong(File::getTotalSpace).sum() /1024.0 /1024.0 /1024.0;
        String ip = Objects.requireNonNull(this.findNetworkInterface(hardware)).getIPv4addr()[0];
        return new BaseDetail()
                .setOsArch(properties.getProperty("os.arch"))//操作系统架构 oshi依赖无法读取
                .setOsName(os.getFamily())
                .setOsVersion(os.getVersionInfo().getVersion())
                .setOsBit(os.getBitness())
                .setCpuName(hardware.getProcessor().getProcessorIdentifier().getName())
                .setCpuCore(hardware.getProcessor().getLogicalProcessorCount())
                .setMemory(memory)
                .setDisk(diskSize)
                .setIp(ip);
    }



/*    //读取网卡
    private NetworkIF findNetworkInterface(HardwareAbstractionLayer hardware) {
        try {
            for (NetworkIF network : hardware.getNetworkIFs()) {
                String[] ipv4Addr = network.getIPv4addr();
                NetworkInterface ni = network.queryNetworkInterface();
                if(!ni.isLoopback() && !ni.isVirtual() && ni.isUp() && !ni.isPointToPoint()
                        && (ni.getName().startsWith("eth") || ni.getName().startsWith("en"))
                        && ipv4Addr.length > 0 ){
                    return network;
                }
            }
        }catch (Exception e){
            log.error("读取网络接口信息出错，Read network error");;
        }
        return null;
    }*/

    //读取网卡
    private NetworkIF findNetworkInterface(HardwareAbstractionLayer hardware) {
        try {
            for (NetworkIF network : hardware.getNetworkIFs()) {
                String[] ipv4Addr = network.getIPv4addr();
                NetworkInterface ni = network.queryNetworkInterface();

                // 基础必要条件
                if(ni.isLoopback() || !ni.isUp() || ipv4Addr.length == 0) {
                    continue;
                }

                log.info("当前网卡: name={}, up={}, virtual={}, loopback={}, p2p={}",
                        ni.getName(),
                        ni.isUp(),
                        ni.isVirtual(),
                        ni.isLoopback(),
                        ni.isPointToPoint()
                );

                // 打印调试信息
                log.debug("检测网卡: name={}, virtual={}, p2p={}, ipv4={}",
                        ni.getName(), ni.isVirtual(), ni.isPointToPoint(), Arrays.toString(ipv4Addr));

                // 优先匹配物理网卡
                if(!ni.isVirtual() && !ni.isPointToPoint()) {
                    return network;
                }
            }

            // 如果没有物理网卡，则返回第一个符合条件的虚拟网卡
            for (NetworkIF network : hardware.getNetworkIFs()) {
                if(network.getIPv4addr().length > 0) {
                    return network;
                }
            }
        } catch(Exception e) {
            log.error("读取网络接口出错", e);
        }
        return null;
    }
}

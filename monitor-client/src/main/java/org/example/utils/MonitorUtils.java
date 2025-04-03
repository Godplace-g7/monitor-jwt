package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.BaseDetail;
import org.example.entity.RuntimeDetail;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Date;
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


    public RuntimeDetail monitorRuntimeDetail() {
        double statisticTime = 0.5; //定义一个统计时间 统计这段时间的相关数据
        try {
            HardwareAbstractionLayer hardware = info.getHardware(); //返回系统硬件信息
            NetworkIF networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware));
            CentralProcessor processor = hardware.getProcessor();//获取中央处理器
            double upload = networkInterface.getBytesSent(), download = networkInterface.getBytesRecv();//获取当前已经发送，已经接受的数量数据
            double read = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum();//将所有的硬盘读取量集中到一起
            double write = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum();//将所有的硬盘写入量集中到一起
            long[] ticks = processor.getSystemCpuLoadTicks();//获取cpu 的各项指标

            Thread.sleep((long) (statisticTime * 1000));//休眠1s

            networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware));
            upload = (networkInterface.getBytesSent() - upload) / statisticTime; //统计0.5秒内下载上传速度 xxByte/s
            download =  (networkInterface.getBytesRecv() - download) / statisticTime;
            read = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum() - read) / statisticTime;
            write = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum() - write) / statisticTime;

            double memory = (hardware.getMemory().getTotal() - hardware.getMemory().getAvailable()) / 1024.0 / 1024 / 1024;//总量减去当前可用的
            double disk = Arrays.stream(File.listRoots())
                    .mapToLong(file -> file.getTotalSpace() - file.getFreeSpace()).sum() / 1024.0 / 1024 / 1024;

            return new RuntimeDetail()
                    .setCpuUsage(this.calculateCpuUsage(processor, ticks)) //设置cpu指标
                    .setMemoryUsage(memory)
                    .setDiskUsage(disk)
                    .setNetworkUpload(upload / 1024)
                    .setNetworkDownload(download / 1024)
                    .setDiskRead(read / 1024/ 1024)
                    .setDiskWrite(write / 1024 / 1024)
                    .setTimestamp(new Date().getTime());
        } catch (Exception e) {
            log.error("读取运行时数据出现问题", e);
        }
        return null;
    }

//获取cpu各项指标的方法
private double calculateCpuUsage(CentralProcessor processor, long[] prevTicks) {
    long[] ticks = processor.getSystemCpuLoadTicks();
    long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
            - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
    long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
            - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
    long softIrq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
            - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
    long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
            - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
    long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
            - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
    long cUser = ticks[CentralProcessor.TickType.USER.getIndex()]
            - prevTicks[CentralProcessor.TickType.USER.getIndex()];
    long ioWait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
            - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
    long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
            - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
    long totalCpu = cUser + nice + cSys + idle + ioWait + irq + softIrq + steal;
    return (cSys + cUser) * 1.0 / totalCpu;
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

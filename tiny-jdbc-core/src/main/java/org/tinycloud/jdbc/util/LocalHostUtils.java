package org.tinycloud.jdbc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;

/**
 * <p>
 * java获取本地虚拟机IP
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-07-26 15:11:53
 */
public class LocalHostUtils {
    private static final Logger log = LoggerFactory.getLogger(LocalHostUtils.class);

    /**
     * 获取本机的 IPv4 地址。
     * 该方法会遍历所有网络接口，尝试获取第一个非回环的 IPv4 地址。
     * 如果未找到合适的地址，则会尝试使用 InetAddress.getLocalHost() 方法获取本地主机地址。
     *
     * @return 本机的 IPv4 地址，如果获取失败则抛出异常
     */
    public static String getLocalHost() {
        String ipLocalHost = null;
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (netInterface.isUp()) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                            String hostAddress = address.getHostAddress();
                            if (hostAddress != null && !hostAddress.equals("127.0.0.1") && !hostAddress.equals("/127.0.0.1")) {
                                // 得到本地IP
                                ipLocalHost = address.toString().split("[/]")[1];
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Cannot get first non-loopback ip address：", e);
        }
        if (ipLocalHost != null && !ipLocalHost.isEmpty()) {
            return ipLocalHost;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to retrieve localhost");
        }
    }

    /**
     * 获取本机的非回环 IPv4 地址对应的 InetAddress 对象。
     * 该方法会遍历所有网络接口，尝试获取第一个符合条件的非回环 IPv4 地址对应的 InetAddress 对象。
     * 如果未找到合适的地址，则会尝试使用 InetAddress.getLocalHost() 方法获取本地主机的 InetAddress 对象。
     *
     * @return 本机的非回环 IPv4 地址对应的 InetAddress 对象，如果获取失败则抛出异常
     */
    public static InetAddress getInetAddress() {
        InetAddress inetAddress = null;
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                if (netInterface.isUp()) {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress temp = addresses.nextElement();
                        if (temp instanceof Inet4Address && !temp.isLoopbackAddress()) {
                            String host = temp.getHostAddress();
                            if (host != null && !"0.0.0.0".equals(host) && !"127.0.0.1".equals(host)) {
                                inetAddress = temp;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Cannot get first non-loopback address：", e);
        }
        if (inetAddress != null) {
            return inetAddress;
        }
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to retrieve localhost");
        }
    }
}

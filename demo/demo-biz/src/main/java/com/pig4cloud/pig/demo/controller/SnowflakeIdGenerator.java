package com.pig4cloud.pig.demo.controller;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SnowflakeId + Nacos
 * <p>
 * [用Nacos分配Snowflake的Worker ID_贾小黑的博客-CSDN博客_snowflake workerid](https://blog.csdn.net/JustinJia91/article/details/119155812)
 */
@Component
public class SnowflakeIdGenerator {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private NacosServiceManager nacosServiceManager;

	@Autowired
	private NacosDiscoveryProperties nacosDiscoveryProperties;

	private static Snowflake snowflakeIdWorker;

	private static int nodeId;

	private static String ip;
	private static int port;

	@PostConstruct
	public void run() throws Exception {
		init();
	}

	/**
	 * 获取雪花Id
	 *
	 * @return
	 */
	public static long nextId() {
		return snowflakeIdWorker.nextId();
	}

	/**
	 * 获取当前节点Id
	 *
	 * @return
	 */
	public static int nodeId() {
		return nodeId;
	}

	/**
	 * 获取当前节点的ip和端口号
	 *
	 * @return ip 端口号
	 */
	public static String ipPort() {
		return String.format("%s:%s", ip, port);
	}

	/**
	 * 获取当前节点的ip
	 *
	 * @return ip
	 */
	public static String ip() {
		return ip;
	}

	/**
	 * 获取当前节点的ip和端口号
	 *
	 * @return ip 端口号
	 */
	public static int port() {
		return port;
	}

	/**
	 * 获取当前服务所有节点 + 增加服务监听
	 *
	 * @throws NacosException 异常
	 */
	private void init() throws NacosException {
		NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
		namingService.subscribe(nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), event -> {
			if (-1 == nacosDiscoveryProperties.getPort()) {
				return;
			}
			nodeId = calcNodeIndex(((NamingEvent) event).getInstances());
			if (nodeId > 1024) {
				throw new IllegalArgumentException("Worker & Datacenter Id calc results exceed 1024");
			}
			long workerId = nodeId % 32;
			long datacenterId = (long) Math.floor((float) nodeId / 32);
			logger.info("nodeId:" + nodeId + " workerId:" + workerId + " datacenterId:" + datacenterId);
			snowflakeIdWorker = IdUtil.getSnowflake(workerId, datacenterId);
			ip = nacosDiscoveryProperties.getIp();
			port = nacosDiscoveryProperties.getPort();
		});
	}

	/**
	 * 用ip+port计算服务列表的索引
	 *
	 * @param instanceList 实例列表
	 * @return 当前节点索引
	 */
	private int calcNodeIndex(List<Instance> instanceList) {
		List<Long> ipPortList = instanceList.stream().map(x -> dealIpPort(x.getIp(), x.getPort())).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
		return ipPortList.indexOf(dealIpPort(nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort()));
	}

	/**
	 * ip补0 + 端口号
	 * 如：192.168.199.107:7001 => 1921681991077001
	 *
	 * @param ip   IP地址
	 * @param port 端口号
	 * @return 合并ip和端口号
	 */
	private static Long dealIpPort(String ip, int port) {
		String[] ips = ip.split("\\.");
		StringBuilder sbr = new StringBuilder();
		for (String s : ips) {
			sbr.append(new DecimalFormat("000").format(Integer.parseInt(s)));
		}
		return Long.parseLong(sbr.toString() + port);
	}

}

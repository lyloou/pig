//package com.pig4cloud.pig.demo.controller;
//
//import com.alibaba.cloud.nacos.NacosConfigProperties;
//import com.alibaba.nacos.api.config.ConfigService;
//import lombok.SneakyThrows;
//import org.apache.ibatis.transaction.Transaction;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.util.StringUtils;
//
//import javax.annotation.PostConstruct;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.atomic.AtomicLong;
//
//public class ThreadPoolConfigUpdateListener {
//
//	@Value("${apollo.bootstrap.namespaces:application}")
//	private String namespace;
//
//	@Autowired
//	private DynamicThreadPoolFacade dynamicThreadPoolManager;
//
//	@Autowired
//	private DynamicThreadPoolProperties poolProperties;
//
//	@PostConstruct
//	public void init() {
//		initConfigUpdateListener();
//	}
//
//	@SneakyThrows
//	public void initConfigUpdateListener() {
//		String apolloNamespace = namespace;
//		if (StringUtils.hasText(poolProperties.getApolloNamespace())) {
//			apolloNamespace = poolProperties.getApolloNamespace();
//		}
//		String finalApolloNamespace = apolloNamespace;
//		NacosConfigProperties.Config config = ConfigService.getConfig(finalApolloNamespace);
//		config.addChangeListener(changeEvent -> {
//			try {
//				Thread.sleep(poolProperties.getWaitRefreshConfigSeconds() * 1000);
//			} catch (InterruptedException e) {
//				log.error("配置刷新异常", e);
//			}
//			dynamicThreadPoolManager.refreshThreadPoolExecutor();
//			log.info("线程池配置有变化，刷新完成");
//		});
//	}
//
//	public void refreshThreadPoolExecutor(DynamicThreadPoolProperties dynamicThreadPoolProperties) {
//		dynamicThreadPoolProperties.getExecutors().forEach(poolProperties -> {
//			NaughtyThreadPoolTaskExecutor executor = getExecutor(poolProperties.getThreadPoolName());
//			if (executor == null) {
//				executor = new NaughtyThreadPoolTaskExecutor();
//
//				managerExecutor(executor, poolProperties);
//				executor.setBlockingQueue(getBlockingQueue(poolProperties.getQueueType(), poolProperties.getQueueCapacity()));
//
//				executor.initialize();
//				//将new出的对象放入Spring容器中
//				defaultListableBeanFactory.registerSingleton(poolProperties.getThreadPoolName(), executor);
//				//自动注入依赖
//				autowireCapableBeanFactory.autowireBean(executor);
//			} else {
//				managerExecutor(executor, poolProperties);
//				BlockingQueue<Runnable> queue = executor.getThreadPoolExecutor().getQueue();
//				if (queue instanceof ResizableCapacityLinkedBlockIngQueue) {
//					((ResizableCapacityLinkedBlockIngQueue<Runnable>) queue).setCapacity(poolProperties.getQueueCapacity());
//				}
//			}
//
//		});
//	}
//
//	private void managerExecutor(NaughtyThreadPoolTaskExecutor executor, ThreadPoolProperties poolProperties) {
//		try {
//			if (executor != null) {
//				executor.setBeanName(poolProperties.getThreadPoolName());
//				executor.setCorePoolSize(poolProperties.getCorePoolSize());
//				executor.setMaxPoolSize(poolProperties.getMaximumPoolSize());
//				executor.setKeepAliveSeconds((int) poolProperties.getKeepAliveTime());
//				executor.setRejectedExecutionHandler(this.getRejectedExecutionHandler(poolProperties.getRejectedExecutionType(), poolProperties.getThreadPoolName()));
//				executor.setThreadPoolName(poolProperties.getThreadPoolName());
//			}
//		} catch (Exception e) {
//			log.error("Executor 参数设置异常", e);
//		}
//	}
//
//
//	@Override
//	protected void beforeExecute(Thread t, Runnable r) {
//		String threadName = Thread.currentThread().getName();
//		Transaction transaction = Cat.newTransaction(threadPoolName, runnableNameMap.get(r.getClass().getSimpleName()));
//		transactionMap.put(threadName, transaction);
//		super.beforeExecute(t, r);
//	}
//
//	@Override
//	protected void afterExecute(Runnable r, Throwable t) {
//		super.afterExecute(r, t);
//		String threadName = Thread.currentThread().getName();
//		Transaction transaction = transactionMap.get(threadName);
//		transaction.setStatus(Message.SUCCESS);
//		if (t != null) {
//			Cat.logError(t);
//			transaction.setStatus(t);
//		}
//		transaction.complete();
//		transactionMap.remove(threadName);
//	}
//
//	public StatusExtension registerStatusExtension(ThreadPoolProperties prop, Object object) {
//		NaughtyThreadPoolTaskExecutor executor = (NaughtyThreadPoolTaskExecutor) object;
//		StatusExtension statusExtension =  new StatusExtension() {
//			@Override
//			public String getId() {
//				return "thread.pool.info." + prop.getThreadPoolName();
//			}
//
//			@Override
//			public String getDescription() {
//				return "线程池监控";
//			}
//
//			@Override
//			public Map<String, String> getProperties() {
//				AtomicLong rejectCount = getRejectCount(prop.getThreadPoolName());
//
//				Map<String, String> pool = new HashMap<>();
//				pool.put("activeCount", String.valueOf(executor.getActiveCount()));
//				pool.put("keepAliveTime", String.valueOf(executor.getKeepAliveSeconds()));
//				int coreSize = executor.getCorePoolSize();
//				int maxSize = executor.getMaxPoolSize();
//				if (coreSize!=0){
//					pool.put("active/core", String.valueOf(Float.valueOf(executor.getActiveCount())/Float.valueOf(coreSize)));
//				}
//				if (maxSize!=0){
//					pool.put("active/max", String.valueOf(Float.valueOf(executor.getActiveCount())/Float.valueOf(maxSize)));
//				}
//				pool.put("coreSize", String.valueOf(executor.getCorePoolSize()));
//				pool.put("maxSize", String.valueOf(executor.getMaxPoolSize()));
//				ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
//				pool.put("completedTaskCount", String.valueOf(threadPoolExecutor.getCompletedTaskCount()));
//				pool.put("largestPoolSize", String.valueOf(threadPoolExecutor.getLargestPoolSize()));
//				pool.put("taskCount", String.valueOf(threadPoolExecutor.getTaskCount()));
//				pool.put("rejectCount", String.valueOf(rejectCount == null ? 0 : rejectCount.get()));
//				pool.put("queueSize", String.valueOf(threadPoolExecutor.getQueue().size()));
//				return pool;
//			}
//		};
//		StatusExtensionRegister.getInstance().register(statusExtension);
//		return statusExtension;
//	}
//}
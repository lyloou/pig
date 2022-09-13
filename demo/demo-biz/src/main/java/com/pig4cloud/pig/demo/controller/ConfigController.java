package com.pig4cloud.pig.demo.controller;

import cn.hutool.core.util.StrUtil;
import com.pig4cloud.pig.common.security.annotation.Inner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
@RefreshScope
@Inner(value = false)
public class ConfigController {

	@Value("${useLocalCache:false}")
	private boolean useLocalCache;

	@RequestMapping("/get")
	public boolean get() {
		return useLocalCache;
	}


	@GetMapping("id")
	public String id() {
		return StrUtil.format("nodeId:{}, id:{}, ipPort:{}",
				SnowflakeIdGenerator.nodeId(), SnowflakeIdGenerator.nextId(),
				SnowflakeIdGenerator.ipPort());
	}

}
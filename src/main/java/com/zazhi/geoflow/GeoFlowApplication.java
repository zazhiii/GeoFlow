package com.zazhi.geoflow;

import cn.hutool.crypto.SecureUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeoFlowApplication {

	public static void main(String[] args) {
		SecureUtil.disableBouncyCastle();
		SpringApplication.run(GeoFlowApplication.class, args);
	}

}

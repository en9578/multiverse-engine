package com.minbao.multiverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 多元宇宙引擎 — 启动入口
 */
@SpringBootApplication
@EnableAsync
public class MultiverseApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiverseApplication.class, args);
    }
}

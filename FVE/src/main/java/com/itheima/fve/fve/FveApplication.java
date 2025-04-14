package com.itheima.fve.fve;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement
@EnableCaching
@ComponentScan(basePackages = {"com.itheima.fve.fve.controller",
        "com.itheima.fve.fve.service",
        "com.itheima.fve.fve.config",
        "com.itheima.fve.fve.common",
        "com.itheima.fve.fve.Utils" })
public class FveApplication {
    public static void main(String[] args){
        SpringApplication.run(FveApplication.class,args);
        log.info("项目启动成功...");
    }
}

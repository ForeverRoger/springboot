package com.offcn;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@MapperScan("com.offcn.mapper")
@SpringBootApplication
public class GoodsServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceApp.class,args);
    }
}

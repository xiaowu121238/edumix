package com.itxiaowu.edumix;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** EduMix 后端启动入口，负责加载 Spring Boot 配置并扫描 MyBatis-Plus Mapper。 */
@MapperScan("com.itxiaowu.edumix.mapper")
@SpringBootApplication
public class EdumixApplication {

  public static void main(String[] args) {
    SpringApplication.run(EdumixApplication.class, args);
  }
}

package com.itxiaowu.edumix;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.itxiaowu.edumix.mapper")
@SpringBootApplication
public class EdumixApplication {

  public static void main(String[] args) {
    SpringApplication.run(EdumixApplication.class, args);
  }
}

package org.donghaobin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("org.donghaobin.mapper")
@SpringBootApplication
public class TalkingTogetherApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalkingTogetherApplication.class, args);
    }
}

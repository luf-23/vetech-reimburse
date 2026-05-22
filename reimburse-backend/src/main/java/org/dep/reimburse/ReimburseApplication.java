package org.dep.reimburse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.dep.reimburse.mapper")
public class ReimburseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReimburseApplication.class, args);
    }
}

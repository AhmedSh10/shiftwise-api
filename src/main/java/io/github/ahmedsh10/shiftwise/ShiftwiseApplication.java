package io.github.ahmedsh10.shiftwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShiftwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiftwiseApplication.class, args);
    }
}

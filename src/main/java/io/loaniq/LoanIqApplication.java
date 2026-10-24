package io.loaniq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LoanIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanIqApplication.class, args);
    }
}

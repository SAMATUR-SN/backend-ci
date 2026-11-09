package sn.samatur.samatur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SamaturApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamaturApplication.class, args);
	}

}

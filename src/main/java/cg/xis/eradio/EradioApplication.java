package cg.xis.eradio;

import cg.xis.eradio.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EradioApplication {

	public static void main(String[] args) {
		// Load .env file before Spring Boot starts to ensure placeholders are resolved
		DotEnvConfig.loadDotEnv();
		SpringApplication.run(EradioApplication.class, args);
	}

}

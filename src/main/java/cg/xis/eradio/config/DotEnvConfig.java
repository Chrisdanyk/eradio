package cg.xis.eradio.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotEnvConfig {

    static {
        loadDotEnv();
    }

    /**
     * Loads .env file and sets system properties before Spring Boot starts.
     * This must be called early, before SpringApplication.run().
     */
    public static void loadDotEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            });
        } catch (Exception e) {
            // .env file is optional, so we ignore errors
            // This is normal if running without a .env file
        }
    }
}

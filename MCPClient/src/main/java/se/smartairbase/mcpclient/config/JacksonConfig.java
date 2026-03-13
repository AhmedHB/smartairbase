package se.smartairbase.mcpclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * Registers JSON serialization defaults used by the HTTP layer.
 */
public class JacksonConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

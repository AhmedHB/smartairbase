package se.smartairbase.mcpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {
    private static final Logger LOG =
            LoggerFactory.getLogger(McpServerApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        LOG.info("MCP Server started");
    }
}

package se.smartairbase.mcpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * Boots the HTTP client facade that sits in front of the MCP server.
 */
public class McpClientApplication {
    private static final Logger LOG =
            LoggerFactory.getLogger(McpClientApplication.class);


    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
        LOG.info("MCP client application started");
    }

}

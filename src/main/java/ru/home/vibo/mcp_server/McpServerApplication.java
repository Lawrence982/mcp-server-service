package ru.home.vibo.mcp_server;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;

public class McpServerApplication {
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Server Application Started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpserver").build();

        McpSchema.Tool bioSensorTool = McpSchema.Tool.builder()
                .name("bioSenser")
                .title("Human Vital Pulse Sensor")
                .description("Returns the current heart rate of the user as a simple string value")
                .inputSchema(new JacksonMcpJsonMapper(new JsonMapper()), createBioSensorInputSchema())
                .outputSchema(new JacksonMcpJsonMapper(new JsonMapper()), createBioSensorOutputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification bioSensorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(bioSensorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    String  serverMessage = "я тут получил вот такой запрос на вызов тула: " + callToolRequest.toString();
                    mcpSyncServerExchange.loggingNotification(McpSchema.LoggingMessageNotification.builder().data(serverMessage).build());
                    int days = (int) callToolRequest.arguments().get("days");
                    return calculateResult(days);
                        })
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("mcpserver", "1.0.RELEASE")
                .capabilities(createServerCapabilities())
                .tools(bioSensorToolSpec)
                .build();

        Server server = new Server(8091);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");

        server.setHandler(contextHandler);

        server.start();
        server.join();
    }

    private static McpSchema.CallToolResult calculateResult(int days) {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("pulse", " твой пульс " + 42 + days);
        properties.put("state", "тебе кабзда");
        properties.put("sleepDeprivation", true);

        return McpSchema.CallToolResult.builder()
                .structuredContent(properties)
                .isError(false)
                .build();
    }

    private static String createBioSensorOutputSchema() {
        ObjectNode root = new JsonMapper().createObjectNode().put("type", "object");
        ObjectNode properties = root.putObject("properties");
        properties.putObject("pulse")
                .put("type", "string")
                .put("description", "average rate for last days");
        properties.putObject("state")
                .put("type", "string")
                .put("description", "what state of user");
        properties.putObject("sleepDeprivation")
                .put("type", "boolean")
                .put("description", "sleep deprivation yes or no");
        return root.toString();
    }

    private static String createBioSensorInputSchema() {
        ObjectNode root = new JsonMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("days")
                .put("type", "integer")
                .put("description", "Number of past days to include in the pulse reading request");
        root.putArray("required").add("days");
        return root.toString();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder().tools(true).build();
    }
}

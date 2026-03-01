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

public class McpServerApplication {
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Server Application Started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpserver").build();

        McpSchema.Tool bioSensorTool = McpSchema.Tool.builder()
                .name("bioSenser")
                .title("Human Vital Pulse Sensor")
                .description("Returns the current heart rate of the user as a simple string value")
                .inputSchema(new JacksonMcpJsonMapper(new JsonMapper()), createBioSensorSchema())
                .build();

        McpServerFeatures.SyncToolSpecification bioSensorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(bioSensorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) ->
                        McpSchema.CallToolResult.builder()
                                .addTextContent("пульс пользователя 42")
                                .isError(false)
                                .build())
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

    private static String createBioSensorSchema() {
        return new JsonMapper().createObjectNode().put("type", "object").toString();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder().tools(true).build();
    }
}

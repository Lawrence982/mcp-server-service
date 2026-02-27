package ru.home.vibo.mcp_server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class McpServerApplication {
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Server Application Started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpserver").build();

        McpServer.sync(transportProvider)
                .serverInfo("mcpserver", "1.0.RELEASE")
                .capabilities(createServerCapabilities()).build();

        Server server = new Server(8091);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");

        server.setHandler(contextHandler);

        server.start();
        server.join();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder().build();
    }
}

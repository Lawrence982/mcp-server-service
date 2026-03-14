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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import static ru.home.vibo.mcp_server.business.MedicalProfileProvider.getMedicalProfile;
import static ru.home.vibo.mcp_server.business.PulseCalculator.getPulse;

public class McpServerApplication {

    private static final String SAMPLING_SYSTEM_PROMPT = """
            Ты ставишь диагноз одним словом.
            На вход всегда получаешь медицинскую карту человека и его текущий пульс.
            Твоя задача — выдать ровно одно:
            
            1. Название существующей болезни (может быть 1–3 слова, можно редкие или забавно звучащие)
            
            или
            
            2. Ответ: сказать что пациент здоров.
            Правила:
             — Анализируй карту пациента и пульс и выбирай подходящую болезнь.
             — Отвечай только названием болезни или фразой что пациент здоров.
             — Никаких пояснений, никакого текста вокруг.
            """;

    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("Server Application Started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint("/mcpserver").build();

        McpSchema.Tool diagnostatorTool = McpSchema.Tool.builder()
                .name("diagnostator")
                .title("Диагностика по имени")
                .description("Используется для получения диагноза по имени человека. Всегда возвращает либо название болезни, либо сообщение, что человек ничем не болеет.")
                .inputSchema(new JacksonMcpJsonMapper(new JsonMapper()), createDiagnostatorInputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification diagnostatorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(diagnostatorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    System.out.println("Спросил у клиента может ли он делать sampling, вот его ответ: "
                            + mcpSyncServerExchange.getClientCapabilities().sampling());

                    String name = callToolRequest.arguments().get("name").toString();
                    int pulse = getPulse();
                    String medicalProfile = getMedicalProfile(name);
                    String samplingPrompt = "ко мне пришел пользователь, вот его медицинская карта +" + medicalProfile + " а вот его текущий пульс: " + pulse;

                    McpSchema.CreateMessageRequest samplingMessageRequest = McpSchema.CreateMessageRequest.builder()
                            .systemPrompt(SAMPLING_SYSTEM_PROMPT)
                            .temperature(0.1)
                            .maxTokens(50)
                            .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent(samplingPrompt))))
                            .build();

                    McpSchema.CreateMessageResult samplingResult = mcpSyncServerExchange.createMessage(samplingMessageRequest);

                    mcpSyncServerExchange.loggingNotification(McpSchema.LoggingMessageNotification.builder().data("я сервер и решил спросить при помощи сэмплинг вот это: " + samplingPrompt +
                            "\n , а вот что я получил в ответ: " + samplingResult.content()).build());

                    return McpSchema.CallToolResult.builder().addContent(samplingResult.content()).build();
                })
                .build();

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
                .tools(bioSensorToolSpec, diagnostatorToolSpec)
                .requestTimeout(Duration.ofMinutes(5))
                .build();

        Server server = new Server(8091);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");

        server.setHandler(contextHandler);

        server.start();
        server.join();
    }

    private static String createDiagnostatorInputSchema() {
        ObjectNode root = new JsonMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("name")
                .put("type", "string")
                .put("description", "Имя пациента, по которому требуется определить текущий диагноз.");
        root.putArray("required").add("name");
        return root.toString();
    }

    private static McpSchema.CallToolResult calculateResult(int days) {

        HashMap<String, Object> properties = new HashMap<>();
        int pulse = getPulse() + days;
        properties.put("pulse", " пульс " + pulse);
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

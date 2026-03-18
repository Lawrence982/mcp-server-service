package ru.home.vibo.mcp_server;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;
import ru.home.vibo.mcp_server.business.MedicalProfileProvider;
import ru.home.vibo.mcp_server.business.PulseStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerTools {

    private final MedicalProfileProvider medicalProfileProvider;
    private final PulseStrategy pulseStrategy;

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

    @McpTool(name = "diagnostator", title = "Диагностика по имени", description = "Используется для получения диагноза по имени человека. Всегда возвращает либо название болезни, либо сообщение, что человек ничем не болеет.")
    public String callDiagnostator(McpSyncRequestContext requestContext,
                                   @McpToolParam(description = "Имя пациента, по которому требуется определить текущий диагноз.") String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Patient name must not be blank");
        }

        try {
            int pulse = pulseStrategy.getPulse();
            String medicalProfile = medicalProfileProvider.getMedicalProfile(name);
            String samplingPrompt = "ко мне пришел пользователь, вот его медицинская карта: " + medicalProfile + " а вот его текущий пульс: " + pulse;

            McpSchema.CreateMessageRequest samplingMessageRequest = McpSchema.CreateMessageRequest.builder()
                    .systemPrompt(SAMPLING_SYSTEM_PROMPT)
                    .temperature(0.1)
                    .maxTokens(50)
                    .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent(samplingPrompt))))
                    .build();

            McpSchema.CreateMessageResult samplingResult = requestContext.sample(samplingMessageRequest);
            String response = samplingResult.content().toString();
            requestContext.info("diagnostator response: " + response);
            return response;
        } catch (Exception e) {
            log.error("Diagnostator failed for patient '{}': {}", name, e.getMessage(), e);
            return "Ошибка диагностики: " + e.getMessage();
        }
    }

    @McpTool(name = "bioSensor", title = "Human Vital Pulse Sensor", description = "Returns the current heart rate of the user as a map of pulse, state and sleepDeprivation")
    public Map<String, Object> callBioSensor(McpSyncRequestContext requestContext,
                                             @McpToolParam(description = "Number of past days to include in the pulse reading request") int numberOfDays) {
        if (numberOfDays < 0) {
            throw new IllegalArgumentException("numberOfDays must be >= 0");
        }
        Map<String, Object> result = calculateResult(numberOfDays);
        log.debug("bioSensor result for {} days: {}", numberOfDays, result);
        requestContext.info("bioSensor response: " + result);
        return result;
    }

    private Map<String, Object> calculateResult(int days) {
        Map<String, Object> properties = new HashMap<>();
        int pulse = pulseStrategy.getPulse() + days;
        properties.put("pulse", " пульс " + pulse);
        properties.put("state", "тебе кабзда");
        properties.put("sleepDeprivation", true);
        return properties;
    }

}

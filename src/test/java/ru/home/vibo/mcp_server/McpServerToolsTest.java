package ru.home.vibo.mcp_server;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import ru.home.vibo.mcp_server.business.MedicalProfileProvider;
import ru.home.vibo.mcp_server.business.PulseStrategy;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpServerToolsTest {

    @Mock
    private MedicalProfileProvider medicalProfileProvider;

    @Mock
    private PulseStrategy pulseStrategy;

    @Mock
    private McpSyncRequestContext requestContext;

    @InjectMocks
    private McpServerTools tools;

    // ==================== callDiagnostator ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void callDiagnostator_blankName_throwsIllegalArgumentException(String name) {
        assertThatThrownBy(() -> tools.callDiagnostator(requestContext, name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Patient name must not be blank");
        verifyNoInteractions(pulseStrategy, medicalProfileProvider, requestContext);
    }

    @Test
    void callDiagnostator_happyPath_returnsSamplingContent() {
        when(pulseStrategy.getPulse()).thenReturn(42);
        when(medicalProfileProvider.getMedicalProfile("Фейлор")).thenReturn("тест профиль");
        McpSchema.CreateMessageResult result = buildSamplingResult("диагноз");
        when(requestContext.sample(any(McpSchema.CreateMessageRequest.class))).thenReturn(result);

        String response = tools.callDiagnostator(requestContext, "Фейлор");

        assertThat(response).isEqualTo(result.content().toString());
        verify(pulseStrategy).getPulse();
        verify(medicalProfileProvider).getMedicalProfile("Фейлор");
        verify(requestContext).sample(any(McpSchema.CreateMessageRequest.class));
        verify(requestContext).info(any());
    }

    @Test
    void callDiagnostator_verifiesCreateMessageRequestParameters() {
        when(pulseStrategy.getPulse()).thenReturn(42);
        when(medicalProfileProvider.getMedicalProfile("Лиссандра")).thenReturn("профиль Лиссандры");
        when(requestContext.sample(any(McpSchema.CreateMessageRequest.class))).thenReturn(buildSamplingResult("здоров"));

        tools.callDiagnostator(requestContext, "Лиссандра");

        ArgumentCaptor<McpSchema.CreateMessageRequest> captor =
                ArgumentCaptor.forClass(McpSchema.CreateMessageRequest.class);
        verify(requestContext).sample(captor.capture());

        McpSchema.CreateMessageRequest req = captor.getValue();
        assertThat(req.maxTokens()).isEqualTo(50);
        assertThat(req.temperature()).isEqualTo(0.1);
        assertThat(req.messages()).hasSize(1);
        assertThat(req.messages().get(0).role()).isEqualTo(McpSchema.Role.USER);
        assertThat(req.systemPrompt()).contains("Ты ставишь диагноз");
    }

    @Test
    void callDiagnostator_promptContainsPulseAndProfile() {
        int pulse = 77;
        String profile = "профиль данные";
        when(pulseStrategy.getPulse()).thenReturn(pulse);
        when(medicalProfileProvider.getMedicalProfile("Эт Рус")).thenReturn(profile);
        when(requestContext.sample(any(McpSchema.CreateMessageRequest.class))).thenReturn(buildSamplingResult("болезнь"));

        tools.callDiagnostator(requestContext, "Эт Рус");

        ArgumentCaptor<McpSchema.CreateMessageRequest> captor =
                ArgumentCaptor.forClass(McpSchema.CreateMessageRequest.class);
        verify(requestContext).sample(captor.capture());

        McpSchema.TextContent textContent =
                (McpSchema.TextContent) captor.getValue().messages().get(0).content();
        assertThat(textContent.text()).contains(String.valueOf(pulse));
        assertThat(textContent.text()).contains(profile);
    }

    @Test
    void callDiagnostator_getMedicalProfileThrows_returnsErrorMessage() {
        when(pulseStrategy.getPulse()).thenReturn(50);
        when(medicalProfileProvider.getMedicalProfile("Незнакомец"))
                .thenThrow(new IllegalArgumentException("Unknown patient: Незнакомец"));

        String response = tools.callDiagnostator(requestContext, "Незнакомец");

        assertThat(response).startsWith("Ошибка диагностики: Unknown patient");
        verify(requestContext, never()).sample(any(McpSchema.CreateMessageRequest.class));
    }

    @Test
    void callDiagnostator_sampleThrows_returnsErrorMessage() {
        when(pulseStrategy.getPulse()).thenReturn(50);
        when(medicalProfileProvider.getMedicalProfile("Фейлор")).thenReturn("профиль");
        when(requestContext.sample(any(McpSchema.CreateMessageRequest.class))).thenThrow(new RuntimeException("network error"));

        String response = tools.callDiagnostator(requestContext, "Фейлор");

        assertThat(response).startsWith("Ошибка диагностики:");
    }

    // ==================== callBioSensor ====================

    @Test
    void callBioSensor_negativeDays_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> tools.callBioSensor(requestContext, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("numberOfDays must be >= 0");
    }

    @Test
    void callBioSensor_zeroDays_returnsMapWithRequiredKeys() {
        when(pulseStrategy.getPulse()).thenReturn(50);

        Map<String, Object> result = tools.callBioSensor(requestContext, 0);

        assertThat(result).containsKeys("pulse", "state", "sleepDeprivation");
        assertThat(result.get("state")).isEqualTo("тебе кабзда");
        assertThat(result.get("sleepDeprivation")).isEqualTo(true);
    }

    @Test
    void callBioSensor_pulseIncludesDays() {
        when(pulseStrategy.getPulse()).thenReturn(30);

        Map<String, Object> result = tools.callBioSensor(requestContext, 10);

        assertThat(result.get("pulse").toString()).contains("40");
    }

    @Test
    void callBioSensor_sleepDeprivationAlwaysTrue() {
        when(pulseStrategy.getPulse()).thenReturn(1);

        Map<String, Object> result = tools.callBioSensor(requestContext, 5);

        assertThat(result.get("sleepDeprivation")).isEqualTo(true);
    }

    @Test
    void callBioSensor_infoCalledOnce() {
        when(pulseStrategy.getPulse()).thenReturn(42);

        tools.callBioSensor(requestContext, 0);

        verify(requestContext, times(1)).info(any());
    }

    @Test
    void callBioSensor_getPulseCalledOnce() {
        when(pulseStrategy.getPulse()).thenReturn(42);

        tools.callBioSensor(requestContext, 0);

        verify(pulseStrategy, times(1)).getPulse();
    }

    // ==================== helpers ====================

    private McpSchema.CreateMessageResult buildSamplingResult(String text) {
        return McpSchema.CreateMessageResult.builder()
                .role(McpSchema.Role.ASSISTANT)
                .content(new McpSchema.TextContent(text))
                .model("test-model")
                .stopReason(McpSchema.CreateMessageResult.StopReason.END_TURN)
                .build();
    }
}

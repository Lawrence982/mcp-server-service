package ru.home.vibo.mcp_server.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicalProfileProviderTest {

    private final MedicalProfileProvider provider = new MedicalProfileProvider();

    @ParameterizedTest
    @ValueSource(strings = {"Фейлор", "Эт Рус", "Лиссандра"})
    void getMedicalProfile_knownPatient_returnsNonBlankProfile(String name) {
        assertThat(provider.getMedicalProfile(name)).isNotBlank();
    }

    @Test
    void getMedicalProfile_faylor_containsKeywords() {
        String profile = provider.getMedicalProfile("Фейлор");
        assertThat(profile).containsIgnoringCase("золот");
    }

    @Test
    void getMedicalProfile_etRus_containsKeywords() {
        String profile = provider.getMedicalProfile("Эт Рус");
        assertThat(profile).containsIgnoringCase("пульс");
    }

    @Test
    void getMedicalProfile_lissandra_containsKeywords() {
        String profile = provider.getMedicalProfile("Лиссандра");
        assertThat(profile).containsIgnoringCase("пульс");
    }

    @Test
    void getMedicalProfile_unknownPatient_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> provider.getMedicalProfile("Незнакомец"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown patient");
    }

    @Test
    void getMedicalProfile_emptyString_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> provider.getMedicalProfile(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown patient");
    }

    @Test
    void getMedicalProfile_null_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> provider.getMedicalProfile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }
}

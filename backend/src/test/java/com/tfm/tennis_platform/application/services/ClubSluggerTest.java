package com.tfm.tennis_platform.application.services;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClubSluggerTest {

    private final ClubSlugger slugger = new ClubSlugger();

    @Test
    void slugify_normalizes_accents_and_spaces() {
        assertThat(slugger.slugify("Club de Tenis Peñagolosa")).isEqualTo("club-de-tenis-penagolosa");
    }

    @Test
    void slugify_removes_leading_and_trailing_separators() {
        assertThat(slugger.slugify("  ---Club Norte!!!  ")).isEqualTo("club-norte");
    }

    @Test
    void slugify_returns_empty_string_for_null() {
        assertThat(slugger.slugify(null)).isEmpty();
    }

    @Test
    void slugify_limits_length() {
        String result = slugger.slugify("a".repeat(200));

        assertThat(result).hasSize(160);
    }

    @Test
    void generate_unique_uses_base_when_available() {
        assertThat(slugger.generateUnique("Club Norte", Set.of()::contains)).isEqualTo("club-norte");
    }

    @Test
    void generate_unique_appends_numeric_suffix_when_taken() {
        Set<String> existing = Set.of("club-norte", "club-norte-2");

        assertThat(slugger.generateUnique("Club Norte", existing::contains)).isEqualTo("club-norte-3");
    }

    @Test
    void generate_unique_falls_back_to_club_when_name_has_no_slug_characters() {
        assertThat(slugger.generateUnique("***", Set.of()::contains)).isEqualTo("club");
    }
}

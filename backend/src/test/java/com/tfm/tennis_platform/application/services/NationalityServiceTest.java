package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.NationalityOutput;
import org.junit.jupiter.api.Test;

import java.text.Collator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NationalityServiceTest {
    private final NationalityService service = new NationalityService();

    @Test
    void shouldReturnIso3NationalitiesSortedBySpanishName() {
        List<NationalityOutput> nationalities = service.getAll();
        Set<String> codes = new HashSet<>();
        Collator collator = Collator.getInstance(Locale.forLanguageTag("es-ES"));

        assertTrue(nationalities.stream().anyMatch(nationality -> nationality.code().equals("ESP")));
        assertTrue(nationalities.stream().allMatch(nationality -> nationality.code().length() == 3));
        assertTrue(nationalities.stream().allMatch(nationality -> codes.add(nationality.code())));

        List<NationalityOutput> sortedNationalities = nationalities.stream()
                .sorted((left, right) -> {
                    int nameComparison = collator.compare(left.name(), right.name());
                    return nameComparison != 0 ? nameComparison : left.code().compareTo(right.code());
                })
                .toList();

        assertEquals(sortedNationalities, nationalities);
    }
}

package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.commands.NationalityOutput;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;

@Service
public class NationalityService {
    private static final Locale DISPLAY_LOCALE = Locale.forLanguageTag("es-ES");

    public List<NationalityOutput> getAll() {
        Collator collator = Collator.getInstance(DISPLAY_LOCALE);

        return Arrays.stream(Locale.getISOCountries())
                .map(this::toNationality)
                .filter(Objects::nonNull)
                .distinct()
                .sorted((left, right) -> {
                    int nameComparison = collator.compare(left.name(), right.name());
                    return nameComparison != 0 ? nameComparison : left.code().compareTo(right.code());
                })
                .toList();
    }

    private NationalityOutput toNationality(String countryCode) {
        Locale countryLocale = Locale.of("", countryCode);

        try {
            String code = countryLocale.getISO3Country();
            String name = countryLocale.getDisplayCountry(DISPLAY_LOCALE);

            if (code == null || code.length() != 3 || name == null || name.isBlank()) {
                return null;
            }

            return new NationalityOutput(code.toUpperCase(Locale.ROOT), name);
        } catch (MissingResourceException exception) {
            return null;
        }
    }
}

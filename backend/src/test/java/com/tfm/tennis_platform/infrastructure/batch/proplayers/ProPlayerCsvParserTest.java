package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProPlayerCsvParserTest {

    private final ProPlayerCsvParser parser = new ProPlayerCsvParser();

    @Test
    void parsesQuotedNamesDatesAndNullableLicenses() {
        String csv = """
            PUNTOS,LICENCIA,NAME,POSICION,POSICION TERR,POSICION PROVINCIAL,POSICION CLUB,POSICION EDAD,EDAD,NOMBRE CLUB,NOMBRE PROVINCIAL,NOMBRE TERRITORIAL,NOMBRE CATEGORIA,PUNTOS OTORGA,FECHA NACIMIENTO,GENDER
            527400,null,"ZHENG, QINWEN",1,1,1,1,1,Absoluta,"CLUB TENNIS CORNELLA, BARCELONA",BARCELONA,FEDERACIO CATALANA DE TENNIS,1ª Categoría,200,08/10/2002,FEMALE
            """;

        List<ProPlayerCsvRow> rows = parser.parse(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, rows.size());
        ProPlayerCsvRow row = rows.getFirst();
        assertEquals(527400, row.points());
        assertNull(row.license());
        assertEquals("ZHENG, QINWEN", row.name());
        assertEquals("CLUB TENNIS CORNELLA, BARCELONA", row.clubName());
        assertEquals(LocalDate.of(2002, 10, 8), row.birthDate());
        assertEquals("FEMALE", row.gender());
    }

    @Test
    void usesFallbackGenderWhenGenderColumnIsMissing() {
        String csv = """
            PUNTOS,LICENCIA,NAME,POSICION,POSICION TERR,POSICION PROVINCIAL,POSICION CLUB,POSICION EDAD,EDAD,NOMBRE CLUB,NOMBRE PROVINCIAL,NOMBRE TERRITORIAL,NOMBRE CATEGORIA,PUNTOS OTORGA,FECHA NACIMIENTO
            2032500,4617687,"ALCARAZ GARFIA, CARLOS",1,1,1,1,1,Absoluta,FEDERACION DE TENIS REGION DE MURCIA,MURCIA,FEDERACION DE TENIS DE LA REGION DE MURCIA,1ª Categoría,200,05/05/2003
            """;

        List<ProPlayerCsvRow> rows = parser.parse(csv.getBytes(StandardCharsets.UTF_8), "MALE");

        assertEquals(1, rows.size());
        assertEquals("MALE", rows.getFirst().gender());
    }

    @Test
    void parsesBundledSeedFiles() throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(new ClassPathResource("db/seed/pro_players_male.csv").exists(), "Male seed file not available, skipping");
        org.junit.jupiter.api.Assumptions.assumeTrue(new ClassPathResource("db/seed/pro_players_female.csv").exists(), "Female seed file not available, skipping");

        List<ProPlayerCsvRow> maleRows = parser.parse(new ClassPathResource("db/seed/pro_players_male.csv").getContentAsByteArray());
        List<ProPlayerCsvRow> femaleRows = parser.parse(new ClassPathResource("db/seed/pro_players_female.csv").getContentAsByteArray());

        assertTrue(maleRows.size() > 20_000);
        assertTrue(femaleRows.size() > 7_000);
    }
}

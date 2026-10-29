package com.tfm.tennis_platform.infrastructure.persistence.mapper;

import com.tfm.tennis_platform.domain.models.ProPlayer;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ProPlayerEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProPlayerMapperTest {

    private final ProPlayerMapper mapper = new ProPlayerMapper();

    @Test
    void should_return_null_when_entity_is_null() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void should_map_basic_fields() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .license("L001")
                .name("NADAL, RAFAEL")
                .rankingPosition(1)
                .points(5000)
                .ageCategory("30+")
                .clubName("RC Madrid")
                .birthDate(LocalDate.of(1986, 6, 3))
                .gender("M")
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getLicense()).isEqualTo("L001");
        assertThat(result.getFullName()).isEqualTo("NADAL, RAFAEL");
        assertThat(result.getFirstName()).isEqualTo("RAFAEL");
        assertThat(result.getLastName()).isEqualTo("NADAL");
        assertThat(result.getRankingPosition()).isEqualTo(1);
        assertThat(result.getPoints()).isEqualTo(5000);
        assertThat(result.getAgeCategory()).isEqualTo("30+");
        assertThat(result.getClubName()).isEqualTo("RC Madrid");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1986, 6, 3));
        assertThat(result.getGender()).isEqualTo("M");
    }

    @Test
    void should_parse_name_without_comma() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .name("NADAL")
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getFirstName()).isEqualTo("NADAL");
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void should_handle_null_name() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .name(null)
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getFullName()).isNull();
        assertThat(result.getFirstName()).isEmpty();
        assertThat(result.getLastName()).isNull();
    }

    @Test
    void should_handle_blank_name() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .name("   ")
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getFullName()).isNull();
        assertThat(result.getFirstName()).isEmpty();
    }

    @Test
    void should_normalize_null_fields() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .name("A, B")
                .license(null)
                .ageCategory(null)
                .clubName(null)
                .gender(null)
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getLicense()).isNull();
        assertThat(result.getAgeCategory()).isNull();
        assertThat(result.getClubName()).isNull();
        assertThat(result.getGender()).isNull();
    }

    @Test
    void should_trim_whitespace_from_fields() {
        ProPlayerEntity entity = ProPlayerEntity.builder()
                .id(1)
                .name("  LAST, FIRST  ")
                .license("  L001  ")
                .build();

        ProPlayer result = mapper.toDomain(entity);

        assertThat(result.getFullName()).isEqualTo("LAST, FIRST");
        assertThat(result.getLicense()).isEqualTo("L001");
        assertThat(result.getFirstName()).isEqualTo("FIRST");
        assertThat(result.getLastName()).isEqualTo("LAST");
    }
}

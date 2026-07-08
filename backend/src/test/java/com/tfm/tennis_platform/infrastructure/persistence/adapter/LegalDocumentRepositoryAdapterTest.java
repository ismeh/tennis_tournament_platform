package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.LegalDocumentVersionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.LegalDocumentVersionMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaLegalDocumentVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegalDocumentRepositoryAdapter")
class LegalDocumentRepositoryAdapterTest {

    @Mock
    private JpaLegalDocumentVersionRepository jpaRepository;

    @Mock
    private LegalDocumentVersionMapper mapper;

    @InjectMocks
    private LegalDocumentRepositoryAdapter adapter;

    private LegalDocumentVersion buildDomain() {
        return LegalDocumentVersion.builder()
                .id(1L)
                .documentType(DocumentType.PRIVACY_POLICY)
                .version("1.0")
                .contentSnapshot("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    private LegalDocumentVersionEntity buildEntity() {
        return LegalDocumentVersionEntity.builder()
                .id(1L)
                .documentType(DocumentType.PRIVACY_POLICY)
                .version("1.0")
                .contentSnapshot("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should map to entity, save, and map back to domain")
        void should_save_and_map_back() {
            LegalDocumentVersion domain = buildDomain();
            LegalDocumentVersionEntity entity = buildEntity();
            LegalDocumentVersionEntity savedEntity = LegalDocumentVersionEntity.builder()
                    .id(1L)
                    .documentType(DocumentType.PRIVACY_POLICY)
                    .version("1.0")
                    .contentSnapshot("{\"key\":\"value\"}")
                    .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                    .build();
            LegalDocumentVersion mappedDomain = buildDomain();

            when(mapper.toEntity(domain)).thenReturn(entity);
            when(jpaRepository.save(entity)).thenReturn(savedEntity);
            when(mapper.toDomain(savedEntity)).thenReturn(mappedDomain);

            LegalDocumentVersion result = adapter.save(domain);

            assertThat(result).isEqualTo(mappedDomain);
            verify(mapper).toEntity(domain);
            verify(jpaRepository).save(entity);
            verify(mapper).toDomain(savedEntity);
        }
    }

    @Nested
    @DisplayName("findLatestByDocumentType")
    class FindLatestByDocumentTypeTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            LegalDocumentVersionEntity entity = buildEntity();
            LegalDocumentVersion domain = buildDomain();

            when(jpaRepository.findLatestByDocumentType(DocumentType.PRIVACY_POLICY))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<LegalDocumentVersion> result =
                    adapter.findLatestByDocumentType(DocumentType.PRIVACY_POLICY);

            assertThat(result).contains(domain);
            verify(jpaRepository).findLatestByDocumentType(DocumentType.PRIVACY_POLICY);
        }

        @Test
        @DisplayName("should return empty when not found")
        void should_return_empty_when_not_found() {
            when(jpaRepository.findLatestByDocumentType(DocumentType.TERMS_CONDITIONS))
                    .thenReturn(Optional.empty());

            Optional<LegalDocumentVersion> result =
                    adapter.findLatestByDocumentType(DocumentType.TERMS_CONDITIONS);

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("findByDocumentTypeAndVersion")
    class FindByDocumentTypeAndVersionTests {

        @Test
        @DisplayName("should return mapped domain when found")
        void should_return_mapped_domain() {
            LegalDocumentVersionEntity entity = buildEntity();
            LegalDocumentVersion domain = buildDomain();

            when(jpaRepository.findByDocumentTypeAndVersion(DocumentType.PRIVACY_POLICY, "1.0"))
                    .thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<LegalDocumentVersion> result =
                    adapter.findByDocumentTypeAndVersion(DocumentType.PRIVACY_POLICY, "1.0");

            assertThat(result).contains(domain);
            verify(jpaRepository).findByDocumentTypeAndVersion(DocumentType.PRIVACY_POLICY, "1.0");
        }

        @Test
        @DisplayName("should return empty when version not found")
        void should_return_empty_when_version_not_found() {
            when(jpaRepository.findByDocumentTypeAndVersion(DocumentType.PRIVACY_POLICY, "9.9"))
                    .thenReturn(Optional.empty());

            Optional<LegalDocumentVersion> result =
                    adapter.findByDocumentTypeAndVersion(DocumentType.PRIVACY_POLICY, "9.9");

            assertThat(result).isEmpty();
            verifyNoInteractions(mapper);
        }
    }
}

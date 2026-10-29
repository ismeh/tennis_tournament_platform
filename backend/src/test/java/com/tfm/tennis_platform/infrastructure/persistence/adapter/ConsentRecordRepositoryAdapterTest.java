package com.tfm.tennis_platform.infrastructure.persistence.adapter;

import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.domain.models.enums.ConsentAction;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.infrastructure.persistence.entity.ConsentRecordEntity;
import com.tfm.tennis_platform.infrastructure.persistence.entity.LegalDocumentVersionEntity;
import com.tfm.tennis_platform.infrastructure.persistence.mapper.ConsentRecordMapper;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaConsentRecordRepository;
import com.tfm.tennis_platform.infrastructure.persistence.repository.JpaLegalDocumentVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentRecordRepositoryAdapterTest {

    @Mock
    private JpaConsentRecordRepository jpaRepository;
    @Mock
    private JpaLegalDocumentVersionRepository legalDocumentRepository;
    @Mock
    private ConsentRecordMapper mapper;
    @InjectMocks
    private ConsentRecordRepositoryAdapter adapter;

    @Test
    void should_save_consent_record() {
        UUID userId = UUID.randomUUID();
        Long documentVersionId = 1L;
        ConsentRecord domain = ConsentRecord.builder()
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .documentVersionId(documentVersionId)
                .build();

        LegalDocumentVersionEntity docVersion = LegalDocumentVersionEntity.builder()
                .id(documentVersionId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .version("1.0")
                .build();
        ConsentRecordEntity entity = ConsentRecordEntity.builder()
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .build();
        ConsentRecordEntity savedEntity = ConsentRecordEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .documentVersion(docVersion)
                .build();
        ConsentRecord mapped = ConsentRecord.builder()
                .id(savedEntity.getId())
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .documentVersionId(documentVersionId)
                .createdAt(LocalDateTime.now())
                .build();

        when(legalDocumentRepository.findById(documentVersionId)).thenReturn(Optional.of(docVersion));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(mapped);

        ConsentRecord result = adapter.save(domain);

        assertThat(result).isEqualTo(mapped);
        verify(legalDocumentRepository).findById(documentVersionId);
        verify(mapper).toEntity(domain);
        verify(jpaRepository).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void should_throw_when_document_version_not_found() {
        ConsentRecord record = ConsentRecord.builder().documentVersionId(999L).build();
        when(legalDocumentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(record))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void should_find_by_user_id() {
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ConsentRecordEntity e1 = ConsentRecordEntity.builder().id(id1).userId(userId).documentType(DocumentType.PRIVACY_POLICY).action(ConsentAction.GRANTED).build();
        ConsentRecordEntity e2 = ConsentRecordEntity.builder().id(id2).userId(userId).documentType(DocumentType.TERMS_CONDITIONS).action(ConsentAction.GRANTED).build();
        ConsentRecord d1 = ConsentRecord.builder().id(id1).userId(userId).documentType(DocumentType.PRIVACY_POLICY).action(ConsentAction.GRANTED).build();
        ConsentRecord d2 = ConsentRecord.builder().id(id2).userId(userId).documentType(DocumentType.TERMS_CONDITIONS).action(ConsentAction.GRANTED).build();

        when(jpaRepository.findByUserId(userId)).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<ConsentRecord> result = adapter.findByUserId(userId);

        assertThat(result).hasSize(2).containsExactly(d1, d2);
        verify(jpaRepository).findByUserId(userId);
    }

    @Test
    void should_return_empty_list_when_no_records_for_user() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findByUserId(userId)).thenReturn(List.of());

        List<ConsentRecord> result = adapter.findByUserId(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void should_find_latest_by_user_id_and_document_type() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        ConsentRecordEntity entity = ConsentRecordEntity.builder()
                .id(id)
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .build();
        ConsentRecord domain = ConsentRecord.builder()
                .id(id)
                .userId(userId)
                .documentType(DocumentType.PRIVACY_POLICY)
                .action(ConsentAction.GRANTED)
                .build();

        when(jpaRepository.findLatestByUserIdAndDocumentType(userId, DocumentType.PRIVACY_POLICY))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<ConsentRecord> result = adapter.findLatestByUserIdAndDocumentType(userId, DocumentType.PRIVACY_POLICY);

        assertThat(result).contains(domain);
    }

    @Test
    void should_return_empty_when_no_latest_record() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findLatestByUserIdAndDocumentType(userId, DocumentType.PRIVACY_POLICY))
                .thenReturn(Optional.empty());

        assertThat(adapter.findLatestByUserIdAndDocumentType(userId, DocumentType.PRIVACY_POLICY)).isEmpty();
    }
}

package com.tfm.tennis_platform.infrastructure.persistence.repository;

import com.tfm.tennis_platform.infrastructure.persistence.entity.InscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaInscriptionRepository extends JpaRepository<InscriptionEntity, UUID> {
    List<InscriptionEntity> findByEvent_Tournament_Id(UUID tournamentId);
    List<InscriptionEntity> findByEvent_Id(UUID eventId);
    List<InscriptionEntity> findByParticipant_Id(UUID participantId);
    boolean existsByEvent_IdAndParticipant_Id(UUID eventId, UUID participantId);
}

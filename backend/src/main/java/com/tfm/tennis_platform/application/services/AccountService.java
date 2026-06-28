package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.UnauthorizedException;
import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.models.enums.ConsentAction;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountExportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final MemberRepository memberRepository;
    private final PersonRepository personRepository;
    private final TournamentRepository tournamentRepository;
    private final PasswordEncoder passwordEncoder;
    private final LegalDocumentService legalDocumentService;
    private final ConsentService consentService;

    @Transactional
    public void deleteAccount(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta."));

        if (password == null || password.isBlank()) {
            throw new InvalidArgumentException("Debes proporcionar tu contraseña para confirmar la baja.");
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new UnauthorizedException("La contraseña no es correcta.");
        }

        UUID memberId = member.getId();
        UUID personId = member.getPersonId();

        Member adminMember = memberRepository.findByRole(UserRole.ADMIN)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta de administrador para la transferencia."));

        tournamentRepository.transferTournaments(memberId, adminMember.getId());

        String anonymizedEmail = "deleted-" + memberId + "@anonymized.local";
        memberRepository.anonymize(memberId, anonymizedEmail);

        if (personId != null) {
            personRepository.anonymize(personId, "Eliminado");
        }

        log.info("Account anonymized and data transferred for member: {}", memberId);
    }

    @Transactional(readOnly = true)
    public AccountExportResponse exportAccountData(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta."));

        AccountExportResponse.AccountInfo accountInfo = new AccountExportResponse.AccountInfo(
                member.getEmail(),
                member.getRole().name(),
                member.getTier().name(),
                member.getRegisteredAt(),
                member.isPrivacyPolicyAccepted(),
                member.getPrivacyPolicyVersion(),
                member.isTermsConditionsAccepted(),
                member.getTermsConditionsVersion()
        );

        AccountExportResponse.PersonInfo personInfo = null;
        if (member.getPersonId() != null) {
            Person person = personRepository.findById(member.getPersonId()).orElse(null);
            if (person != null) {
                personInfo = new AccountExportResponse.PersonInfo(
                        person.getFirstName(),
                        person.getLastName(),
                        person.getNationality(),
                        person.getBirthDate(),
                        person.getGender(),
                        person.getTennisId()
                );
            }
        }

        List<ConsentRecord> consentHistory = consentService.getConsentHistory(member.getId());

        return new AccountExportResponse(
                accountInfo,
                personInfo,
                consentHistory.stream().map(cr -> new AccountExportResponse.ConsentInfo(
                        cr.getDocumentType().name(),
                        cr.getAction().name(),
                        cr.getCreatedAt()
                )).toList(),
                List.of()
        );
    }

    @Transactional
    public void updatePrivacyConsent(String email, boolean accepted, String version) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta."));

        if (version == null || version.isBlank()) {
            throw new InvalidArgumentException("La versión de la política de privacidad es obligatoria.");
        }

        memberRepository.updatePrivacyConsent(
                member.getId(),
                accepted,
                accepted ? LocalDateTime.now() : null,
                version
        );

        LegalDocumentVersion docVersion = legalDocumentService.getVersion(DocumentType.PRIVACY_POLICY, version);
        consentService.recordConsent(member.getId(), DocumentType.PRIVACY_POLICY,
                accepted ? ConsentAction.GRANTED : ConsentAction.REVOKED,
                docVersion.getId());

        log.info("Privacy consent updated for member {}: accepted={}, version={}", member.getId(), accepted, version);
    }

    @Transactional
    public void updateTermsConsent(String email, boolean accepted, String version) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta."));

        if (version == null || version.isBlank()) {
            throw new InvalidArgumentException("La versión de los términos y condiciones es obligatoria.");
        }

        memberRepository.updateTermsConsent(
                member.getId(),
                accepted,
                accepted ? LocalDateTime.now() : null,
                version
        );

        LegalDocumentVersion docVersion = legalDocumentService.getVersion(DocumentType.TERMS_CONDITIONS, version);
        consentService.recordConsent(member.getId(), DocumentType.TERMS_CONDITIONS,
                accepted ? ConsentAction.GRANTED : ConsentAction.REVOKED,
                docVersion.getId());

        log.info("Terms consent updated for member {}: accepted={}, version={}", member.getId(), accepted, version);
    }

    @Transactional(readOnly = true)
    public List<ConsentRecord> getConsentHistory(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("No se encontró la cuenta."));
        return consentService.getConsentHistory(member.getId());
    }
}

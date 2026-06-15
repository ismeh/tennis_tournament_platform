package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.UnauthorizedException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
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
                member.getPrivacyPolicyVersion()
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

        return new AccountExportResponse(
                accountInfo,
                personInfo,
                List.of(),
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

        log.info("Privacy consent updated for member {}: accepted={}, version={}", member.getId(), accepted, version);
    }
}

package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.UnauthorizedException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Person;
import com.tfm.tennis_platform.domain.models.LegalDocumentVersion;
import com.tfm.tennis_platform.domain.models.enums.MemberTier;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.models.enums.DocumentType;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.PersonRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountExportResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LegalDocumentService legalDocumentService;
    @Mock
    private ConsentService consentService;

    @InjectMocks
    private AccountService accountService;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "secret123";
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();

    private Member buildMember() {
        return Member.builder()
                .id(MEMBER_ID)
                .email(EMAIL)
                .password("encoded-password")
                .role(UserRole.PLAYER)
                .tier(MemberTier.FREE)
                .registeredAt(LocalDateTime.of(2025, 1, 1, 10, 0))
                .privacyPolicyAccepted(true)
                .privacyPolicyVersion("1.0")
                .personId(PERSON_ID)
                .build();
    }

    @Test
    void deleteAccount_deletes_when_password_matches() {
        Member member = buildMember();
        Member admin = Member.builder().id(UUID.randomUUID()).email("admin@example.com").role(UserRole.ADMIN).build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(true);
        when(memberRepository.findByRole(UserRole.ADMIN)).thenReturn(Optional.of(admin));

        accountService.deleteAccount(EMAIL, PASSWORD);

        verify(tournamentRepository).transferTournaments(MEMBER_ID, admin.getId());
        verify(memberRepository).anonymize(MEMBER_ID, "deleted-" + MEMBER_ID + "@anonymized.local");
        verify(personRepository).anonymize(PERSON_ID, "Eliminado");
    }

    @Test
    void deleteAccount_throws_when_member_not_found() {
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> accountService.deleteAccount(EMAIL, PASSWORD));
    }

    @Test
    void deleteAccount_throws_when_password_is_blank() {
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(buildMember()));

        assertThrows(InvalidArgumentException.class, () -> accountService.deleteAccount(EMAIL, "  "));
        assertThrows(InvalidArgumentException.class, () -> accountService.deleteAccount(EMAIL, null));
    }

    @Test
    void deleteAccount_throws_when_password_is_wrong() {
        Member member = buildMember();
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> accountService.deleteAccount(EMAIL, PASSWORD));
    }

    @Test
    void deleteAccount_throws_when_no_admin_found() {
        Member member = buildMember();
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(true);
        when(memberRepository.findByRole(UserRole.ADMIN)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> accountService.deleteAccount(EMAIL, PASSWORD));
    }

    @Test
    void deleteAccount_skips_person_anonymize_when_personId_is_null() {
        Member member = buildMember().toBuilder().personId(null).build();
        Member admin = Member.builder().id(UUID.randomUUID()).email("admin@example.com").role(UserRole.ADMIN).build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(PASSWORD, member.getPassword())).thenReturn(true);
        when(memberRepository.findByRole(UserRole.ADMIN)).thenReturn(Optional.of(admin));

        accountService.deleteAccount(EMAIL, PASSWORD);

        verify(personRepository, never()).anonymize(any(), any());
    }

    @Test
    void exportAccountData_returns_full_data_when_person_exists() {
        Member member = buildMember();
        Person person = Person.builder()
                .id(PERSON_ID)
                .firstName("John")
                .lastName("Doe")
                .nationality("ESP")
                .gender("M")
                .tennisId("T123")
                .build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person));

        AccountExportResponse result = accountService.exportAccountData(EMAIL);

        assertEquals(EMAIL, result.account().email());
        assertEquals("PLAYER", result.account().role());
        assertEquals("FREE", result.account().tier());
        assertTrue(result.account().privacyPolicyAccepted());
        assertEquals("1.0", result.account().privacyPolicyVersion());

        assertNotNull(result.person());
        assertEquals("John", result.person().firstName());
        assertEquals("Doe", result.person().lastName());
        assertEquals("ESP", result.person().nationality());
        assertEquals("M", result.person().gender());
        assertEquals("T123", result.person().tennisId());
    }

    @Test
    void exportAccountData_returns_null_person_when_personId_is_null() {
        Member member = buildMember().toBuilder().personId(null).build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

        AccountExportResponse result = accountService.exportAccountData(EMAIL);

        assertNotNull(result.account());
        assertNull(result.person());
    }

    @Test
    void exportAccountData_returns_null_person_when_person_not_found() {
        Member member = buildMember();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(personRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

        AccountExportResponse result = accountService.exportAccountData(EMAIL);

        assertNotNull(result.account());
        assertNull(result.person());
    }

    @Test
    void exportAccountData_throws_when_member_not_found() {
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> accountService.exportAccountData(EMAIL));
    }

    @Test
    void updatePrivacyConsent_updates_when_valid() {
        Member member = buildMember();
        LegalDocumentVersion docVersion = LegalDocumentVersion.builder().id(1L).version("2.0").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(legalDocumentService.getVersion(DocumentType.PRIVACY_POLICY, "2.0")).thenReturn(docVersion);

        accountService.updatePrivacyConsent(EMAIL, true, "2.0");

        verify(memberRepository).updatePrivacyConsent(eq(MEMBER_ID), eq(true), any(LocalDateTime.class), eq("2.0"));
    }

    @Test
    void updatePrivacyConsent_sets_null_when_declined() {
        Member member = buildMember();
        LegalDocumentVersion docVersion = LegalDocumentVersion.builder().id(1L).version("2.0").build();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(legalDocumentService.getVersion(DocumentType.PRIVACY_POLICY, "2.0")).thenReturn(docVersion);

        accountService.updatePrivacyConsent(EMAIL, false, "2.0");

        verify(memberRepository).updatePrivacyConsent(MEMBER_ID, false, null, "2.0");
    }

    @Test
    void updatePrivacyConsent_throws_when_member_not_found() {
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> accountService.updatePrivacyConsent(EMAIL, true, "2.0"));
    }

    @Test
    void updatePrivacyConsent_throws_when_version_is_blank() {
        Member member = buildMember();

        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));

        assertThrows(InvalidArgumentException.class, () -> accountService.updatePrivacyConsent(EMAIL, true, "  "));
        assertThrows(InvalidArgumentException.class, () -> accountService.updatePrivacyConsent(EMAIL, true, null));
    }
}

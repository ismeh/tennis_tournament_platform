package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.AccountService;
import com.tfm.tennis_platform.domain.models.ConsentRecord;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountDeletionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountDeletionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountExportResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ConsentHistoryResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ConsentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TermsConsentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @DeleteMapping
    public ResponseEntity<AccountDeletionResponse> deleteAccount(
            Principal principal,
            @RequestBody AccountDeletionRequest request) {
        accountService.deleteAccount(principal.getName(), request.password());
        log.info("Account deletion requested by user: {}", principal.getName());
        return ResponseEntity.ok(new AccountDeletionResponse(
                "Tu cuenta ha sido anonimizada y tus datos personales han sido eliminados. " +
                "Los torneos que creaste han sido transferidos al administrador.",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<AccountExportResponse> exportAccountData(Principal principal) {
        AccountExportResponse data = accountService.exportAccountData(principal.getName());
        return ResponseEntity.ok(data);
    }

    @PutMapping("/consent")
    public ResponseEntity<Void> updateConsent(
            Principal principal,
            @RequestBody ConsentRequest request) {
        accountService.updatePrivacyConsent(
                principal.getName(),
                request.accepted(),
                request.privacyPolicyVersion()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/consent/terms")
    public ResponseEntity<Void> updateTermsConsent(
            Principal principal,
            @RequestBody TermsConsentRequest request) {
        accountService.updateTermsConsent(
                principal.getName(),
                request.accepted(),
                request.termsConditionsVersion()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consent/history")
    public ResponseEntity<ConsentHistoryResponse> getConsentHistory(Principal principal) {
        List<ConsentRecord> records = accountService.getConsentHistory(principal.getName());
        List<ConsentHistoryResponse.ConsentEntry> entries = records.stream()
                .map(cr -> new ConsentHistoryResponse.ConsentEntry(
                        cr.getDocumentType().name(),
                        cr.getAction().name(),
                        null,
                        cr.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(new ConsentHistoryResponse(entries));
    }
}

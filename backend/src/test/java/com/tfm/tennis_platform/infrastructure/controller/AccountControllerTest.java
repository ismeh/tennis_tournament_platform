package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.AccountService;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountDeletionRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountDeletionResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.AccountExportResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.ConsentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;
    @InjectMocks
    private AccountController controller;

    @Test
    void should_delete_account() {
        Principal principal = () -> "user@test.com";
        AccountDeletionRequest request = new AccountDeletionRequest("password123");

        ResponseEntity<AccountDeletionResponse> result = controller.deleteAccount(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().message()).contains("anonimizada");
        assertThat(result.getBody().processedAt()).isNotNull();
        verify(accountService).deleteAccount("user@test.com", "password123");
    }

    @Test
    void should_export_account_data() {
        Principal principal = () -> "user@test.com";
        AccountExportResponse exportData = new AccountExportResponse(null, null, List.of(), List.of());

        when(accountService.exportAccountData("user@test.com")).thenReturn(exportData);

        ResponseEntity<AccountExportResponse> result = controller.exportAccountData(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(accountService).exportAccountData("user@test.com");
    }

    @Test
    void should_update_consent() {
        Principal principal = () -> "user@test.com";
        ConsentRequest request = new ConsentRequest(true, "v1.0");

        ResponseEntity<Void> result = controller.updateConsent(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(accountService).updatePrivacyConsent("user@test.com", true, "v1.0");
    }
}

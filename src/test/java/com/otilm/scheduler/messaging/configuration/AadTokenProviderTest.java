package com.otilm.scheduler.messaging.configuration;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AadTokenProviderTest {

    @Mock
    private TokenCredential credential;

    private AadTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new AadTokenProvider(credential);
    }

    @Test
    void apply_delegatesToGetTokenAndReturnsString() {
        AccessToken accessToken = new AccessToken("test-token", OffsetDateTime.now().plusHours(1));
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        Object result = tokenProvider.apply(null, null);

        assertEquals("test-token", result);
    }

    @Test
    void firstCall_fetchesTokenFromCredential() {
        AccessToken accessToken = new AccessToken("fresh-token", OffsetDateTime.now().plusHours(1));
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        Object result = tokenProvider.apply(null, null);

        assertEquals("fresh-token", result);
        verify(credential, times(1)).getToken(any(TokenRequestContext.class));
    }

    @Test
    void secondCall_returnsCachedToken_noSecondCredentialCall() {
        AccessToken accessToken = new AccessToken("cached-token", OffsetDateTime.now().plusHours(1));
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.just(accessToken));

        tokenProvider.apply(null, null);
        Object result = tokenProvider.apply(null, null);

        assertEquals("cached-token", result);
        verify(credential, times(1)).getToken(any(TokenRequestContext.class));
    }

    @Test
    void expiredToken_triggersRefresh() {
        AccessToken expiredToken = new AccessToken("old-token", OffsetDateTime.now().minusMinutes(1));
        AccessToken freshToken = new AccessToken("new-token", OffsetDateTime.now().plusHours(1));
        when(credential.getToken(any(TokenRequestContext.class)))
                .thenReturn(Mono.just(expiredToken))
                .thenReturn(Mono.just(freshToken));

        tokenProvider.apply(null, null);
        Object result = tokenProvider.apply(null, null);

        assertEquals("new-token", result);
        verify(credential, times(2)).getToken(any(TokenRequestContext.class));
    }

    @Test
    void nullTokenFromCredential_throwsIllegalStateException() {
        when(credential.getToken(any(TokenRequestContext.class))).thenReturn(Mono.empty());

        assertThrows(IllegalStateException.class, () -> tokenProvider.apply(null, null));
    }
}

package com.czertainly.scheduler.messaging.configuration;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import jakarta.jms.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.BiFunction;

/**
 * Token provider for Azure Active Directory (AAD) authentication with Azure Service Bus.
 * <p>
 * This class implements the Qpid JMS PASSWORD_OVERRIDE extension mechanism to provide
 * OAuth2 tokens for AMQP connections. It caches tokens and automatically refreshes them
 * before expiration.
 * </p>
 */
public class AadTokenProvider implements BiFunction<Connection, URI, Object> {

    private static final Logger logger = LoggerFactory.getLogger(AadTokenProvider.class);

    private static final String SERVICEBUS_SCOPE = "https://servicebus.azure.net/.default";
    private static final int TOKEN_REFRESH_BUFFER_MINUTES = 5;
    private static final Duration TOKEN_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final TokenCredential credential;
    private volatile String cachedToken;
    private volatile OffsetDateTime tokenExpiry;

    public AadTokenProvider(TokenCredential credential) {
        this.credential = credential;
    }

    @Override
    public Object apply(Connection connection, URI uri) {
        return getToken();
    }

    /**
     * Gets a valid OAuth2 token, refreshing if necessary.
     *
     * @return OAuth2 access token string
     */
    private synchronized String getToken() {
        if (isTokenExpired()) {
            refreshToken();
        }
        return cachedToken;
    }

    private boolean isTokenExpired() {
        return cachedToken == null
                || tokenExpiry == null
                || OffsetDateTime.now().plusMinutes(TOKEN_REFRESH_BUFFER_MINUTES).isAfter(tokenExpiry);
    }

    private void refreshToken() {
        logger.debug("Refreshing AAD token for Service Bus");
        TokenRequestContext context = new TokenRequestContext().addScopes(SERVICEBUS_SCOPE);
        AccessToken accessToken = credential.getToken(context).block(TOKEN_REQUEST_TIMEOUT);

        if (accessToken != null) {
            cachedToken = accessToken.getToken();
            tokenExpiry = accessToken.getExpiresAt();
            logger.debug("AAD token refreshed, expires at: {}", tokenExpiry);
        } else {
            throw new IllegalStateException("Failed to obtain AAD token for Service Bus");
        }
    }
}
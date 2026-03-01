package com.czertainly.scheduler.messaging.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.messaging")
@Validated
public record MessagingProperties(
        @NotNull MessagingProperties.BrokerType brokerType,
        String brokerUrl,
        String host,
        Integer port,
        String username,
        String password,
        String virtualHost,
        @NotBlank String exchange,
        RoutingKey routingKey,
        @Valid AadAuth aadAuth,
        Pool pool
) {

    /**
     * Validates authentication and connection configuration based on broker type.
     */
    public MessagingProperties {
        boolean hasUserAndPassword = StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password);
        boolean hasAadAuth = aadAuth != null && aadAuth.isEnabled();
        boolean hasBrokerUrl = StringUtils.isNotBlank(brokerUrl);
        boolean hasHostAndPort = StringUtils.isNotBlank(host) && port != null;

        switch (brokerType) {
            case RABBITMQ -> {
                if (!hasBrokerUrl && !hasHostAndPort) {
                    throw new IllegalArgumentException(
                            "RabbitMQ requires either BROKER_URL or BROKER_HOST and BROKER_PORT to be configured");
                }
                if (!hasUserAndPassword) {
                    throw new IllegalArgumentException(
                            "RabbitMQ requires BROKER_USERNAME and BROKER_PASSWORD to be configured");
                }
            }
            case SERVICEBUS -> {
                if (!hasBrokerUrl) {
                    throw new IllegalArgumentException(
                            "ServiceBus requires BROKER_URL to be configured");
                }
                if (!hasUserAndPassword && !hasAadAuth) {
                    throw new IllegalArgumentException(
                            "ServiceBus requires either BROKER_USERNAME/BROKER_PASSWORD (SAS) " +
                            "or BROKER_AZURE_TENANT_ID/BROKER_AZURE_CLIENT_ID/BROKER_AZURE_CLIENT_SECRET (AAD) to be configured");
                }
            }
        }
    }

    /**
     * Returns the effective broker URL for connection.
     * Uses brokerUrl if provided, otherwise constructs from host and port (for RabbitMQ legacy support).
     */
    public String getEffectiveBrokerUrl() {
        if (StringUtils.isNotBlank(brokerUrl)) {
            return brokerUrl;
        }
        // Construct URL from host and port (RabbitMQ legacy support)
        return "amqp://" + host + ":" + port;
    }

    public String producerDestination() {
        if (brokerType == BrokerType.SERVICEBUS) {
            return exchange();
        }

        return "/exchanges/" + exchange() + "/" + routingKey().scheduler();
    }

    public record RoutingKey(
            String scheduler
    ) {
    }

    public record AadAuth(
            String tenantId,
            String clientId,
            String clientSecret
    ) {
        /**
         * Checks if AAD authentication is enabled by verifying all required fields are present.
         *
         * @return true if all AAD credentials are provided, false otherwise
         */
        public boolean isEnabled() {
            return StringUtils.isNotBlank(tenantId)
                    && StringUtils.isNotBlank(clientId)
                    && StringUtils.isNotBlank(clientSecret);
        }
    }

    /**
     * Connection pool configuration for JmsPoolConnectionFactory.
     * Used for both ServiceBus and RabbitMQ producer connection factories.
     */
    public record Pool(
            Integer maxConnections,
            Integer connectionIdleTimeout,
            Integer connectionCheckInterval,
            Integer maxSessionsPerConnection,
            Boolean useAnonymousProducers
    ) {
        public Pool {
            if (maxConnections == null || maxConnections <= 0) maxConnections = 1;
            if (connectionIdleTimeout == null) connectionIdleTimeout = 30000;
            if (connectionCheckInterval == null) connectionCheckInterval = 60000;
            if (maxSessionsPerConnection == null || maxSessionsPerConnection <= 0) maxSessionsPerConnection = 500;
            if (useAnonymousProducers == null) useAnonymousProducers = true;
        }
    }

    public enum BrokerType {
        RABBITMQ,
        SERVICEBUS
    }
}

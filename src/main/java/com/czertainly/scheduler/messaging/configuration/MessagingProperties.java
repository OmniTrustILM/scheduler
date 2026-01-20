package com.czertainly.scheduler.messaging.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.messaging")
@Validated
public record MessagingProperties(
        @NotNull MessagingProperties.BrokerType brokerType,
        @NotBlank String brokerUrl,
        @NotNull @Positive int sessionCacheSize,
        String user,
        String password,
        String vhost,
        @NotBlank String exchange,
        RoutingKey routingKey,
        @Valid AadAuth aadAuth
) {

    /**
     * Validates authentication configuration based on broker type.
     */
    public MessagingProperties {
        boolean hasUserAndPassword = StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password);
        boolean hasAadAuth = aadAuth != null && aadAuth.isEnabled();

        switch (brokerType) {
            case RABBITMQ -> {
                if (!hasUserAndPassword) {
                    throw new IllegalArgumentException(
                            "RabbitMQ requires BROKER_USER and BROKER_PASSWORD to be configured");
                }
            }
            case SERVICEBUS -> {
                if (!hasUserAndPassword && !hasAadAuth) {
                    throw new IllegalArgumentException(
                            "ServiceBus requires either BROKER_USER/BROKER_PASSWORD (SAS) " +
                            "or AZURE_TENANT_ID/AZURE_CLIENT_ID/AZURE_CLIENT_SECRET (AAD) to be configured");
                }
            }
        }
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

    public enum BrokerType {
        RABBITMQ,
        SERVICEBUS
    }
}
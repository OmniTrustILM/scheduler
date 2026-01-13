package com.czertainly.scheduler.messaging.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.messaging")
@Validated
public record MessagingProperties(
        @NotNull MessagingProperties.BrokerType brokerType,
        @NotBlank String brokerUrl,
        @NotNull @Positive int sessionCacheSize,
        @NotBlank String user,
        @NotBlank String password,
        String vhost,
        @NotBlank String exchange,
        RoutingKey routingKey
) {

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

    public enum BrokerType {
        RABBITMQ,
        SERVICEBUS
    }
}
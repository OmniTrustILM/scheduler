package com.czertainly.scheduler.messaging.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.messaging")
@Validated
public record MessagingProperties(
        @NotNull BrokerName name,
        @NotBlank String brokerUrl,
        @NotBlank String user,
        @NotBlank String password,
        String vhost,
        @NotBlank String exchange,
        String exchangePrefix,
        RoutingKey routingKey
) {

    public String producerDestination() {
        if (name == BrokerName.SERVICEBUS) {
            return exchange();
        }

        if (exchangePrefix != null) {
            return exchangePrefix + exchange() + "/" + routingKey().scheduler();
        }
        return exchange() + "/" + routingKey().scheduler();
    }

    public record RoutingKey(
            String scheduler
    ) {
    }

    public enum BrokerName {
        RABBITMQ,
        SERVICEBUS
    }
}
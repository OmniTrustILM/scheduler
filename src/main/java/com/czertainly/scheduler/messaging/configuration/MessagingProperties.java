package com.czertainly.scheduler.messaging.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.messaging")
@Validated
public record MessagingProperties(
        @NotBlank String brokerUrl,
        @NotBlank String user,
        @NotBlank String password,
        String vhost,
        @NotBlank String exchange,
        String exchangePrefix,
        @NotNull @Valid RoutingKey routingKey
) {

    public String destionation() {
        if (exchangePrefix != null) {
            return exchangePrefix + "/" + exchange() + "/" + routingKey().scheduler();
        }
        return exchange() + "/" + routingKey().scheduler();
    }

    public record RoutingKey(
            @NotBlank String scheduler
    ) {
    }

}
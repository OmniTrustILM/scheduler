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
        @NotBlank String queue,
        @NotBlank String user,
        @NotBlank String password,
        @NotBlank String exchange,
        @NotNull @Valid RoutingKey routingKey
) {

    public String destionation() {
        return "/exchanges/" + exchange() + "/" + routingKey().scheduler();
    }

    public record RoutingKey(
            @NotBlank String scheduler
    ) {
    }

}
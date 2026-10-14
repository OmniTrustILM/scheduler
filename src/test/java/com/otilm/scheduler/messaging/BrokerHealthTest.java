package com.otilm.scheduler.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A scheduler that cannot reach the broker cannot hand any job to core, so the broker counts towards its health. Spring
 * Boot contributes that check only from its JMS auto-configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class BrokerHealthTest {

    private ApplicationContext context;

    @Autowired
    void setContext(ApplicationContext context) {
        this.context = context;
    }

    @Test
    void brokerCountsTowardsHealth() {
        assertTrue(context.containsBean("jmsHealthContributor"));
    }
}

package com.otilm.scheduler.service.impl;

import com.otilm.api.model.scheduler.SchedulerJobDto;
import com.otilm.api.model.scheduler.SchedulerRequestDto;
import com.otilm.api.model.scheduler.SchedulerTriggerState;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The service against a real Quartz scheduler rather than a mock answering call by call: the job and trigger keys it
 * creates are the ones it reads and removes, a disabled job reads PAUSED, and the fire times are the trigger's own. The
 * scheduler is in-memory and never started, so nothing fires and the fire times hold still for the assertions.
 */
class SchedulerServiceImplQuartzRoundTripTest {

    private static final String JOB_NAME = "CryptoAssetPqcSweepTask";
    private static final String CRON = "0 30 * ? * *";
    private static final String CLASS_NAME = "com.otilm.core.tasks.CryptoAssetPqcSweepTask";

    private Scheduler quartz;
    private SchedulerServiceImpl schedulerService;

    @BeforeEach
    void setUp() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "round-trip-" + UUID.randomUUID());
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        properties.setProperty("org.quartz.threadPool.threadCount", "1");
        quartz = new StdSchedulerFactory(properties).getScheduler();

        schedulerService = new SchedulerServiceImpl();
        schedulerService.setScheduler(quartz);
    }

    @AfterEach
    void shutDown() throws Exception {
        quartz.shutdown();
    }

    @Test
    void aJobReadsBackAsQuartzHoldsItThroughCreateDisableEnableAndDelete() throws Exception {
        Instant before = Instant.now();

        schedulerService.createNewJob(request());

        SchedulerJobDto created = listed().orElseThrow();
        assertEquals(CRON, created.getCronExpression());
        assertEquals(CLASS_NAME, created.getClassNameToBeExecuted());
        assertEquals(SchedulerTriggerState.NORMAL, created.getTriggerState());
        assertNotNull(created.getNextFireTime());
        // The trigger's own next fire: the next half past, within the hour ahead.
        ZonedDateTime next = created.getNextFireTime().atZone(ZoneId.systemDefault());
        assertEquals(30, next.getMinute());
        assertEquals(0, next.getSecond());
        assertFalse(created.getNextFireTime().isBefore(before.minusSeconds(1)));
        assertTrue(created.getNextFireTime().isBefore(before.plus(Duration.ofHours(1)).plusSeconds(1)));
        // Never started, so never fired.
        assertNull(created.getPreviousFireTime());

        schedulerService.disableJob(JOB_NAME);
        assertEquals(SchedulerTriggerState.PAUSED, listed().orElseThrow().getTriggerState());

        schedulerService.enableJob(JOB_NAME);
        assertEquals(SchedulerTriggerState.NORMAL, listed().orElseThrow().getTriggerState());

        schedulerService.deleteJob(JOB_NAME);
        assertTrue(listed().isEmpty());
        assertTrue(quartz.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
    }

    private Optional<SchedulerJobDto> listed() throws Exception {
        List<SchedulerJobDto> jobs = schedulerService.listJobs().getSchedulerJobList();
        return jobs.stream().filter(job -> JOB_NAME.equals(job.getJobName())).findFirst();
    }

    private static SchedulerRequestDto request() {
        SchedulerRequestDto request = new SchedulerRequestDto();
        request.setSchedulerJob(new SchedulerJobDto(JOB_NAME, CRON, CLASS_NAME));
        return request;
    }
}

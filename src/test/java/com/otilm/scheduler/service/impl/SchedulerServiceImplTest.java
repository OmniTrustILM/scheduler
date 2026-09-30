package com.otilm.scheduler.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.otilm.api.exception.SchedulerException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.scheduler.SchedulerJobDto;
import com.otilm.api.model.scheduler.SchedulerRequestDto;
import com.otilm.api.model.scheduler.SchedulerResponseDto;
import com.otilm.api.model.scheduler.SchedulerStatus;
import com.otilm.api.model.scheduler.SchedulerTriggerState;
import com.otilm.scheduler.constants.JobConstants;
import com.otilm.scheduler.utils.SchedulerUtils;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    private static final String CAUSE_MUST_BE_RETAINED = "the rethrown exception must retain the Quartz exception as its cause";

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    private SchedulerRequestDto schedulerRequestDto;
    private SchedulerJobDto schedulerJobDto;

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        schedulerJobDto = new SchedulerJobDto();
        schedulerJobDto.setJobName("testJob");
        schedulerJobDto.setCronExpression("0 0 12 * * ?");
        schedulerJobDto.setClassNameToBeExecuted("com.otilm.scheduler.TestJob");

        schedulerRequestDto = new SchedulerRequestDto();
        schedulerRequestDto.setSchedulerJob(schedulerJobDto);

        serviceLogger = (Logger) LoggerFactory.getLogger(SchedulerServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void createNewJobSuccessfully() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulerService.createNewJob(schedulerRequestDto);

        verify(scheduler).checkExists(any(JobKey.class));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createNewJobWhenJobAlreadyExists() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        schedulerService.createNewJob(schedulerRequestDto);

        verify(scheduler).checkExists(any(JobKey.class));
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createNewJobWithInvalidCronExpression() throws org.quartz.SchedulerException {
        schedulerJobDto.setCronExpression("invalid-cron");

        assertThrows(ValidationException.class, () -> schedulerService.createNewJob(schedulerRequestDto));
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createNewJobThrowsSchedulerException() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        doThrow(new org.quartz.SchedulerException("Scheduler error"))
                .when(scheduler)
                .scheduleJob(any(JobDetail.class), any(Trigger.class));

        assertThrows(SchedulerException.class, () -> schedulerService.createNewJob(schedulerRequestDto));
    }

    @Test
    void createNewJobWithEmptyCronExpression() {
        schedulerJobDto.setCronExpression("");

        assertThrows(ValidationException.class, () -> schedulerService.createNewJob(schedulerRequestDto));
    }

    @Test
    void createNewJobWithComplexCronExpression() throws Exception {
        schedulerJobDto.setCronExpression("0 0/5 14,18 * * ?");
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulerService.createNewJob(schedulerRequestDto);

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void updateJobSuccessfully() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulerService.updateJob(schedulerRequestDto);

        verify(scheduler).unscheduleJob(any(TriggerKey.class));
        verify(scheduler).deleteJob(any(JobKey.class));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void updateJobThrowsSchedulerExceptionOnDelete() throws Exception {
        doThrow(new org.quartz.SchedulerException("Delete error")).when(scheduler).deleteJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.updateJob(schedulerRequestDto));
    }

    @Test
    void updateJobWithInvalidCronExpression() throws Exception {
        schedulerJobDto.setCronExpression("invalid-cron");

        assertThrows(ValidationException.class, () -> schedulerService.updateJob(schedulerRequestDto));
        verify(scheduler).unscheduleJob(any(TriggerKey.class));
        verify(scheduler).deleteJob(any(JobKey.class));
    }

    /**
     * The trigger lives in the job's group, where prepareTrigger put it; an ungrouped key names a trigger that never
     * existed.
     */
    @Test
    void theTriggerKeyNamesTheTriggerPrepareTriggerCreates() {
        Trigger prepared = SchedulerUtils.prepareTrigger("testJob", "0 0 12 * * ?");

        assertEquals(new TriggerKey("testJob_trigger", "ilm"), prepared.getKey());
        assertEquals(prepared.getKey(), SchedulerUtils.triggerKey("testJob"));
    }

    @Test
    void deleteJobSuccessfully() throws Exception {
        schedulerService.deleteJob("testJob");

        verify(scheduler).unscheduleJob(new TriggerKey("testJob_trigger", "ilm"));
        verify(scheduler).deleteJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void deleteJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Delete error")).when(scheduler).deleteJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.deleteJob("testJob"));
    }

    @Test
    void listJobsSuccessfully() throws Exception {
        JobKey key1 = new JobKey("job1", JobConstants.GROUP_NAME);
        JobKey key2 = new JobKey("job2", JobConstants.GROUP_NAME);

        JobDetailImpl jobDetail1 = new JobDetailImpl();
        jobDetail1.setKey(key1);
        jobDetail1.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, "com.test.Job1");

        JobDetailImpl jobDetail2 = new JobDetailImpl();
        jobDetail2.setKey(key2);
        jobDetail2.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, "com.test.Job2");

        CronTriggerImpl trigger1 = new CronTriggerImpl();
        trigger1.setCronExpression("0 0 12 * * ?");
        trigger1.setKey(SchedulerUtils.triggerKey("job1"));

        CronTriggerImpl trigger2 = new CronTriggerImpl();
        trigger2.setCronExpression("0 0 18 * * ?");
        trigger2.setKey(SchedulerUtils.triggerKey("job2"));

        when(scheduler.getJobDetails(any(GroupMatcher.class))).thenReturn(List.of(jobDetail1, jobDetail2));
        when(scheduler.getTrigger(SchedulerUtils.triggerKey("job1"))).thenReturn(trigger1);
        when(scheduler.getTrigger(SchedulerUtils.triggerKey("job2"))).thenReturn(trigger2);
        when(scheduler.getTriggerState(any(TriggerKey.class))).thenReturn(Trigger.TriggerState.NORMAL);

        SchedulerResponseDto response = schedulerService.listJobs();

        assertNotNull(response);
        assertEquals(SchedulerStatus.OK, response.getSchedulerStatus());
        assertEquals(2, response.getSchedulerJobList().size());
        assertTrue(response
                .getSchedulerJobList()
                .stream()
                .allMatch(job -> job.getTriggerState() == SchedulerTriggerState.NORMAL));
    }

    @Test
    void listJobsWhenNoJobsExist() throws Exception {
        when(scheduler.getJobDetails(any(GroupMatcher.class))).thenReturn(List.of());

        SchedulerResponseDto response = schedulerService.listJobs();

        assertNotNull(response);
        assertEquals(SchedulerStatus.OK, response.getSchedulerStatus());
        assertNotNull(response.getSchedulerJobList());
        assertTrue(response.getSchedulerJobList().isEmpty());
    }

    @Test
    void listJobsThrowsSchedulerException() throws Exception {
        when(scheduler.getJobDetails(any(GroupMatcher.class)))
                .thenThrow(new org.quartz.SchedulerException("List error"));

        assertThrows(SchedulerException.class, () -> schedulerService.listJobs());
    }

    @Test
    void listJobsReportsEachTriggersObservedFacts() throws Exception {
        JobKey jobKey = new JobKey("job1", JobConstants.GROUP_NAME);
        TriggerKey triggerKey = SchedulerUtils.triggerKey("job1");
        Date previous = Date.from(Instant.parse("2026-09-29T10:00:00Z"));
        Date next = Date.from(Instant.parse("2026-09-29T12:00:00Z"));
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setCronExpression("0 0 12 * * ?");
        trigger.setKey(triggerKey);
        trigger.setPreviousFireTime(previous);
        trigger.setNextFireTime(next);
        when(scheduler.getJobDetails(any(GroupMatcher.class)))
                .thenReturn(List.of(jobDetailFor(jobKey, "com.test.Job1")));
        when(scheduler.getTrigger(triggerKey)).thenReturn(trigger);
        when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.PAUSED);

        SchedulerJobDto job = schedulerService.listJobs().getSchedulerJobList().get(0);

        assertEquals("job1", job.getJobName());
        assertEquals("0 0 12 * * ?", job.getCronExpression());
        assertEquals("com.test.Job1", job.getClassNameToBeExecuted());
        assertEquals(SchedulerTriggerState.PAUSED, job.getTriggerState());
        assertEquals(previous.toInstant(), job.getPreviousFireTime());
        assertEquals(next.toInstant(), job.getNextFireTime());
    }

    /**
     * A job without a trigger is a fact to report, not a fault: it is exactly the case core needs to see. Its state is
     * not asked for: a first createNewJob landing between the two reads would answer NORMAL, and core would then show a
     * scheduled job with no next fire time.
     */
    @Test
    void listJobsReportsAJobWithoutATriggerAsNoneWithoutReadingItsState() throws Exception {
        JobKey jobKey = new JobKey("job1", JobConstants.GROUP_NAME);
        when(scheduler.getJobDetails(any(GroupMatcher.class)))
                .thenReturn(List.of(jobDetailFor(jobKey, "com.test.Job1")));
        when(scheduler.getTrigger(SchedulerUtils.triggerKey("job1"))).thenReturn(null);
        lenient()
                .when(scheduler.getTriggerState(SchedulerUtils.triggerKey("job1")))
                .thenReturn(Trigger.TriggerState.NORMAL);

        SchedulerJobDto job = schedulerService.listJobs().getSchedulerJobList().get(0);

        assertEquals(SchedulerTriggerState.NONE, job.getTriggerState());
        assertNull(job.getCronExpression());
        assertNull(job.getNextFireTime());
        assertNull(job.getPreviousFireTime());
        verify(scheduler, never()).getTriggerState(any(TriggerKey.class));
    }

    /**
     * A trigger deleted, or replaced by updateJob, after it was read answers NONE when its state is asked for. The job
     * is then reported as one without a trigger, not with the fire times and expression of a trigger that is gone.
     */
    @Test
    void listJobsReportsATriggerGoneBeforeItsStateIsReadAsNoneWithoutItsFireTimes() throws Exception {
        JobKey jobKey = new JobKey("job1", JobConstants.GROUP_NAME);
        TriggerKey triggerKey = SchedulerUtils.triggerKey("job1");
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setCronExpression("0 0 12 * * ?");
        trigger.setKey(triggerKey);
        trigger.setPreviousFireTime(Date.from(Instant.parse("2026-09-29T10:00:00Z")));
        trigger.setNextFireTime(Date.from(Instant.parse("2026-09-29T12:00:00Z")));
        when(scheduler.getJobDetails(any(GroupMatcher.class)))
                .thenReturn(List.of(jobDetailFor(jobKey, "com.test.Job1")));
        when(scheduler.getTrigger(triggerKey)).thenReturn(trigger);
        when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NONE);

        SchedulerJobDto job = schedulerService.listJobs().getSchedulerJobList().get(0);

        assertEquals("job1", job.getJobName());
        assertEquals("com.test.Job1", job.getClassNameToBeExecuted());
        assertEquals(SchedulerTriggerState.NONE, job.getTriggerState());
        assertNull(job.getCronExpression());
        assertNull(job.getNextFireTime());
        assertNull(job.getPreviousFireTime());
    }

    @Test
    void listJobsReportsATriggerThatIsNotACronTriggerWithoutAnExpression() throws Exception {
        JobKey jobKey = new JobKey("job1", JobConstants.GROUP_NAME);
        TriggerKey triggerKey = SchedulerUtils.triggerKey("job1");
        Date previous = Date.from(Instant.parse("2026-09-29T10:00:00Z"));
        Date next = Date.from(Instant.parse("2026-09-29T12:00:00Z"));
        SimpleTriggerImpl trigger = new SimpleTriggerImpl();
        trigger.setKey(triggerKey);
        trigger.setPreviousFireTime(previous);
        trigger.setNextFireTime(next);
        when(scheduler.getJobDetails(any(GroupMatcher.class)))
                .thenReturn(List.of(jobDetailFor(jobKey, "com.test.Job1")));
        when(scheduler.getTrigger(triggerKey)).thenReturn(trigger);
        when(scheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);

        SchedulerJobDto job = schedulerService.listJobs().getSchedulerJobList().get(0);

        assertNull(job.getCronExpression());
        assertEquals(previous.toInstant(), job.getPreviousFireTime());
        assertEquals(next.toInstant(), job.getNextFireTime());
        assertEquals(SchedulerTriggerState.NORMAL, job.getTriggerState());
    }

    /** Core lists on every list page and detail view, so a listing must not write an INFO line each time. */
    @Test
    void listJobsLogsNothingAtInfo() throws Exception {
        when(scheduler.getJobDetails(any(GroupMatcher.class))).thenReturn(List.of());

        schedulerService.listJobs();

        assertTrue(logAppender.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.INFO)),
                "a listing must log below INFO");
    }

    @Test
    void everyQuartzTriggerStateHasAWireValue() {
        for (Trigger.TriggerState state : Trigger.TriggerState.values()) {
            assertEquals(state.name(), SchedulerUtils.triggerStateOf(state).name());
        }
    }

    @Test
    void enableJobSuccessfully() throws Exception {
        schedulerService.enableJob("testJob");

        verify(scheduler).resumeJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void enableJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Resume error")).when(scheduler).resumeJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.enableJob("testJob"));
    }

    @Test
    void disableJobSuccessfully() throws Exception {
        schedulerService.disableJob("testJob");

        verify(scheduler).pauseJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void disableJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Pause error")).when(scheduler).pauseJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.disableJob("testJob"));
    }

    @Test
    void createNewJobWithSpecialCharactersInJobName() throws Exception {
        schedulerJobDto.setJobName("test-job_123");
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulerService.createNewJob(schedulerRequestDto);

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void updateJobMultipleTimes() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulerService.updateJob(schedulerRequestDto);
        schedulerService.updateJob(schedulerRequestDto);

        verify(scheduler, times(2)).unscheduleJob(any(TriggerKey.class));
        verify(scheduler, times(2)).deleteJob(any(JobKey.class));
        verify(scheduler, times(2)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void createNewJobWithDifferentCronExpressions() throws Exception {
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        String[] validCronExpressions = {
                "0 0 12 * * ?", // Every day at noon
                "0 15 10 * * ?", // 10:15 AM every day
                "0 0/5 * * * ?", // Every 5 minutes
                "0 0 0 1 1 ?", // Midnight on January 1st
                "0 0 22 ? * MON-FRI" // 10 PM Monday through Friday
        };

        for (String cronExpression : validCronExpressions) {
            schedulerJobDto.setCronExpression(cronExpression);
            schedulerJobDto.setJobName("testJob_" + cronExpression.hashCode());
            schedulerService.createNewJob(schedulerRequestDto);
        }

        verify(scheduler, times(validCronExpressions.length)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void deleteNonExistentJob() throws Exception {
        doThrow(new org.quartz.SchedulerException("Job not found")).when(scheduler).deleteJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.deleteJob("nonExistentJob"));
    }

    @Test
    void enableAlreadyEnabledJob() throws Exception {
        schedulerService.enableJob("testJob");

        verify(scheduler).resumeJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void disableAlreadyDisabledJob() throws Exception {
        schedulerService.disableJob("testJob");

        verify(scheduler).pauseJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void createNewJobLogsTheCaughtException() throws Exception {
        final Throwable cause = new org.quartz.SchedulerException("Scheduler error");
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);
        doThrow(cause).when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        final SchedulerException thrown = assertThrows(SchedulerException.class,
                () -> schedulerService.createNewJob(schedulerRequestDto));

        assertErrorLoggedWith(cause);
        assertSame(cause, thrown.getCause(), CAUSE_MUST_BE_RETAINED);
    }

    @Test
    void deleteJobLogsTheCaughtException() throws Exception {
        final Throwable cause = new org.quartz.SchedulerException("Delete error");
        doThrow(cause).when(scheduler).deleteJob(any(JobKey.class));

        final SchedulerException thrown = assertThrows(SchedulerException.class,
                () -> schedulerService.deleteJob("testJob"));

        assertErrorLoggedWith(cause);
        assertSame(cause, thrown.getCause(), CAUSE_MUST_BE_RETAINED);
    }

    @Test
    void listJobsLogsTheCaughtException() throws Exception {
        final Throwable cause = new org.quartz.SchedulerException("List error");
        when(scheduler.getJobDetails(any())).thenThrow(cause);

        final SchedulerException thrown = assertThrows(SchedulerException.class, () -> schedulerService.listJobs());

        assertErrorLoggedWith(cause);
        assertSame(cause, thrown.getCause(), CAUSE_MUST_BE_RETAINED);
    }

    @Test
    void enableJobLogsTheCaughtException() throws Exception {
        final Throwable cause = new org.quartz.SchedulerException("Resume error");
        doThrow(cause).when(scheduler).resumeJob(any(JobKey.class));

        final SchedulerException thrown = assertThrows(SchedulerException.class,
                () -> schedulerService.enableJob("testJob"));

        assertErrorLoggedWith(cause);
        assertSame(cause, thrown.getCause(), CAUSE_MUST_BE_RETAINED);
    }

    @Test
    void disableJobLogsTheCaughtException() throws Exception {
        final Throwable cause = new org.quartz.SchedulerException("Pause error");
        doThrow(cause).when(scheduler).pauseJob(any(JobKey.class));

        final SchedulerException thrown = assertThrows(SchedulerException.class,
                () -> schedulerService.disableJob("testJob"));

        assertErrorLoggedWith(cause);
        assertSame(cause, thrown.getCause(), CAUSE_MUST_BE_RETAINED);
    }

    private static JobDetailImpl jobDetailFor(JobKey jobKey, String className) {
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(jobKey);
        jobDetail.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, className);
        return jobDetail;
    }

    /**
     * Asserts that exactly one ERROR event was recorded and that it carries {@code expected} as its throwable.
     *
     * @param expected Exception the service was expected to hand to the logger.
     */
    private void assertErrorLoggedWith(Throwable expected) {
        final List<ILoggingEvent> errors = logAppender.list
                .stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .toList();
        assertEquals(1, errors.size(), "expected exactly one ERROR event");

        final IThrowableProxy thrown = errors.get(0).getThrowableProxy();
        assertNotNull(thrown, "the caught exception must be passed to the logger, not its message");
        assertEquals(expected.getClass().getName(), thrown.getClassName());
        assertEquals(expected.getMessage(), thrown.getMessage());
    }
}

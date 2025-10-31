package com.czertainly.scheduler.service.impl;

import com.czertainly.api.exception.SchedulerException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.scheduler.SchedulerJobDto;
import com.czertainly.api.model.scheduler.SchedulerRequestDto;
import com.czertainly.api.model.scheduler.SchedulerResponseDto;
import com.czertainly.api.model.scheduler.SchedulerStatus;
import com.czertainly.scheduler.constants.JobConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    private SchedulerRequestDto schedulerRequestDto;
    private SchedulerJobDto schedulerJobDto;

    @BeforeEach
    void setUp() {
        schedulerJobDto = new SchedulerJobDto();
        schedulerJobDto.setJobName("testJob");
        schedulerJobDto.setCronExpression("0 0 12 * * ?");
        schedulerJobDto.setClassNameToBeExecuted("com.czertainly.scheduler.TestJob");

        schedulerRequestDto = new SchedulerRequestDto();
        schedulerRequestDto.setSchedulerJob(schedulerJobDto);
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
                .when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

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
        doThrow(new org.quartz.SchedulerException("Delete error"))
                .when(scheduler).deleteJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.updateJob(schedulerRequestDto));
    }

    @Test
    void updateJobWithInvalidCronExpression() throws Exception {
        schedulerJobDto.setCronExpression("invalid-cron");

        assertThrows(ValidationException.class, () -> schedulerService.updateJob(schedulerRequestDto));
        verify(scheduler).unscheduleJob(any(TriggerKey.class));
        verify(scheduler).deleteJob(any(JobKey.class));
    }

    @Test
    void deleteJobSuccessfully() throws Exception {
        schedulerService.deleteJob("testJob");

        verify(scheduler).unscheduleJob(new TriggerKey("testJob" + JobConstants.JOB_TRIGGER_SUFFIX));
        verify(scheduler).deleteJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void deleteJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Delete error"))
                .when(scheduler).deleteJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.deleteJob("testJob"));
    }

    @Test
    void listJobsSuccessfully() throws Exception {
        Set<JobKey> jobKeys = new HashSet<>();
        jobKeys.add(new JobKey("job1", JobConstants.GROUP_NAME));
        jobKeys.add(new JobKey("job2", JobConstants.GROUP_NAME));

        JobDetailImpl jobDetail1 = new JobDetailImpl();
        jobDetail1.setKey(new JobKey("job1", JobConstants.GROUP_NAME));
        jobDetail1.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, "com.test.Job1");

        JobDetailImpl jobDetail2 = new JobDetailImpl();
        jobDetail2.setKey(new JobKey("job2", JobConstants.GROUP_NAME));
        jobDetail2.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, "com.test.Job2");

        CronTriggerImpl trigger1 = new CronTriggerImpl();
        trigger1.setCronExpression("0 0 12 * * ?");
        trigger1.setKey(new TriggerKey("job1" + JobConstants.JOB_TRIGGER_SUFFIX));

        CronTriggerImpl trigger2 = new CronTriggerImpl();
        trigger2.setCronExpression("0 0 18 * * ?");
        trigger2.setKey(new TriggerKey("job2" + JobConstants.JOB_TRIGGER_SUFFIX));

        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(jobKeys);
        when(scheduler.getJobDetail(new JobKey("job1", JobConstants.GROUP_NAME))).thenReturn(jobDetail1);
        when(scheduler.getJobDetail(new JobKey("job2", JobConstants.GROUP_NAME))).thenReturn(jobDetail2);
        when(scheduler.getTrigger(new TriggerKey("job1" + JobConstants.JOB_TRIGGER_SUFFIX))).thenReturn(trigger1);
        when(scheduler.getTrigger(new TriggerKey("job2" + JobConstants.JOB_TRIGGER_SUFFIX))).thenReturn(trigger2);

        SchedulerResponseDto response = schedulerService.listJobs();

        assertNotNull(response);
        assertEquals(SchedulerStatus.OK, response.getSchedulerStatus());
        assertNotNull(response.getSchedulerJobList());
        assertEquals(2, response.getSchedulerJobList().size());
    }

    @Test
    void listJobsWhenNoJobsExist() throws Exception {
        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(new HashSet<>());

        SchedulerResponseDto response = schedulerService.listJobs();

        assertNotNull(response);
        assertEquals(SchedulerStatus.OK, response.getSchedulerStatus());
        assertNotNull(response.getSchedulerJobList());
        assertTrue(response.getSchedulerJobList().isEmpty());
    }

    @Test
    void listJobsThrowsSchedulerException() throws Exception {
        when(scheduler.getJobKeys(any(GroupMatcher.class)))
                .thenThrow(new org.quartz.SchedulerException("List error"));

        assertThrows(SchedulerException.class, () -> schedulerService.listJobs());
    }

    @Test
    void listJobsWithMissingTrigger() throws Exception {
        Set<JobKey> jobKeys = new HashSet<>();
        jobKeys.add(new JobKey("job1", JobConstants.GROUP_NAME));

        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setKey(new JobKey("job1", JobConstants.GROUP_NAME));
        jobDetail.getJobDataMap().put(JobConstants.CLASS_TOBE_EXECUTED, "com.test.Job1");

        when(scheduler.getJobKeys(any(GroupMatcher.class))).thenReturn(jobKeys);
        when(scheduler.getJobDetail(any(JobKey.class))).thenReturn(jobDetail);
        when(scheduler.getTrigger(any(TriggerKey.class))).thenReturn(null);

        assertThrows(NullPointerException.class, () -> schedulerService.listJobs());
    }

    @Test
    void enableJobSuccessfully() throws Exception {
        schedulerService.enableJob("testJob");

        verify(scheduler).resumeJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void enableJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Resume error"))
                .when(scheduler).resumeJob(any(JobKey.class));

        assertThrows(SchedulerException.class, () -> schedulerService.enableJob("testJob"));
    }

    @Test
    void disableJobSuccessfully() throws Exception {
        schedulerService.disableJob("testJob");

        verify(scheduler).pauseJob(new JobKey("testJob", JobConstants.GROUP_NAME));
    }

    @Test
    void disableJobThrowsSchedulerException() throws Exception {
        doThrow(new org.quartz.SchedulerException("Pause error"))
                .when(scheduler).pauseJob(any(JobKey.class));

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
                "0 0 12 * * ?",           // Every day at noon
                "0 15 10 * * ?",          // 10:15 AM every day
                "0 0/5 * * * ?",          // Every 5 minutes
                "0 0 0 1 1 ?",            // Midnight on January 1st
                "0 0 22 ? * MON-FRI"      // 10 PM Monday through Friday
        };

        for (String cronExpression : validCronExpressions) {
            schedulerJobDto.setCronExpression(cronExpression);
            schedulerJobDto.setJobName("testJob_" + cronExpression.hashCode());
            schedulerService.createNewJob(schedulerRequestDto);
        }

        verify(scheduler, times(validCronExpressions.length))
                .scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void deleteNonExistentJob() throws Exception {
        doThrow(new org.quartz.SchedulerException("Job not found"))
                .when(scheduler).deleteJob(any(JobKey.class));

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
}


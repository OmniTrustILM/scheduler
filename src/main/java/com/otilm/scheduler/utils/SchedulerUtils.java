package com.otilm.scheduler.utils;

import com.otilm.scheduler.constants.JobConstants;
import com.otilm.scheduler.jobs.SchedulerJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

public class SchedulerUtils {

    public static JobDetail prepareJobDetail(final String jobName, final String className) {
        return JobBuilder
                .newJob(SchedulerJob.class)
                .withIdentity(jobName, JobConstants.GROUP_NAME)
                .usingJobData(JobConstants.CLASS_TOBE_EXECUTED, className)
                .build();
    }

    public static Trigger prepareTrigger(final String jobName, final String cronExpression) {
        return TriggerBuilder
                .newTrigger()
                .withIdentity(triggerKey(jobName))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }

    /** The one key a job's trigger is created, read and removed by: in the job's group, not Quartz's default one. */
    public static TriggerKey triggerKey(final String jobName) {
        return new TriggerKey(jobName + JobConstants.JOB_TRIGGER_SUFFIX, JobConstants.GROUP_NAME);
    }

}

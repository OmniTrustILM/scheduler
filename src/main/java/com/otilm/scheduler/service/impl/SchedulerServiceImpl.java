package com.otilm.scheduler.service.impl;

import com.otilm.api.exception.SchedulerException;
import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.scheduler.SchedulerJobDto;
import com.otilm.api.model.scheduler.SchedulerRequestDto;
import com.otilm.api.model.scheduler.SchedulerResponseDto;
import com.otilm.api.model.scheduler.SchedulerStatus;
import com.otilm.api.model.scheduler.SchedulerTriggerState;
import com.otilm.scheduler.constants.JobConstants;
import com.otilm.scheduler.service.SchedulerService;
import com.otilm.scheduler.utils.SchedulerUtils;
import java.util.ArrayList;
import java.util.List;
import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SchedulerServiceImpl implements SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private Scheduler scheduler;

    @Override
    public void createNewJob(SchedulerRequestDto schedulerDto) throws SchedulerException {
        final SchedulerJobDto schedulerDetail = schedulerDto.getSchedulerJob();
        try {
            if (scheduler.checkExists(new JobKey(schedulerDetail.getJobName(), JobConstants.GROUP_NAME))) {
                logger.info("Job {} already exists.", schedulerDto.getSchedulerJob().getJobName());
                return;
            }

            if (!CronExpression.isValidExpression(schedulerDetail.getCronExpression())) {
                throw new ValidationException(ValidationError.create("Invalid format of CRON expression"));
            }

            logger.info("Scheduling new job with name {}", schedulerDetail.getJobName());
            final JobDetail jobDetail = SchedulerUtils
                    .prepareJobDetail(schedulerDetail.getJobName(), schedulerDetail.getClassNameToBeExecuted());
            final Trigger jobTrigger = SchedulerUtils
                    .prepareTrigger(schedulerDetail.getJobName(), schedulerDetail.getCronExpression());
            scheduler.scheduleJob(jobDetail, jobTrigger);
            logger
                    .info("Job {} scheduled with CRON expression {}", schedulerDetail.getJobName(),
                            schedulerDetail.getCronExpression());
        } catch (org.quartz.SchedulerException e) {
            logger.error("Unable to schedule job {}", schedulerDetail.getJobName(), e);
            throw new SchedulerException(e.getMessage(), e);
        }
    }

    @Override
    public void updateJob(SchedulerRequestDto schedulerDto) throws SchedulerException {
        final SchedulerJobDto schedulerDetail = schedulerDto.getSchedulerJob();
        logger.info("Updating job with name {}", schedulerDetail.getJobName());
        deleteJob(schedulerDetail.getJobName());
        createNewJob(schedulerDto);
    }

    @Override
    public void deleteJob(String jobName) throws SchedulerException {
        logger.info("Delete/Unregister job with name {}", jobName);
        try {
            scheduler.unscheduleJob(SchedulerUtils.triggerKey(jobName));
            scheduler.deleteJob(new JobKey(jobName, JobConstants.GROUP_NAME));
            logger.info("Job {} was unregistered.", jobName);
        } catch (org.quartz.SchedulerException e) {
            logger.error("Unable to unregister job {}", jobName, e);
            throw new SchedulerException(e.getMessage(), e);
        }
    }

    @Override
    public SchedulerResponseDto listJobs() throws SchedulerException {
        logger.info("Retrieve list of registered jobs.");
        final List<SchedulerJobDto> schedulerDetailList = new ArrayList<>();
        try {
            for (final JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JobConstants.GROUP_NAME))) {
                final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDetail == null) {
                    // Deleted by a concurrent request or node after the key listing. Only this window is guarded: a
                    // trigger gone by the time describe reads it reports NONE, but a read Quartz itself fails (a delete
                    // and a re-create inside getTrigger's own reads) still fails this listing; the next one is right.
                    continue;
                }
                schedulerDetailList.add(describe(jobKey, jobDetail));
            }
        } catch (org.quartz.SchedulerException e) {
            logger.error("Unable to retrieve list of registered jobs.", e);
            throw new SchedulerException(e.getMessage(), e);
        }

        final SchedulerResponseDto schedulerResponseDto = new SchedulerResponseDto(SchedulerStatus.OK);
        schedulerResponseDto.setSchedulerJobList(schedulerDetailList);
        return schedulerResponseDto;
    }

    /**
     * What Quartz holds for one job: the trigger, looked up in the group prepareTrigger created it in, and that
     * trigger's state -- NONE when the job has no trigger, which is a fact to report rather than a fault. The state is
     * read only for a trigger this read found, so a trigger created after it cannot lend its state to a job reported
     * without fire times. Only a cron trigger has an expression to report; the fire times are any trigger's.
     */
    private SchedulerJobDto describe(final JobKey jobKey, final JobDetail jobDetail)
            throws org.quartz.SchedulerException {
        final TriggerKey triggerKey = SchedulerUtils.triggerKey(jobKey.getName());
        final Trigger trigger = scheduler.getTrigger(triggerKey);
        final String cronExpression = trigger instanceof CronTrigger cronTrigger
                ? cronTrigger.getCronExpression()
                : null;
        final String className = jobDetail.getJobDataMap().getString(JobConstants.CLASS_TOBE_EXECUTED);
        final SchedulerJobDto job = new SchedulerJobDto(jobKey.getName(), cronExpression, className);
        if (trigger == null) {
            job.setTriggerState(SchedulerTriggerState.NONE);
            return job;
        }
        job.setTriggerState(SchedulerUtils.triggerStateOf(scheduler.getTriggerState(triggerKey)));
        job.setNextFireTime(SchedulerUtils.toInstant(trigger.getNextFireTime()));
        job.setPreviousFireTime(SchedulerUtils.toInstant(trigger.getPreviousFireTime()));
        return job;
    }

    @Override
    public void enableJob(String jobName) throws SchedulerException {
        logger.info("Enabling job with name {}", jobName);
        try {
            scheduler.resumeJob(new JobKey(jobName, JobConstants.GROUP_NAME));
            logger.info("Job {} was resumed.", jobName);
        } catch (org.quartz.SchedulerException e) {
            logger.error("Unable to resume job {}", jobName, e);
            throw new SchedulerException(e.getMessage(), e);
        }
    }

    @Override
    public void disableJob(String jobName) throws SchedulerException {
        logger.info("Disabling job with name {}", jobName);
        try {
            scheduler.pauseJob(new JobKey(jobName, JobConstants.GROUP_NAME));
            logger.info("Job {} was paused.", jobName);
        } catch (org.quartz.SchedulerException e) {
            logger.error("Unable to pause job {}", jobName, e);
            throw new SchedulerException(e.getMessage(), e);
        }
    }

    // SETTERs

    @Autowired
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}

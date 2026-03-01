package com.czertainly.scheduler.jobs;

import com.czertainly.api.model.scheduler.SchedulerJobExecutionMessage;
import com.czertainly.scheduler.constants.JobConstants;
import com.czertainly.scheduler.messaging.JmsMessageProducer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchedulerJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerJob.class);

    private JmsMessageProducer messageProducer;

    @Autowired
    public void setMessageProducer(JmsMessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        final String jobName = jobExecutionContext.getJobDetail().getKey().getName();
        logger.info("SchedulerJob {} was fired.", jobName);

        final String jobClassName = jobExecutionContext.getJobDetail().getJobDataMap().getString(JobConstants.CLASS_TOBE_EXECUTED);
        final SchedulerJobExecutionMessage schedulerExecutionMessage = new SchedulerJobExecutionMessage(jobName, jobClassName);
        messageProducer.sendMessage(schedulerExecutionMessage);
    }
}

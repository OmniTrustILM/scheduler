package com.otilm.scheduler.service;

import com.otilm.api.exception.ConnectorException;
import com.otilm.api.exception.SchedulerException;
import com.otilm.api.model.scheduler.SchedulerRequestDto;
import com.otilm.api.model.scheduler.SchedulerResponseDto;

public interface SchedulerService {

    void createNewJob(final SchedulerRequestDto schedulerDto) throws SchedulerException;

    void updateJob(final SchedulerRequestDto schedulerDto) throws SchedulerException;

    void deleteJob(final String jobName) throws SchedulerException;

    SchedulerResponseDto listJobs() throws SchedulerException;

    void enableJob(final String jobName) throws ConnectorException, SchedulerException;

    void disableJob(final String jobName) throws SchedulerException;

}

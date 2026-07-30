package org.dromara.common.job.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 独立开启 Spring 定时调度，避免受 SnailJob 开关影响。
 */
@AutoConfiguration
@EnableScheduling
public class SchedulingConfig {
}

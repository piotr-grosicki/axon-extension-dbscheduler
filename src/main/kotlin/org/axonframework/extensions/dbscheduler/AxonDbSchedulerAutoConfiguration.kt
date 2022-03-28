package org.axonframework.extensions.dbscheduler

import com.github.kagkarlsson.scheduler.Scheduler
import org.axonframework.extensions.dbscheduler.deadline.AxonDbSchedulerDeadlineConfiguration
import org.axonframework.springboot.autoconfig.InfraConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnClass(Scheduler::class)
@AutoConfigureAfter(InfraConfiguration::class)
@Import(AxonDbSchedulerDeadlineConfiguration::class)
class AxonDbSchedulerAutoConfiguration
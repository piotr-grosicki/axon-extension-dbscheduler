package org.axonframework.extensions.dbscheduler.deadline

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.DeadExecutionHandler
import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import org.axonframework.common.AxonNonTransientException
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.serialization.Serializer
import org.axonframework.spring.config.AxonConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy

class AxonDbSchedulerDeadlineConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun deadlineManager(
        scheduler: Scheduler,
        deadlineTaskDataSerializer: DeadlineTaskDataSerializer,
        taskTemplate: Task<DeadlineTask.DataMap>
    ): DeadlineManager = DbSchedulerDeadlineManager(scheduler, deadlineTaskDataSerializer, taskTemplate)


    @Bean
    @ConditionalOnMissingBean
    fun deadlineTaskDataSerializer(
        serializer: Serializer
    ) = DeadlineTaskDataSerializer(serializer)

    @Bean
    @ConditionalOnMissingBean
    fun deadlineTaskTemplate(
        transactionManager: TransactionManager,
        axonConfiguration: AxonConfiguration,
        @Lazy handlerInterceptorsProvider: HandlerInterceptorsProvider,
        deadlineTaskDataSerializer: DeadlineTaskDataSerializer,
        dbSchedulerDeadlineRefirePolicy: DbSchedulerDeadlineFailureHandler
    ): Task<DeadlineTask.DataMap> =
        Tasks.oneTime(DbSchedulerDeadlineManager.TASK_NAME, DeadlineTask.DataMap::class.java)
            .onFailure(dbSchedulerDeadlineRefirePolicy)
            .onDeadExecution(DeadExecutionHandler.ReviveDeadExecution())
            .execute { instance, _ ->
                DeadlineTask(
                    transactionManager = transactionManager,
                    scopeAwareComponents = ConfigurationScopeAwareProvider(axonConfiguration),
                    interceptors = { handlerInterceptorsProvider.getHandlerInterceptors() }
                ).execute(deadlineTaskDataSerializer.deserialize(instance.data))
            }

    @Bean
    @ConditionalOnMissingBean
    fun refirePolicy() = DbSchedulerDeadlineFailureHandler { it !is AxonNonTransientException }
}
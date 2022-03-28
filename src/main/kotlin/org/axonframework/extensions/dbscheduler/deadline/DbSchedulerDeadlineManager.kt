package org.axonframework.extensions.dbscheduler.deadline

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.TaskInstanceId
import com.github.kagkarlsson.scheduler.task.TaskInstanceId.StandardTaskInstanceId
import org.axonframework.deadline.AbstractDeadlineManager
import org.axonframework.deadline.DeadlineException
import org.axonframework.deadline.DeadlineMessage
import org.axonframework.deadline.GenericDeadlineMessage
import org.axonframework.lifecycle.Phase
import org.axonframework.lifecycle.ShutdownHandler
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.ScopeDescriptor
import java.time.Instant

class DbSchedulerDeadlineManager(
    private val scheduler: Scheduler,
    private val deadlineTaskDataSerializer: DeadlineTaskDataSerializer,
    private val deadlineTaskTemplate: Task<DeadlineTask.DataMap>
) : AbstractDeadlineManager(), HandlerInterceptorsProvider {

    override fun schedule(
        triggerDateTime: Instant,
        deadlineName: String,
        messageOrPayload: Any?,
        deadlineScope: ScopeDescriptor
    ): String {
        val deadlineMessage = toDeadlineMessage(messageOrPayload, deadlineName, triggerDateTime)
        val deadlineId = createDeadlineId(deadlineMessage.deadlineName, deadlineMessage.identifier)

        runOnPrepareCommitOrNow {
            val interceptedDeadlineMessage = processDispatchInterceptors(deadlineMessage)
            val deadlineTaskData = createDeadlineTaskData(
                deadlineName = deadlineName,
                deadlineMessage = interceptedDeadlineMessage,
                scopeDescriptor = deadlineScope
            )

            scheduler.schedule(
                deadlineTaskTemplate.instance(
                    deadlineId,
                    deadlineTaskDataSerializer.serialize(deadlineTaskData)
                ),
                triggerDateTime
            )
        }

        return deadlineId
    }

    override fun cancelSchedule(deadlineName: String, scheduleId: String) {
        runOnPrepareCommitOrNow {
            cancelSchedule(StandardTaskInstanceId(TASK_NAME, createDeadlineId(deadlineName, scheduleId)))
        }
    }

    override fun cancelAll(deadlineName: String) {
        runOnPrepareCommitOrNow {
            getAllDeadlineTasks()
                .filter { extractDeadlineName(it.taskInstance) == deadlineName }
                .forEach { cancelSchedule(it.taskInstance) }
        }
    }

    override fun cancelAllWithinScope(deadlineName: String, scope: ScopeDescriptor) {
        runOnPrepareCommitOrNow {
            getAllDeadlineTasks()
                .filter { deadlineTaskDataSerializer.deserialize(it.data).scopeDescriptor.get() == scope }
                .forEach { cancelSchedule(it.taskInstance) }
        }
    }

    @ShutdownHandler(phase = Phase.INBOUND_EVENT_CONNECTORS)
    override fun shutdown() {
        try {
            scheduler.stop()
        } catch (e: Exception) {
            throw DeadlineException("An error occurred while trying to shutdown the deadline manager", e)
        }
    }

    private fun cancelSchedule(taskInstanceId: TaskInstanceId) {
        try {
            scheduler.cancel(taskInstanceId)
        } catch (e: Exception) {
            throw DeadlineException("Could not cancel deadline with id ${taskInstanceId.id}", e)
        }
    }

    override fun getHandlerInterceptors(): List<MessageHandlerInterceptor<in DeadlineMessage<Any>>> =
        handlerInterceptors()

    private fun createDeadlineTaskData(
        deadlineName: String,
        deadlineMessage: DeadlineMessage<Any>,
        scopeDescriptor: ScopeDescriptor
    ): DeadlineTask.Data =
        DeadlineTask.Data(
            deadlineName = deadlineName,
            deadlineMessage = { deadlineMessage },
            scopeDescriptor = { scopeDescriptor }
        )


    private fun toDeadlineMessage(
        messageOrPayload: Any?,
        deadlineName: String,
        triggerDateTime: Instant
    ) = GenericDeadlineMessage.asDeadlineMessage<Any>(deadlineName, messageOrPayload, triggerDateTime)


    private fun getAllDeadlineTasks() =
        scheduler.getScheduledExecutionsForTask(TASK_NAME, DeadlineTask.DataMap::class.java)

    private fun createDeadlineId(deadlineName: String, scheduleId: String) =
        "$deadlineName${TASK_INSTANCE_ID_DELIMITER}$scheduleId"

    private fun extractDeadlineName(taskInstanceId: TaskInstanceId) =
        taskInstanceId.id.substringBefore(TASK_INSTANCE_ID_DELIMITER)

    companion object {
        const val TASK_NAME = "axon-deadline"
        private const val TASK_INSTANCE_ID_DELIMITER = ';'
    }

}

interface HandlerInterceptorsProvider {
    fun getHandlerInterceptors(): List<MessageHandlerInterceptor<in DeadlineMessage<Any>>>
}
package org.axonframework.extensions.dbscheduler.deadline

import com.github.kagkarlsson.scheduler.task.ExecutionComplete
import com.github.kagkarlsson.scheduler.task.ExecutionOperations
import com.github.kagkarlsson.scheduler.task.FailureHandler
import mu.KLogging
import java.time.Instant
import java.util.function.Predicate

class DbSchedulerDeadlineFailureHandler(
    private val refirePolicy: Predicate<Throwable>
) : FailureHandler<DeadlineTask.DataMap> {


    override fun onFailure(
        executionComplete: ExecutionComplete,
        executionOperations: ExecutionOperations<DeadlineTask.DataMap>
    ) {
        val failureCause = executionComplete.cause.get().cause
        if (failureCause != null && refirePolicy.test(failureCause)) {
            logger.error(
                "Exception occurred during processing a deadline job which will be retried " +
                        "[${deadlineId(executionComplete)}]",
                executionComplete.cause
            )
            executionOperations.reschedule(executionComplete, Instant.now())
        } else {
            logger.error(
                "Exception occurred during processing a deadline job [${deadlineId(executionComplete)}]",
                executionComplete.cause
            )
            executionOperations.stop()
        }
    }

    private fun deadlineId(executionComplete: ExecutionComplete) =
        executionComplete.execution.taskInstance.id

    companion object : KLogging()
}
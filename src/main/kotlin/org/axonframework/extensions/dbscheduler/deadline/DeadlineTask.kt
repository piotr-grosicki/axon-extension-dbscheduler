package org.axonframework.extensions.dbscheduler.deadline

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.deadline.DeadlineMessage
import org.axonframework.messaging.*
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork
import java.util.function.Supplier

class DeadlineTask(
    private val transactionManager: TransactionManager,
    private val scopeAwareComponents: ScopeAwareProvider,
    private val interceptors: Supplier<List<MessageHandlerInterceptor<in DeadlineMessage<Any>>>>
) {

    fun execute(taskData: Data) {
        val deadlineMessage = taskData.deadlineMessage.get()
        val deadlineScope = taskData.scopeDescriptor.get()

        val unitOfWork = DefaultUnitOfWork.startAndGet(deadlineMessage)
        unitOfWork.attachTransaction(transactionManager)

        val chain = DefaultInterceptorChain(
            unitOfWork,
            interceptors.get()
        ) { interceptedDeadlineMessage ->
            executeScheduledDeadline(
                scopeAwareComponents,
                interceptedDeadlineMessage,
                deadlineScope
            )
        }

        val result = unitOfWork.executeWithResult(chain::proceed)
        if (result.isExceptional) {
            throw result.exceptionResult()
        }
    }

    private fun executeScheduledDeadline(
        scopeAwareComponents: ScopeAwareProvider,
        deadlineMessage: DeadlineMessage<Any>,
        scopeDescriptor: ScopeDescriptor
    ) {
        scopeAwareComponents.provideScopeAwareStream(scopeDescriptor)
            .filter { scopeAwareComponent -> scopeAwareComponent.canResolve(scopeDescriptor) }
            .forEach { scopeAwareComponent ->
                try {
                    scopeAwareComponent.send(deadlineMessage, scopeDescriptor)
                } catch (e: Exception) {
                    throw ExecutionException("Execution failed for message: {}", e)
                }
            }
    }

    data class Data(
        val deadlineName: String,
        val deadlineMessage: Supplier<DeadlineMessage<Any>>,
        val scopeDescriptor: Supplier<ScopeDescriptor>
    )

    class DataMap : HashMap<String, Any?>()

}
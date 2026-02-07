package com.back.global.task.`in`

import com.back.global.task.config.TaskHandlerRegistry
import com.back.global.task.out.TaskRepository
import com.back.standard.dto.TaskPayload
import com.back.standard.util.Ut
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executors

@Component
class TaskProcessor(
    private val taskRepository: TaskRepository,
    private val taskHandlerRegistry: TaskHandlerRegistry,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = LoggerFactory.getLogger(TaskProcessor::class.java)
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    @Scheduled(fixedDelay = 100000)
    fun processTasks() {
        val taskIds = transactionTemplate.execute {
            val pendingTasks = taskRepository.findPendingTasksWithLock(10)
            pendingTasks.forEach { it.markAsProcessing() }
            pendingTasks.map { it.id }
        } ?: emptyList()

        taskIds.forEach { taskId ->
            executor.submit { executeTask(taskId) }
        }
    }

    private fun executeTask(taskId: Int) = transactionTemplate.execute {
        val task = taskRepository.findById(taskId).orElse(null) ?: return@execute

        try {
            val payloadClass = Class.forName(task.taskType)
            val payload = Ut.JSON.fromString(task.payload, payloadClass) as TaskPayload
            val handler = taskHandlerRegistry.getHandler(payload::class.java)

            if (handler != null) {
                handler.method.invoke(handler.bean, payload)
                task.markAsCompleted()
            } else {
                logger.warn("No handler found for task type: ${task.taskType}")
                task.errorMessage = "No handler found"
                task.scheduleRetry()
            }
        } catch (e: Exception) {
            val rootCause = e.cause ?: e
            logger.error("Task failed: $taskId (retry: ${task.retryCount}/${task.maxRetries})", rootCause)
            task.errorMessage = rootCause.message ?: rootCause::class.simpleName
            task.scheduleRetry()
        }
    }
}

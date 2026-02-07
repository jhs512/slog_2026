package com.back.global.task.config

import com.back.global.task.domain.TaskHandler
import com.back.standard.dto.TaskPayload
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.lang.reflect.Method

data class TaskHandlerMethod(
    val bean: Any,
    val method: Method
)

@Component
class TaskHandlerRegistry(
    private val applicationContext: ApplicationContext
) : ApplicationListener<ContextRefreshedEvent> {

    private val handlers = mutableMapOf<Class<out TaskPayload>, TaskHandlerMethod>()

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        applicationContext.beanDefinitionNames.forEach { beanName ->
            val bean = applicationContext.getBean(beanName)

            bean::class.java.methods
                .filter { it.isAnnotationPresent(TaskHandler::class.java) }
                .forEach { method ->
                    val parameterTypes = method.parameterTypes

                    if (parameterTypes.size == 1 && TaskPayload::class.java.isAssignableFrom(parameterTypes[0])) {
                        @Suppress("UNCHECKED_CAST")
                        val payloadType = parameterTypes[0] as Class<out TaskPayload>
                        handlers[payloadType] = TaskHandlerMethod(bean, method)
                    }
                }
        }
    }

    fun getHandler(payloadType: Class<out TaskPayload>): TaskHandlerMethod? {
        return handlers[payloadType]
    }
}

package com.SIGMA.USCO.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncEventConfig {

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(24);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("sigma-notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        // ponytail: CallerRunsPolicy = backpressure en vez de RejectedExecutionException;
        // el email se envía en el hilo publicador cuando la cola está llena (bloqueo aceptable).
        // Subir a cola por destinatario/mensajería si el volumen masivo lo exige.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // ponytail: multicaster síncrono para que @TransactionalEventListener(AFTER_COMMIT)
    // se ejecute en el hilo del publicador con transacción activa; el envío de correo
    // sigue siendo async en el NotificationDispatcherService (@Async).
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setErrorHandler(ex -> log.error("Error procesando evento", ex));
        return multicaster;
    }
}


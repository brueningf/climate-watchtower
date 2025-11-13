package com.audit.climate.watchtower.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // default queue used by the seeder and listener
    public static final String DEFAULT_QUEUE = "test.queue";
    public static final String ALERTS_QUEUE = "alerts.queue";
    public static final String DEFAULT_EXCHANGE = ""; // default exchange
    public static final String ALERTS_EXCHANGE = "alerts.exchange";

    @Bean
    public Queue testQueue() {
        return new Queue(DEFAULT_QUEUE, true, false, false);
    }

    @Bean
    public Queue alertsQueue() {
        return new Queue(ALERTS_QUEUE, true);
    }

    @Bean
    public DirectExchange alertsExchange() {
        return new DirectExchange(ALERTS_EXCHANGE);
    }

    @Bean
    public Binding alertsBinding(Queue alertsQueue, DirectExchange alertsExchange) {
        return BindingBuilder.bind(alertsQueue).to(alertsExchange).with("alerts.routing");
    }

    // ensure queues/exchanges are declared on broker
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}

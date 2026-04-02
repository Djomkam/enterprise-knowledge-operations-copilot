package io.innovation.ekoc.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INGESTION_QUEUE = "ekoc.ingestion.queue";
    public static final String INGESTION_EXCHANGE = "ekoc.ingestion.exchange";
    public static final String INGESTION_ROUTING_KEY = "ekoc.ingestion.routing";

    public static final String EMBEDDING_QUEUE = "ekoc.embedding.queue";
    public static final String EMBEDDING_EXCHANGE = "ekoc.embedding.exchange";
    public static final String EMBEDDING_ROUTING_KEY = "ekoc.embedding.routing";

    @Bean
    public Queue ingestionQueue() {
        return QueueBuilder.durable(INGESTION_QUEUE).build();
    }

    @Bean
    public Queue embeddingQueue() {
        return QueueBuilder.durable(EMBEDDING_QUEUE).build();
    }

    @Bean
    public TopicExchange ingestionExchange() {
        return new TopicExchange(INGESTION_EXCHANGE);
    }

    @Bean
    public TopicExchange embeddingExchange() {
        return new TopicExchange(EMBEDDING_EXCHANGE);
    }

    @Bean
    public Binding ingestionBinding(Queue ingestionQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(ingestionQueue)
                .to(ingestionExchange)
                .with(INGESTION_ROUTING_KEY);
    }

    @Bean
    public Binding embeddingBinding(Queue embeddingQueue, TopicExchange embeddingExchange) {
        return BindingBuilder.bind(embeddingQueue)
                .to(embeddingExchange)
                .with(EMBEDDING_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

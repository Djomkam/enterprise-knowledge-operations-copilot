package io.innovation.ekoc.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitMQConfig {

    public static final String INGESTION_QUEUE = "ekoc.ingestion.queue";
    public static final String INGESTION_EXCHANGE = "ekoc.ingestion.exchange";
    public static final String INGESTION_ROUTING_KEY = "ekoc.ingestion.routing";
    public static final String INGESTION_DLQ = "ekoc.ingestion.dlq";
    public static final String INGESTION_DLX = "ekoc.ingestion.dlx";

    public static final String EMBEDDING_QUEUE = "ekoc.embedding.queue";
    public static final String EMBEDDING_EXCHANGE = "ekoc.embedding.exchange";
    public static final String EMBEDDING_ROUTING_KEY = "ekoc.embedding.routing";

    public static final String ANALYTICS_QUEUE = "ekoc.analytics.queue";
    public static final String ANALYTICS_EXCHANGE = "ekoc.analytics.exchange";
    public static final String ANALYTICS_ROUTING_KEY = "ekoc.analytics.routing";

    @Bean
    public DirectExchange ingestionDeadLetterExchange() {
        return new DirectExchange(INGESTION_DLX);
    }

    @Bean
    public Queue ingestionDeadLetterQueue() {
        return QueueBuilder.durable(INGESTION_DLQ).build();
    }

    @Bean
    public Binding ingestionDlqBinding(Queue ingestionDeadLetterQueue, DirectExchange ingestionDeadLetterExchange) {
        return BindingBuilder.bind(ingestionDeadLetterQueue).to(ingestionDeadLetterExchange).with(INGESTION_DLQ);
    }

    @Bean
    public Queue ingestionQueue() {
        return QueueBuilder.durable(INGESTION_QUEUE)
                .withArgument("x-dead-letter-exchange", INGESTION_DLX)
                .withArgument("x-dead-letter-routing-key", INGESTION_DLQ)
                .build();
    }

    @Bean
    public RetryOperationsInterceptor ingestionRetryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(2000, 2.0, 10000) // initial 2s, multiplier 2x, max 10s
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAdviceChain(ingestionRetryInterceptor());
        return factory;
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
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE);
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(analyticsExchange)
                .with(ANALYTICS_ROUTING_KEY);
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

package ar.edu.utn.dds.k3003.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

    public static final String EXCHANGE = "logistica.exchange";
    public static final String QUEUE = "logistica.matchmaking";
    public static final String ROUTING_KEY = "logistica.donacion.pendiente";

    @Bean
    public DirectExchange logisticaExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue matchmakingQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding matchmakingBinding(
        Queue matchmakingQueue,
        DirectExchange logisticaExchange
    ) {
        return BindingBuilder.bind(matchmakingQueue)
            .to(logisticaExchange)
            .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}

package ar.edu.utn.dds.k3003.messaging;

import ar.edu.utn.dds.k3003.messaging.dto.DonacionPendienteMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DonacionProducer {

    private final RabbitTemplate rabbitTemplate;

    public DonacionProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(DonacionPendienteMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitConfiguration.EXCHANGE,
            RabbitConfiguration.ROUTING_KEY,
            message
        );
    }
}

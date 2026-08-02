package ar.edu.utn.dds.k3003.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DonacionRegistradaListener {

    private final DonacionProducer producer;

    public DonacionRegistradaListener(DonacionProducer producer) {
        this.producer = producer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(DonacionRegistradaEvent event) {
        producer.publicar(event.message());
    }
}

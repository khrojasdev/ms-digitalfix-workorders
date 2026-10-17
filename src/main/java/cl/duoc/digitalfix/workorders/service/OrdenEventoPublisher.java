package cl.duoc.digitalfix.workorders.service;

import cl.duoc.digitalfix.workorders.web.dto.OrdenEventoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrdenEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrdenEventoPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${amqp.exchange.negocio}")
    private String exchange;

    @Value("${amqp.routing-key.eventos}")
    private String routingKey;

    public OrdenEventoPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarEvento(OrdenEventoDto evento) {
        try {
            String mensajeJson = objectMapper.writeValueAsString(evento);
            rabbitTemplate.convertAndSend(exchange, routingKey, mensajeJson);
            log.info("Evento publicado en RabbitMQ: Orden {} - {}", evento.ordenId(), evento.tipoEvento());
        } catch (JsonProcessingException e) {
            log.error("Error al convertir el evento a JSON para RabbitMQ", e);
        }
    }
}
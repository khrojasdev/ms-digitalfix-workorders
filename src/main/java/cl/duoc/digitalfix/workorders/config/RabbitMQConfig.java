package cl.duoc.digitalfix.workorders.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange donde este servicio publica los eventos de las ordenes.
 *
 * Hasta ahora solo lo declaraban los consumidores (audit, report y notify) al
 * conectarse. Si workorders publicaba antes de que alguno de ellos se hubiera
 * conectado al broker, el exchange no existia: RabbitMQ cerraba el canal con
 * "404 NOT_FOUND - no exchange 'ordenes.negocio.exchange'" y el evento se perdia.
 *
 * Con este bean, RabbitAdmin lo declara apenas workorders abre su conexion. La
 * declaracion es idempotente y usa los mismos parametros que los consumidores
 * (topic, durable, sin borrado automatico), asi que no choca con la de ellos.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange negocioExchange(@Value("${amqp.exchange.negocio}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }
}

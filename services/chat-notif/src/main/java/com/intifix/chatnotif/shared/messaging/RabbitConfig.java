package com.intifix.chatnotif.shared.messaging;

import static com.intifix.chatnotif.shared.messaging.Eventos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Topología RabbitMQ de este servicio. Cada cola tiene dead-letter: si un
 * mensaje falla tras los reintentos, termina en {@value Eventos#Q_DEAD_LETTER}
 * en vez de perderse o reintentarse para siempre.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Declarables topologia() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetter = QueueBuilder.durable(Q_DEAD_LETTER).build();

        List<Declarable> declarables = new ArrayList<>(List.of(exchange, dlx, deadLetter,
                BindingBuilder.bind(deadLetter).to(dlx).with("chat-notif.#")));

        Map<String, String> colas = Map.of(
                Q_TECNICO_APROBADO, TECNICO_APROBADO,
                Q_TECNICO_RECHAZADO, TECNICO_RECHAZADO,
                Q_CERTIFICADO_APROBADO, CERTIFICADO_APROBADO,
                Q_CERTIFICADO_RECHAZADO, CERTIFICADO_RECHAZADO);
        colas.forEach((cola, routingKey) -> {
            Queue queue = QueueBuilder.durable(cola)
                    .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(cola)
                    .build();
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
            declarables.add(queue);
            declarables.add(binding);
        });
        return new Declarables(declarables);
    }

    /**
     * JSON sin depender de nombres de clase: el tipo se deduce del parámetro
     * del @RabbitListener, así cada servicio puede tener su propia copia del
     * record del evento.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}

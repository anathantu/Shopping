package com.hust.tutu.rabbitmq;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;


@Configuration
public class MQConfig {

    public static final String MIAOSHA_QUEUE = "miaosha.queue";

    @Bean
    public Queue queue(){
        return new Queue(MIAOSHA_QUEUE,true);
    }
}

package com.alibaba.jvm.sandbox.module;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitTemplate {

    private static final RabbitTemplate rabbitTemplate = new RabbitTemplate();
    private static ConnectionFactory factory = new ConnectionFactory();

    private RabbitTemplate(){}

    public String host;

    public int port;

    public String password;

    public String username;

    public String virtual_host;

    static {
        rabbitTemplate.host = (String) YamlReader.getValueByKey("rabbitmq.host");
        rabbitTemplate.password = (String) YamlReader.getValueByKey("rabbitmq.password");
        rabbitTemplate.port = (int) YamlReader.getValueByKey("rabbitmq.port");
        rabbitTemplate.username = (String) YamlReader.getValueByKey("rabbitmq.username");
        rabbitTemplate.virtual_host = (String) YamlReader.getValueByKey("rabbitmq.virtual-host");
        factory.setUsername(rabbitTemplate.host);
        factory.setPassword(rabbitTemplate.password);
        factory.setVirtualHost(rabbitTemplate.virtual_host);
        factory.setHost(rabbitTemplate.host);
        factory.setPort(rabbitTemplate.port);
    }

    public static RabbitTemplate getInstance(){
        return rabbitTemplate;
    }


    public void sendMessage(String message) throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        try {
            // 声明一个队列
            channel.queueDeclare("sql",true,false,false,null);
            // 发送消息  MessageProperties.PERSISTENT_TEXT_PLAIN 消息持久化
            channel.basicPublish("","sql", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            if (channel.isOpen()){
                channel.close();
            }
            if (connection.isOpen()){
                connection.close();
            }
        }
    }

}

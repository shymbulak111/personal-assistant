package kz.projem.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_AUDIT = "assistant.audit";
    public static final String TOPIC_REMINDERS = "assistant.reminders";
    public static final String TOPIC_NOTIFICATIONS = "assistant.notifications";

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(TOPIC_AUDIT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic remindersTopic() {
        return TopicBuilder.name(TOPIC_REMINDERS)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS)
                .partitions(2)
                .replicas(1)
                .build();
    }
}

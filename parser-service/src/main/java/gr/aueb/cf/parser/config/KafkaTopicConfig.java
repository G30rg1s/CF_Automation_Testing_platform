package gr.aueb.cf.parser.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder.name("document-uploaded")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentParsedTopic() {
        return TopicBuilder.name("document-parsed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic documentParseFailedTopic() {
        return TopicBuilder.name("document-parse-failed")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
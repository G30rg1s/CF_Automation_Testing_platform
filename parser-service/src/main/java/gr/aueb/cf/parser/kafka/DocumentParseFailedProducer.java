
package gr.aueb.cf.parser.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.cf.parser.dto.DocumentParseFailedDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentParseFailedProducer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentParseFailedProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DocumentParseFailedProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(DocumentParseFailedDto failedDto)
            throws JsonProcessingException {

        log.info(
                "PARSER KAFKA | Preparing document-parse-failed event"
        );

        String message;

        try {

            message = objectMapper.writeValueAsString(failedDto);

        } catch (JsonProcessingException e) {

            log.error(
                    "PARSER KAFKA | Failed to serialize document-parse-failed event",
                    e
            );

            throw e;
        }

        try {

            kafkaTemplate.send("document-parse-failed", message)
                    .whenComplete((sendResult, exception) -> {

                        if (exception != null) {

                            log.error(
                                    "PARSER KAFKA | Failed to publish document-parse-failed event",
                                    exception
                            );

                        } else {

                            log.info(
                                    "PARSER KAFKA | document-parse-failed event published successfully | Topic: {} | Partition: {} | Offset: {}",
                                    sendResult.getRecordMetadata().topic(),
                                    sendResult.getRecordMetadata().partition(),
                                    sendResult.getRecordMetadata().offset()
                            );
                        }
                    });

            log.info(
                    "PARSER KAFKA | document-parse-failed event submitted for publishing"
            );

        } catch (RuntimeException e) {

            log.error(
                    "PARSER KAFKA | Could not submit document-parse-failed event",
                    e
            );

            throw e;
        }
    }
}

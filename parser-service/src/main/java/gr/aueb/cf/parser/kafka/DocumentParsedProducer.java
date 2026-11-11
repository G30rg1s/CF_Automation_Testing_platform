
package gr.aueb.cf.parser.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.aueb.cf.parser.dto.ParsedDocumentResultDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DocumentParsedProducer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentParsedProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DocumentParsedProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(ParsedDocumentResultDto result)
            throws JsonProcessingException {

        log.info(
                "PARSER KAFKA | Preparing document-parsed event"
        );

        String message;

        try {

            message = objectMapper.writeValueAsString(result);

        } catch (JsonProcessingException e) {

            log.error(
                    "PARSER KAFKA | Failed to serialize document-parsed event",
                    e
            );

            throw e;
        }

        try {

            kafkaTemplate.send("document-parsed", message)
                    .whenComplete((sendResult, exception) -> {

                        if (exception != null) {

                            log.error(
                                    "PARSER KAFKA | Failed to publish document-parsed event",
                                    exception
                            );

                        } else {

                            log.info(
                                    "PARSER KAFKA | document-parsed event published successfully | Topic: {} | Partition: {} | Offset: {}",
                                    sendResult.getRecordMetadata().topic(),
                                    sendResult.getRecordMetadata().partition(),
                                    sendResult.getRecordMetadata().offset()
                            );
                        }
                    });

            log.info(
                    "PARSER KAFKA | document-parsed event submitted for publishing"
            );

        } catch (RuntimeException e) {

            log.error(
                    "PARSER KAFKA | Could not submit document-parsed event",
                    e
            );

            throw e;
        }
    }
}

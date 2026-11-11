
package gr.aueb.cf.automation.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.aueb.cf.automation.dto.documents.DocumentParseRequestDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentUploadedProducer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentUploadedProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DocumentUploadedProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(DocumentParseRequestDto request)
            throws JsonProcessingException {

        log.info(
                "AUTOMATION KAFKA | Preparing document-uploaded event"
        );

        String message;

        try {

            message = objectMapper.writeValueAsString(request);

        } catch (JsonProcessingException e) {

            log.error(
                    "AUTOMATION KAFKA | Failed to serialize document-uploaded event",
                    e
            );

            throw e;
        }

        try {

            kafkaTemplate.send("document-uploaded", message)
                    .whenComplete((sendResult, exception) -> {

                        if (exception != null) {

                            log.error(
                                    "AUTOMATION KAFKA | Failed to publish document-uploaded event",
                                    exception
                            );

                        } else {

                            log.info(
                                    "AUTOMATION KAFKA | document-uploaded event published successfully | Topic: {} | Partition: {} | Offset: {}",
                                    sendResult.getRecordMetadata().topic(),
                                    sendResult.getRecordMetadata().partition(),
                                    sendResult.getRecordMetadata().offset()
                            );
                        }
                    });

            log.info(
                    "AUTOMATION KAFKA | document-uploaded event submitted for publishing"
            );

        } catch (RuntimeException e) {

            log.error(
                    "AUTOMATION KAFKA | Could not submit document-uploaded event",
                    e
            );

            throw e;
        }
    }
}

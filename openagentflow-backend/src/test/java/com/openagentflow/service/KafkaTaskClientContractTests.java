package com.openagentflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openagentflow.domain.task.AsyncTaskMessage;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Kafka任务消息Schema兼容契约测试。 */
class KafkaTaskClientContractTests {

    /** 当前Schema应可解析，未来未知Schema必须拒绝。 */
    @Test
    void shouldEnforceSchemaVersion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        KafkaTaskClient client = new KafkaTaskClient(mock(KafkaTemplate.class), mapper, mock(AsyncTaskOutboxService.class));
        AsyncTaskMessage message = new AsyncTaskMessage();
        message.setSchemaVersion(1);
        message.setTaskId("task-1");
        message.setTaskType("DOCUMENT_PIPELINE_PARSE");
        assertEquals("task-1", client.parse(mapper.writeValueAsString(message)).getTaskId());
        message.setSchemaVersion(99);
        assertThrows(IllegalArgumentException.class, () -> client.parse(mapper.writeValueAsString(message)));
    }
}

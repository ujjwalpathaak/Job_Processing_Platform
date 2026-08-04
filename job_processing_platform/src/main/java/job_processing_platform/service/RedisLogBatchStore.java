package job_processing_platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import job_processing_platform.dto.LogMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisLogBatchStore implements LogBatchStore {

    private static final String BATCH_KEY_PREFIX = "log-batch:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisLogBatchStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(String requestId, LogMessage logMessage) {
        if (requestId == null || requestId.isBlank() || logMessage == null) {
            return;
        }
        try {
            redisTemplate.opsForList().rightPush(batchKey(requestId), objectMapper.writeValueAsString(logMessage));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize log message for Redis batching", e);
        }
    }

    @Override
    public List<LogMessage> drain(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return List.of();
        }

        String key = batchKey(requestId);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size <= 0) {
            return List.of();
        }

        List<String> rawValues = redisTemplate.opsForList().range(key, 0, size - 1);
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }

        List<LogMessage> messages = new ArrayList<>();
        for (String raw : rawValues) {
            try {
                messages.add(objectMapper.readValue(raw, LogMessage.class));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize log message from Redis batching", e);
            }
        }

        redisTemplate.delete(key);
        return messages;
    }

    @Override
    public int size(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return 0;
        }
        Long size = redisTemplate.opsForList().size(batchKey(requestId));
        return size == null ? 0 : Math.toIntExact(size);
    }

    @Override
    public boolean isEmpty(String requestId) {
        return size(requestId) == 0;
    }

    private String batchKey(String requestId) {
        return BATCH_KEY_PREFIX + requestId;
    }
}

package job_processing_platform.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DtoConversionService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> convertDtoToMap(Object dto) {
        return objectMapper.convertValue(dto, Map.class);
    }
}

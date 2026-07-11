package com.msc.springai.service.observability;

import com.msc.springai.entity.AiRequestLog;
import com.msc.springai.mapper.AiRequestLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiRequestLogWriter {

    private final AiRequestLogMapper aiRequestLogMapper;

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void write(AiRequestLog requestLog) {
        aiRequestLogMapper.insert(requestLog);
    }
}
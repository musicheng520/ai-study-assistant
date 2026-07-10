package com.msc.springai.service.tool;

import com.msc.springai.dto.workflow.tool.StudyToolDtos.DocumentSearchToolResult;

import java.util.List;

public interface DocumentSearchToolGateway {

    List<DocumentSearchToolResult> searchFromVectorStore(Long userId,
                                                         Long courseId,
                                                         String query,
                                                         int topK,
                                                         Long documentId);
}
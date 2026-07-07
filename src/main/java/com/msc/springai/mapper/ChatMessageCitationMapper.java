package com.msc.springai.mapper;

import com.msc.springai.entity.ChatMessageCitation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageCitationMapper {

    @Insert("""
        INSERT INTO chat_message_citations (
            message_id,
            document_id,
            chunk_id,
            file_name,
            page_number,
            section_title,
            snippet
        ) VALUES (
            #{messageId},
            #{documentId},
            #{chunkId},
            #{fileName},
            #{pageNumber},
            #{sectionTitle},
            #{snippet}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessageCitation citation);

    default int insertBatch(List<ChatMessageCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return 0;
        }

        int count = 0;

        for (ChatMessageCitation citation : citations) {
            count += insert(citation);
        }

        return count;
    }

    @Select("""
        SELECT
            id,
            message_id AS messageId,
            document_id AS documentId,
            chunk_id AS chunkId,
            file_name AS fileName,
            page_number AS pageNumber,
            section_title AS sectionTitle,
            snippet,
            created_at AS createdAt
        FROM chat_message_citations
        WHERE message_id = #{messageId}
        ORDER BY id ASC
    """)
    List<ChatMessageCitation> findByMessageId(Long messageId);
}
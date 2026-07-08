package com.msc.springai.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DraftKeyInfo {

    private String type;

    private Long userId;

    private Long courseId;

    private String scope;

    private String paramsHash;
}
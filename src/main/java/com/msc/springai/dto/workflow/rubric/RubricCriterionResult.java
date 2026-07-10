package com.msc.springai.dto.workflow.rubric;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RubricCriterionResult {

    private String name;

    private String weight;

    private String description;
}
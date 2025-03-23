package org.example;

import java.util.List;
import java.util.Map;

public class EvaluationOutputEntity extends OutputEntity {

    public EvaluationOutputEntity(List<Map<String, Object>> items) {
        super(items, EvaluationOutputEntity.class.getSimpleName());
    }
}

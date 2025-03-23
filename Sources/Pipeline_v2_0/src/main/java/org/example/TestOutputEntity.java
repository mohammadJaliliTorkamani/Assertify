package org.example;

import java.util.List;
import java.util.Map;

public class TestOutputEntity extends OutputEntity {

    public TestOutputEntity(List<Map<String, Object>> items) {
        super(items, TestOutputEntity.class.getSimpleName());
    }
}

package org.example;

import java.util.Set;

public interface Filter {
    Set<String> apply(Set<String> urls);
}

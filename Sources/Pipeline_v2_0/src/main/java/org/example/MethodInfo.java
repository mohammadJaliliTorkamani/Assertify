package org.example;

import java.util.Objects;

public class MethodInfo {
    private final String methodSignature;
    private final String methodClassName;
    private final String methodName;
    private final String methodPath;

    public MethodInfo(String methodSignature, String methodClassName, String methodName, String methodPath) {
        this.methodSignature = methodSignature;
        this.methodClassName = methodClassName;
        this.methodName = methodName;
        this.methodPath = methodPath;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getMethodClassName() {
        return methodClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodPath() {
        return methodPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return Objects.equals(methodSignature, that.methodSignature) &&
                Objects.equals(methodClassName, that.methodClassName) &&
                Objects.equals(methodName, that.methodName) &&
                Objects.equals(methodPath, that.methodPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodSignature, methodClassName, methodName, methodPath);
    }
}

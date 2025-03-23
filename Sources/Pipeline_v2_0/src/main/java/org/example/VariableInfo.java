package org.example;

public class VariableInfo {
    private String name;
    private String type;
    private String methodSignature;
    private String classNAme;

    public VariableInfo(String name, String type, String methodSignature, String classNAme) {
        this.name = name;
        this.type = type;
        this.methodSignature = methodSignature;
        this.classNAme = classNAme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getClassNAme() {
        return classNAme;
    }

    public void setClassNAme(String classNAme) {
        this.classNAme = classNAme;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + type.hashCode() + methodSignature.hashCode() + classNAme.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VariableInfo other = (VariableInfo) obj;
        return name.equals(other.name) && type.equals(other.type) && methodSignature.equals(other.methodSignature) &&
                classNAme.equals(other.classNAme);
    }
}

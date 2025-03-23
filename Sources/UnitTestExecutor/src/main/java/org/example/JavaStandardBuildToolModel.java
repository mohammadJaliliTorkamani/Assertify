package org.example;

public class JavaStandardBuildToolModel extends BuildToolModel {
    @Override
    public boolean installDependencies() {
        return false;
    }

    @Override
    public boolean runTestCase(String testFile, String testCase) {
        return false;
    }
}

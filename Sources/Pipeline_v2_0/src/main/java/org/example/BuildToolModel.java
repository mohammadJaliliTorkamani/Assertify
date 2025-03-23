package org.example;

import org.example.ComponentResponse.Status;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BuildToolModel {
    public static final int MAX_TEST_LENGTH = 50;

    public static BuildToolModel getProjectBuildToolModel(String repositoryPath) {
        if (hasPomXml(repositoryPath))
            return new MavenBuildToolModel(repositoryPath);
        else if (hasBuildGradle(repositoryPath))
            return new GradleBuildToolModel(repositoryPath);
        return null;
    }

    public static boolean hasPomXml(String repositoryPath) {
        String pomXmlPath = repositoryPath + "/pom.xml";
        return Files.exists(Paths.get(pomXmlPath));
    }

    public static boolean hasBuildGradle(String repositoryPath) {
        String buildGradlePath = repositoryPath + "/build.gradle";
        String settingsGradlePath = repositoryPath + "/settings.gradle";
        String gradlewPath = repositoryPath + "/gradlew";
        return Files.exists(Paths.get(buildGradlePath)) ||
                Files.exists(Paths.get(settingsGradlePath)) ||
                Files.exists(Paths.get(gradlewPath));
    }

    public static ComponentResponse runShellCommand(String[] commands, String dir, String javaAddress) throws Exception {
        StringBuilder totalOutput = new StringBuilder();
        String[] javaAddresses = javaAddress != null ? new String[]{javaAddress} : Constants.JAVA_HOME_VERSIONS;
        for (String _javaAddress : javaAddresses) {
            System.out.println("     Candidate Java: " + _javaAddress);
            formatWithCoveoIfRequired(dir,javaAddress);
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(dir));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.environment().put("JAVA_HOME", _javaAddress);
            String javaBinPath = processBuilder.environment().get("JAVA_HOME") + "/bin";
            processBuilder.environment().put("PATH", javaBinPath + ":" + processBuilder.environment().get("PATH"));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = processBuilder.start();
            totalOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalOutput.append(line).append(" \n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0)
                return new ComponentResponse(Status.OK, totalOutput.toString(), String.join(" ", commands), _javaAddress);
            else
                System.out.println("         Failed to succeed when running shell command with the associated Java version");
        }

        return
                new ComponentResponse(Status.ERROR_OCCURRED,
                        totalOutput.toString(),
                        String.join(" ", commands),
                        Constants.JAVA_HOME_VERSIONS[0]);
    }

    private static void formatWithCoveoIfRequired(String dir, String javaAddresses) {
        if(Utils.containsCoveo(dir)){
            try {
                String[] commands = new String[]{Constants.MVN_BINARY_PATH, "com.coveo:fmt-maven-plugin:format"};
                ProcessBuilder processBuilder = new ProcessBuilder(commands);
                processBuilder.directory(new File(dir));
                processBuilder.environment().putAll(System.getenv());
                if(javaAddresses!=null) {
                    processBuilder.environment().put("JAVA_HOME", javaAddresses);
                    String javaBinPath = processBuilder.environment().get("JAVA_HOME") + "/bin";
                    processBuilder.environment().put("PATH", javaBinPath + ":" + processBuilder.environment().get("PATH"));
                }
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
                Process process = processBuilder.start();
            }catch (Exception e){}
        }
    }

    private static String[] makeCommandJavaVersionExclusive(String[] commands, String javaHomeAddress) {
        //Todo check if this is really required
//        List<String> list = new ArrayList<>();
//        for (int i = 0; i < commands.length; i++) {
//            list.add(commands[i]);
//            if (i == 1) {
//                System.out.println("JAVA_HOME = " + javaHomeAddress);
//                list.add("set");
//                list.add("JAVA_HOME=" + javaHomeAddress);
//                list.add("&&");
//            }
//        }
//        System.setProperty("java.home", javaHomeAddress);

        return commands;//list.toArray(new String[0]);
    }

    private static String getRepoNameFromDir(String dir) {
        String[] dirs = dir.split("/");
        for (int i = dirs.length - 1; i >= 0; i--)
            if (dirs[i].contains("@"))
                return dirs[i];
        return null;
    }

    private static TestResult extractTestResult(String buildTypeModel, String str, String command) {
        if (buildTypeModel.equals("MavenBuildToolModel")) {
            String pattern = "Tests\\srun:\\s(\\d+),\\sFailures:\\s(\\d+),\\sErrors:\\s(\\d+),\\sSkipped: (\\d+)";
            Pattern regexPattern = Pattern.compile(pattern);
            Matcher matcher = regexPattern.matcher(str);
            int testsRun = -1;
            int failures = -1;
            int errors = -1;
            int skipped = -1;

            while (matcher.find()) {
                testsRun = Integer.parseInt(matcher.group(1));
                failures = Integer.parseInt(matcher.group(2));
                errors = Integer.parseInt(matcher.group(3));
                skipped = Integer.parseInt(matcher.group(4));
            }

            return (testsRun == -1 && failures == -1 && errors == -1 && skipped == -1) ? null :
                    new TestResult(testsRun, failures, errors, skipped);
        } else if (buildTypeModel.equals("GradleBuildToolModel")) {//based on the fact that the output of gradle includes the number of total/and failed tests if there is one failed case. otherwise, there is no output (so we have to count --tests to know how many total tests we had)
            String pattern_single = "(\\d+)\\stest\\scompleted,\\s(\\d+)\\sfailed";
            String pattern_plural = "(\\d+)\\stests\\scompleted,\\s(\\d+)\\sfailed";
            Pattern regexPattern_single = Pattern.compile(pattern_single);
            Pattern regexPattern_plural = Pattern.compile(pattern_plural);
            Matcher matcher_single = regexPattern_single.matcher(str);
            Matcher matcher_plural = regexPattern_plural.matcher(str);
            int testsRun = -1;
            int failures = -1;
            if (matcher_single.matches()) {
                while (matcher_single.find()) {
                    testsRun = Integer.parseInt(matcher_single.group(1));
                    failures = Integer.parseInt(matcher_single.group(2));
                }
            } else if (matcher_plural.matches()) {
                while (matcher_plural.find()) {
                    testsRun = Integer.parseInt(matcher_plural.group(1));
                    failures = Integer.parseInt(matcher_plural.group(2));
                }
            }

            if (Utils.countOccurrences(str, Pattern.compile("BUILD\\sFAILED\\sin")) == 0) {
                if (testsRun != -1 && failures != -1) {
                    return new TestResult(testsRun, failures, 0, 0);
                } else if (testsRun == -1 && failures == -1)
                    return new TestResult(Utils.countOccurrences(command, Pattern.compile("--tests")), 0, 0, 0);
            } else {
                if (testsRun != -1 && failures != -1)
                    return new TestResult(testsRun, failures, 0, 0);
            }
        }
        return null;
    }

    public TestResult extractTestExecutionStatistic(String output, String command) {
        return extractTestResult(getClass().getSimpleName(), output, command);
    }

    public abstract ComponentResponse compile() throws Exception;

    public abstract ComponentResponse compile(boolean clean, String javaAddress) throws Exception;

    public abstract ComponentResponse runTestCase(String[] testFiles, String[] testCases, String javaAddress) throws Exception;
}

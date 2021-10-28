package dev.jeka.plugins.sonarqube;


import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Sonar wrapper class for launching sonar analysis in a convenient way. This
 * Sonar wrapper is not specific to Java project so can be used for to analyse
 * any kind of project supported by SonarQube.
 *
 * @author Jerome Angibaud
 */
public final class JkSonarqube {

    public static final String PROJECT_KEY = "projectKey";
    public static final String PROJECT_NAME = "projectName";
    public static final String PROJECT_VERSION = "projectVersion";
    public static final String LANGUAGE = "language";
    public static final String PROFILE = "profile";
    public static final String BRANCH = "branch";
    public static final String SOURCE_ENCODING = "sourceEncoding";
    public static final String VERBOSE = "verbose";
    public static final String WORKING_DIRECTORY = "working.directory";
    public static final String JUNIT_REPORTS_PATH = "junit.reportsPath";
    public static final String SUREFIRE_REPORTS_PATH = "surefire.reportsPath";
    public static final String JACOCO_LEGACY_REPORTS_PATHS = "jacoco.reportPaths";
    public static final String JACOCO_XML_REPORTS_PATHS = "coverage.jacoco.xmlReportPaths";
    public static final String COVERTURA_REPORTS_PATH = "cobertura.reportPath";
    public static final String CLOVER_REPORTS_PATH = "clover.reportPath";
    public static final String DYNAMIC_ANALYSIS = "dynamicAnalysis";
    public static final String PROJECT_BASE_DIR = "projectBaseDir";
    public static final String SOURCES = "sources";
    public static final String BINARIES = "binaries";
    public static final String JAVA_BINARIES = "java.binaries";
    public static final String TEST = "tests";
    public static final String LIBRARIES = "libraries";
    public static final String JAVA_LIBRARIES = "java.libraries";
    public static final String JAVA_TEST_LIBRARIES = "java.test.libraries";
    public static final String JAVA_TEST_BINARIES = "java.test.binaries";
    public static final String SKIP_DESIGN = "skipDesign";
    public static final String HOST_URL = "host.url";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    private static final String RUNNER_JAR_NAME_24 = "sonar-runner-2.4.jar";
    private static final String SCANNER_JAR_NAME_46 = "sonar-scanner-cli-4.6.2.2472.jar";
    private static final String SONAR_PREFIX = "sonar.";

    private final Map<String, String> params = new HashMap<>();

    public static JkSonarqube of(String projectKey, String projectName, String version) {
        final Map<String, String> map = new HashMap<>();
        map.put(PROJECT_KEY, projectKey);
        map.put(PROJECT_NAME, projectName);
        map.put(PROJECT_VERSION, version);
        map.put(WORKING_DIRECTORY, ".sonarTempDir");
        map.put(VERBOSE, Boolean.toString(JkLog.Verbosity.VERBOSE == JkLog.verbosity()));
        final Properties properties = System.getProperties();
        for (final Object keyObject : properties.keySet()) {
            final String key = (String) keyObject;
            if (key.startsWith(SONAR_PREFIX)) {
                map.put(key.substring(SONAR_PREFIX.length()), properties.getProperty(key));
            }
        }
        return new JkSonarqube().setProperties(map);
    }

    // Runner is the legacy client
    public void launchRunner() {
        launch(RUNNER_JAR_NAME_24, "org.sonar.runner.Main");
    }

    // Scanner replaces legacy Runner client
    public void launchScanner() {
        launch(SCANNER_JAR_NAME_46, "org.sonarsource.scanner.cli.Main");
    }

    private void launch(String resourcePath, String mainClassName) {
        JkLog.startTask("Launch Sonar analysis wih " + resourcePath);
        String[] args = JkLog.isVerbose() ? new String[] {"-e", "-X"} : new String[] {"-e"};
        javaProcess(resourcePath, mainClassName).exec(args);
        JkLog.endTask();
    }

    private JkJavaProcess javaProcess(String jarResourcePath, String mainClassName) {
        URL embeddedUrl = JkSonarqube.class.getResource(jarResourcePath);
        Path cachedUrl = JkUtilsIO.copyUrlContentToCacheFile(embeddedUrl, null, JkInternalClassloader.URL_CACHE_DIR);
        return JkJavaProcess.ofJava(mainClassName)
                .setClasspath(cachedUrl)
                .setFailOnError(true)
                .addParams(toProperties())
                .setLogCommand(JkLog.isVerbose())
                .setLogOutput(JkLog.isVerbose());
    }

    private List<String> toProperties() {
        final List<String> result = new LinkedList<>();
        for (final Map.Entry<String, String> entry : this.params.entrySet()) {
            result.add("-Dsonar." + entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    public JkSonarqube setProperty(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    public JkSonarqube setProperty(String key, Iterable<Path> value) {
        return setProperty(key, toPaths(value));
    }

    public JkSonarqube setProperties(Map<String, String> props) {
        this.params.putAll(props);
        return this;
    }

    public JkSonarqube setProjectBaseDir(Path baseDir) {
        return setProperty(PROJECT_BASE_DIR, baseDir.toAbsolutePath().toString());
    }

    public JkSonarqube setBinaries(Iterable<Path> files) {
        String path = toPaths(JkUtilsPath.disambiguate(files));
        return setProperty(BINARIES, path).setProperty(JAVA_BINARIES, path);
    }

    public JkSonarqube setBinaries(Path... files) {
        return setBinaries(Arrays.asList(files));
    }

    public JkSonarqube setSkipDesign(boolean skip) {
        return setProperty(SKIP_DESIGN, Boolean.toString(skip));
    }

    public JkSonarqube setHostUrl(String url) {
        return setProperty(HOST_URL, url);
    }

    public JkSonarqube setJdbcUrl(String url) {
        return setProperty(JDBC_URL, url);
    }

    public JkSonarqube setJdbcUserName(String userName) {
        return setProperty(JDBC_USERNAME, userName);
    }

    public JkSonarqube setJdbcPassword(String pwd) {
        return setProperty(JDBC_PASSWORD, pwd);
    }

    private String toPaths(Iterable<Path> files) {
        final Iterator<Path> it = JkUtilsPath.disambiguate(files).iterator();
        final StringBuilder result = new StringBuilder();
        final Path projectDir = projectDir();
        while (it.hasNext()) {
            final Path file = it.next();
            String path;
            if (file.startsWith(projectDir)) {
                path = projectDir.relativize(file).toString();
            } else {
                path = file.toAbsolutePath().toString();
            }
            result.append(path);
            if (it.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    private Path projectDir() {
        return Paths.get(this.params.get(PROJECT_BASE_DIR));
    }

}

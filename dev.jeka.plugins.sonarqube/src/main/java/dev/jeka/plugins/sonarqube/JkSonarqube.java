package dev.jeka.plugins.sonarqube;


import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.java.JkInternalClassloader;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.JkConstants;

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
    public static final String JACOCO_REPORTS_PATHS = "jacoco.reportPaths";
    public static final String COVERTURA_REPORTS_PATH = "cobertura.reportPath";
    public static final String CLOVER_REPORTS_PATH = "clover.reportPath";
    public static final String DYNAMIC_ANALYSIS = "dynamicAnalysis";
    public static final String PROJECT_BASE_DIR = "projectBaseDir";
    public static final String SOURCES = "sources";
    public static final String BINARIES = "binaries";
    public static final String JAVA_BINARIES = "java.binaries";
    public static final String TEST = "tests";
    public static final String LIBRARIES = "libraries";
    public static final String SKIP_DESIGN = "skipDesign";
    public static final String HOST_URL = "host.url";
    public static final String JDBC_URL = "jdbc.url";
    public static final String JDBC_USERNAME = "jdbc.username";
    public static final String JDBC_PASSWORD = "jdbc.password";
    private static final String RUNNER_JAR_NAME_24 = "sonar-runner-2.4.jar";
    private static final String SCANNER_JAR_NAME_46 = "sonar-scanner-cli-4.6.2.2472.jar";
    private static final String RUNNER_LOCAL_PATH = JkConstants.OUTPUT_PATH + "/temp/" + RUNNER_JAR_NAME_24;
    private static final String SONAR_PREFIX = "sonar.";
    private final Map<String, String> params;

    private final boolean enabled;

    private JkSonarqube(Map<String, String> params, boolean enabled) {
        super();
        this.params = Collections.unmodifiableMap(params);
        this.enabled = enabled;
    }

    public static JkSonarqube of(String projectKey, String projectName, String version) {
        JkUtilsAssert.argument(projectName != null, "Project name can't be null.");
        JkUtilsAssert.argument(projectKey != null, "Project key can't be null.");
        JkUtilsAssert.argument(version != null, "Project version can't be null.");
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
        return new JkSonarqube(map, true);
    }

    public void launchRunner() {
        if (!enabled) {
            JkLog.info("Sonar analysis skipped.");
        }
        JkLog.startTask("Launch Sonar analysis wih sonar-runner.");
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            javaRunnerProcess().runClassSync("org.sonar.runner.Main", "-e", "-X");
        } else {
            javaRunnerProcess().runClassSync("org.sonar.runner.Main", "-e");
        }
        JkLog.endTask();
    }

    public void launchScanner() {
        if (!enabled) {
            JkLog.info("Sonar analysis skipped.");
        }
        JkLog.startTask("Launch Sonar analysis with sonar-scanner");
        if (JkLog.verbosity() == JkLog.Verbosity.VERBOSE) {
            javaScannerProcess().runClassSync("org.sonar.runner.Main", "-e", "-X");
        } else {
            javaScannerProcess().runClassSync("org.sonar.runner.Main", "-e");
        }
        JkLog.endTask();
    }

    public JkSonarqube enabled(boolean enabled) {
        return new JkSonarqube(this.params, enabled);
    }

    // Runner is the legacy cliennt
    private JkJavaProcess javaRunnerProcess() {
        URL embeddedUrl = JkSonarqube.class.getResource(RUNNER_JAR_NAME_24);
        Path cachedUrl = JkUtilsIO.copyUrlContentToCacheFile(embeddedUrl, null, JkInternalClassloader.URL_CACHE_DIR);
        return JkJavaProcess.of().withClasspath(cachedUrl).andOptions(toProperties());
    }

    // Scanner replaces legacy Runner client
    private JkJavaProcess javaScannerProcess() {
        URL embeddedUrl = JkSonarqube.class.getResource(SCANNER_JAR_NAME_46);
        Path cachedUrl = JkUtilsIO.copyUrlContentToCacheFile(embeddedUrl, null, JkInternalClassloader.URL_CACHE_DIR);
        return JkJavaProcess.of().withClasspath(cachedUrl).andOptions(toProperties());
    }

    private List<String> toProperties() {
        final List<String> result = new LinkedList<>();
        for (final Map.Entry<String, String> entry : this.params.entrySet()) {
            result.add("-Dsonar." + entry.getKey() + "=" + entry.getValue());
        }
        return result;
    }

    public JkSonarqube withProperty(String key, String value) {
        return new JkSonarqube(andParams(key, value), enabled);
    }

    public JkSonarqube withProperties(Map<String, String> props) {
        final Map<String, String> newProps = new HashMap<>(this.params);
        newProps.putAll(props);
        return new JkSonarqube(newProps, enabled);
    }

    public JkSonarqube withProjectBaseDir(Path baseDir) {
        return withProperty(PROJECT_BASE_DIR, baseDir.toAbsolutePath().toString());
    }

    public JkSonarqube withSourcesPath(Iterable<Path> files) {
        return withProperty(SOURCES, toPaths(JkUtilsPath.disambiguate(files)));
    }


    public JkSonarqube withTestPath(Iterable<Path> files) {
        return withProperty(TEST, toPaths(files));
    }

    public JkSonarqube withBinaries(Iterable<Path> files) {
        String path = toPaths(JkUtilsPath.disambiguate(files));
        return withProperty(BINARIES, path).withProperty(JAVA_BINARIES, path);
    }

    public JkSonarqube withBinaries(Path... files) {
        return withBinaries(Arrays.asList(files));
    }

    public JkSonarqube withLibraries(Iterable<Path> files) {
        return withProperty(LIBRARIES, toPaths(JkUtilsPath.disambiguate(files)));
    }

    public JkSonarqube withSkipDesign(boolean skip) {
        return withProperty(SKIP_DESIGN, Boolean.toString(skip));
    }

    public JkSonarqube withHostUrl(String url) {
        return withProperty(HOST_URL, url);
    }

    public JkSonarqube withJdbcUrl(String url) {
        return withProperty(JDBC_URL, url);
    }

    public JkSonarqube withJdbcUserName(String userName) {
        return withProperty(JDBC_USERNAME, userName);
    }

    public JkSonarqube withJdbcPassword(String pwd) {
        return withProperty(JDBC_PASSWORD, pwd);
    }

    private String toPaths(Iterable<Path> files) {
        final Iterator<Path> it = files.iterator();
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

    private Map<String, String> andParams(String key, String value) {
        final Map<String, String> newMap = new HashMap<>(this.params);
        newMap.put(key, value);
        return newMap;
    }

    private Path projectDir() {
        return Paths.get(this.params.get(PROJECT_BASE_DIR));
    }

}

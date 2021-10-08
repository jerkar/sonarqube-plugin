package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.project.JkCompileLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectConstruction;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JkDoc("Run SonarQube analysis.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginSonarqube extends JkPlugin {

    private final Map<String, String> properties = new HashMap<>();

    @JkDoc("If true, Sonarqube analysis will be run using sonarrunner instead of sonarscanner. This is necessary to run " +
            "against some legacy sonar server.")
    public boolean legacySonarServer;

    @JkDoc("If false, no sonar analysis will be performed")
    public boolean enabled = true;

    @JkDoc("If true, the list of production dependency files will be provided to sonarqube")
    public boolean provideProductionLibs = true;

    @JkDoc("If true, the list of test dependency files will be provided to sonarqube")
    public boolean provideTestLibs = false;

    public JkPluginSonarqube(JkClass jkClass) {
        super(jkClass);
    }

    private JkSonarqube createConfiguredSonnarqube(JkJavaProject project) {
        final JkCompileLayout prodLayout = project.getConstruction().getCompilation().getLayout();
        final JkCompileLayout testLayout = project.getConstruction().getTesting().getCompilation().getLayout();
        final Path baseDir = project.getBaseDir();
        JkPathSequence libs = JkPathSequence.of();
        JkJavaProjectConstruction construction = project.getConstruction();
        if (provideProductionLibs) {
            JkDependencySet deps = construction.getCompilation().getDependencies()
                    .merge(construction.getRuntimeDependencies()).getResult();
            libs = project.getConstruction().getDependencyResolver().resolve(deps).getFiles();
        }
        final Path testReportDir = project.getConstruction().getTesting().getReportDir();
        JkModuleId moduleId = project.getPublication().getModuleId();
        if (moduleId == null) {
            String baseDirName = baseDir.getFileName().toString();
            moduleId = JkModuleId.of(baseDirName, baseDirName);
        }
        final String version = project.getPublication().getVersion();
        final String fullName = moduleId.getDotedName();
        final String name = moduleId.getName();
        JkSonarqube sonarqube = JkSonarqube
                .of(fullName, name, version)
                .setProperties(JkOptions.getAllStartingWith("sonar."))
                .setProjectBaseDir(baseDir)
                .setBinaries(project.getConstruction().getCompilation().getLayout().resolveClassDir())
                .setProperty(JkSonarqube.SOURCES, prodLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(JkSonarqube.TEST, testLayout.resolveSources().getRootDirsOrZipFiles())
                .setProperty(JkSonarqube.WORKING_DIRECTORY, baseDir.resolve(JkConstants.JEKA_DIR + "/.sonar").toString())
                .setProperty(JkSonarqube.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .setProperty(JkSonarqube.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .setProperty(JkSonarqube.SOURCE_ENCODING, project.getConstruction().getSourceEncoding());
        if (legacySonarServer) {
            sonarqube.setProperty(JkSonarqube.JACOCO_LEGACY_REPORTS_PATHS,
                    baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.exec")).toString());
            sonarqube
                    .setProperty(JkSonarqube.LIBRARIES, libs);
        } else {
            sonarqube.setProperty(JkSonarqube.JACOCO_XML_REPORTS_PATHS,
                    baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.xml")).toString());
            sonarqube
                    .setProperty(JkSonarqube.JAVA_LIBRARIES, libs)
                    .setProperty(JkSonarqube.JAVA_TEST_BINARIES, testLayout.getClassDirPath());
            if (provideTestLibs) {
                JkDependencySet deps = construction.getTesting().getCompilation().getDependencies();
                JkPathSequence testLibs = project.getConstruction().getDependencyResolver().resolve(deps).getFiles();
                sonarqube.setProperty(JkSonarqube.JAVA_TEST_LIBRARIES, testLibs);
            }
        }
        return sonarqube;
    }

    @JkDoc("Runs sonar qube analysis based on properties defined in this plugin. " +
            "Options prefixed set 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        if (!enabled) {
            JkLog.info("Sonarqube analysis has been disabled. No analysis will be performed.");
            return;
        }
        JkJavaProject project = getJkClass().getPlugins().get(JkPluginJava.class).getProject();
        JkSonarqube sonarqube = createConfiguredSonnarqube(project).setProperties(properties);
        if (legacySonarServer) {
            sonarqube.launchRunner();
        } else {
            sonarqube.launchScanner();
        }
    }

}

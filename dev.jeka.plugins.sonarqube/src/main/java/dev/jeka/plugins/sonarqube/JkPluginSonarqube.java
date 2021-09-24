package dev.jeka.plugins.sonarqube;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkModuleId;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.java.project.JkCompileLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkJavaProjectConstruction;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JkDoc("Run SonarQube analysis.")
@JkDocPluginDeps(JkPluginJava.class)
public class JkPluginSonarqube extends JkPlugin {

    private final Map<String, String> properties = new HashMap<>();

    public JkPluginSonarqube(JkClass jkClass) {
        super(jkClass);
    }

    public static JkSonarqube configureSonarFrom(JkJavaProject project) {
        final JkCompileLayout prodLayout = project.getConstruction().getCompilation().getLayout();
        final JkCompileLayout testLayout = project.getConstruction().getTesting().getCompilation().getLayout();
        final Path baseDir = project.getBaseDir();
        JkJavaProjectConstruction construction = project.getConstruction();
        JkDependencySet deps = construction.getCompilation().getDependencies()
                .merge(construction.getRuntimeDependencies()).getResult();
        final JkPathSequence libs = project.getConstruction().getDependencyResolver().resolve(deps).getFiles();
        final Path testReportDir = project.getConstruction().getTesting().getReportDir();
        final JkModuleId moduleId = project.getPublication().getModuleId();
        final String version = project.getPublication().getVersion();
        final String fullName = moduleId.getDotedName();
        final String name = moduleId.getName();
        return JkSonarqube
                .of(fullName, name, version)
                .withProperties(JkOptions.getAllStartingWith("sonar.")).withProjectBaseDir(baseDir)
                .withBinaries(project.getConstruction().getCompilation().getLayout().resolveClassDir())
                .withLibraries(libs)
                .withSourcesPath(prodLayout.resolveSources().getRootDirsOrZipFiles())
                .withTestPath(testLayout.resolveSources().getRootDirsOrZipFiles())
                .withProperty(JkSonarqube.WORKING_DIRECTORY, baseDir.resolve(JkConstants.JEKA_DIR + "/.sonar").toString())
                .withProperty(JkSonarqube.JUNIT_REPORTS_PATH,
                        baseDir.relativize( testReportDir.resolve("junit")).toString())
                .withProperty(JkSonarqube.SUREFIRE_REPORTS_PATH,
                        baseDir.relativize(testReportDir.resolve("junit")).toString())
                .withProperty(JkSonarqube.SOURCE_ENCODING, project.getConstruction().getSourceEncoding())
                .withProperty(JkSonarqube.JACOCO_REPORTS_PATHS,
                        baseDir.relativize(project.getOutputDir().resolve("jacoco/jacoco.exec")).toString());

    }

    @JkDoc("Runs sonar-scanner based on properties defined in this plugin. " +
            "Options prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void run() {
        configureSonarFrom(getJkClass().getPlugins().get(JkPluginJava.class).getProject())
                .withProperties(properties)
                .launchRunner();
    }

    @JkDoc("Runs sonar-runner based on properties defined in this plugin. " +
            "Options prefixed with 'sonar.' as '-sonar.host.url=http://myserver/..' " +
            "will be appended to sonarQube properties.")
    public void runLegacy() {
        configureSonarFrom(getJkClass().getPlugins().get(JkPluginJava.class).getProject())
                .withProperties(properties)
                .launchRunner();
    }

    /**
     * Adds a property to setupAfterPluginActivations sonar instance to run. You'll find predefined keys in {@link JkSonarqube}.
     */
    public JkPluginSonarqube setProp(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

}

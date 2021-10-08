import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.JkGitProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.builtins.git.JkPluginGit;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import java.util.Optional;

class Build extends JkClass {

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    final JkPluginGit gitPlugin = getPlugin(JkPluginGit.class);

    @Override
    protected void setup() {
        javaPlugin.getProject().simpleFacade()
            .setJavaVersion(JkJavaVersion.V8)
            .setSimpleLayout()
            .mixResourcesAndSources()
            .setCompileDependencies(deps -> deps
                .andFiles(JkLocator.getJekaJarPath())
            );
        JkPlugin.setJekaPluginCompatibilityRange(javaPlugin.getProject().getConstruction().getManifest(),
                "0.9.15.M1",
                "https://raw.githubusercontent.com/jerkar/sonarqube-plugin/master/breaking_versions.txt");


                // This section is necessary to publish on a public repository
        javaPlugin.getProject().getPublication()
                .getMaven()
                    .setModuleId("dev.jeka:sonarqube-plugin")
                    .setVersion(this::version)
                    .getPomMetadata()
                        .addApache2License()
                        .getProjectInfo()
                            .setName("Jeka plugin for SonarQube")
                            .setDescription("A Jeka plugin for SonarQube")
                            .setUrl("https://github.com/jerkar/sonarqube-plugin").__
                        .getScm()
                            .setUrl("https://github.com/jerkar/sonarqube-plugin").__.__.__
                .getPostActions()
                    .append(this::tagIfNeeded);
    }

    public void cleanPack() {
        clean(); javaPlugin.pack();
    }

    private String version() {
        String currentTagVersion = gitPlugin.getWrapper().getVersionFromTag();
        currentTagVersion = currentTagVersion.equals("HEAD-SNAPSHOT") ? "master-SNAPSHOT" : currentTagVersion;
        String releaseVersion = gitPlugin.getWrapper().extractSuffixFromLastCommitMessage("Release:");
        return Optional.ofNullable(releaseVersion).orElse(currentTagVersion);
    }

    private void tagIfNeeded() {
        JkGitProcess git = gitPlugin.getWrapper();
        Optional.ofNullable(git.extractSuffixFromLastCommitMessage("Release:"))
                .ifPresent(version -> git.tagAndPush(version));
    }

}
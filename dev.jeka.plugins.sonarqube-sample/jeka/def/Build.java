import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.builtins.git.JkPluginGit;
import dev.jeka.core.tool.builtins.jacoco.JkPluginJacoco;
import dev.jeka.plugins.sonarqube.JkPluginSonarqube;
import dev.jeka.plugins.springboot.JkPluginSpringboot;

@JkDefClasspath("dev.jeka:springboot-plugin:3.0.0.RC10")
@JkDefClasspath("../dev.jeka.plugins.sonarqube/jeka/output/dev.jeka.sonarqube-plugin.jar")
class Build extends JkClass {

    private final JkPluginSpringboot springboot = getPlugin(JkPluginSpringboot.class);

    private final JkPluginGit git = getPlugin(JkPluginGit.class);

    private final JkPluginSonarqube sonarqube = getPlugin(JkPluginSonarqube.class);

    private final JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    @Override
    protected void setup() {
        springboot.setSpringbootVersion("2.5.5");
        JkJavaProject javaProject = springboot.javaPlugin().getProject();
        javaProject.simpleFacade().setJavaVersion(JkJavaVersion.V11);
        javaProject.simpleFacade()
            .setCompileDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-web")
            )
            .setTestDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-test")
            );
        javaProject
                .getConstruction()
                    .getCompiler()
                        .setForkedWithDefaultProcess()
                    .__
                .__
                .getPublication()
                    .getMaven()
                        .setVersion(git.getWrapper().getVersionFromTag());
        sonarqube.provideTestLibs = true;
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        clean(); springboot.javaPlugin().pack();
        sonarqube.run();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).cleanPack();
    }

}
![Build Status](https://github.com/jerkar/sonarqube-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/sonarqube-plugin.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22dev.jeka22%20AND%20a:%22springboot-plugin%22)

# sonarqube-plugin

A Sonarqube plugin for Jeka

## How to use

### Common usage

Just declare the plugin in your build class.  

```java
@JkDefClasspath("dev.jeka.plugins:sonarqube:[version]")
public class Build extends JkClass {
    
    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    JkPluginSonarqube sonarqube = getPlugin(JkPluginSonarqube.class);

    ...
}
```
Execute `run` method from Sonaqube plugin after a build as `jeka clean java#pack sonarqube#run`.

Sonarqube client can be configured programmatically using `sonarqube` instance and/or 
using standard system properties from the command line as `-Dsonar.host=...` (see https://docs.sonarqube.org/latest/analysis/analysis-parameters/).

### Programmatically

You can use directly `JkSonarqube` in build code without using `JkPluginSonarqube`class.

### Bind dynamically

You can invoke Sonarqube plugin from command line on a Jeka project that does declare this plugin it.

`jeka java#pack @dev.jeka.plugins:sonarqube:[version] sonarqube#run`

To get help and options :
`jeka sonarqube#help`

### Example

See example [here](dev.jeka.plugins.sonarqube-sample)


## How to build this project

This project uses Jeka wrapper, you don't need to have Jeka installed on your machine. simply execute `./jekaw cleanPack`
from the root of this project.

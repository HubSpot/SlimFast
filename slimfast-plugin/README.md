# slimfast-plugin

## Overview ##

The slimfast-plugin can be used in place of the maven-assembly-plugin or maven-shade-plugin (which are often used to build 
uber jars). In addition, if you configure the maven-jar-plugin in the right way, the resulting jar (although not an uber jar)
will still be runnable using plain old `java -jar` (ie, without needing to manually construct the classpath). 

This uses a feature of the JVM which is that if you run a jar which has a `Class-Path` entry in its manifest, then those 
paths are added to the classpath of the JVM. Using this feature, we can tell the maven-jar-plugin to build the classpath 
for us at build time and add it as a manifest property. Then we can configure the slimfast-plugin to copy the dependency 
jars to the right place and the resulting jar will start up fine when run with `java -jar`.

## Usage ##

The plugin has three goals: copy, upload, and download. Copy can be used to copy your dependencies to the target folder 
so they're available at runtime. This saves you the time of building an uber jar and removes the jar merging 
complexities, but it doesn't reduce the size of your build artifacts.

Example of the copy goal:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.0.0</version>
        <configuration>
          <archive>
            <manifest>
              <addClasspath>true</addClasspath>
              <mainClass>${your-main-class-property}</mainClass>
              <classpathPrefix>lib/</classpathPrefix>
              <classpathLayoutType>repository</classpathLayoutType>
            </manifest>
          </archive>
        </configuration>
      </plugin>
      <plugin>
        <groupId>com.hubspot.maven.plugins</groupId>
        <artifactId>slimfast-plugin</artifactId>
        <version>0.10</version>
        <executions>
          <execution>
            <goals>
              <goal>copy</goal>
            </goals>
            <phase>package</phase>
            <configuration>
              <manifest>
                <classpathPrefix>lib/</classpathPrefix>
                <classpathLayoutType>repository</classpathLayoutType>
              </manifest>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

**NOTE:** It's very important that the `classpathPrefix` and ` classpathLayoutType` on the maven-jar-plugin match 
the values on the slimfast-plugin, otherwise the jars won't be where the JVM expects and it won't be able 
to find any of the dependency classes.

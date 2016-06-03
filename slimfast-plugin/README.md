# slimfast-plugin

## Overview ##

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

It's very important that the manifest configuration on the maven-jar-plugin matches the manifest
configuration on the slimfast-plugin.

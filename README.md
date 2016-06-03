# SlimFast

![slimfast](https://tremblyfitness.files.wordpress.com/2011/06/slim.jpg)

## Overview ##

SlimFast is a tool for Java apps to help them stop building uber jars for deployment (massive jars containing
all of the app's dependencies). The first part is the maven plugin, which can be used in place of the 
maven-assembly-plugin or maven-shade-plugin (which are often used to build uber jars). 

## Usage ##

Maven plugin that uploads all of the project dependencies to S3 (if they're not already present). We use this at HubSpot to avoid
having to build fat JARs. Instead, we upload all of the dependencies to S3 at build time and store the S3 keys. Then at deploy time 
we download these artifacts from S3 (using a local cache for efficiency).

Example:

```xml
<plugin>
  <groupId>com.hubspot.maven.plugins</groupId>
  <artifactId>slimfast-plugin</artifactId>
  <version>0.6</version>
  <executions>
    <execution>
      <goals>
        <goal>upload</goal>
      </goals>
      <configuration>
        <manifest>
          <classpathPrefix>lib/</classpathPrefix>
          <classpathLayoutType>repository</classpathLayoutType>
        </manifest>
      </configuration>
    </execution>
  </executions>
</plugin>
```

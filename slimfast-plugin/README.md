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

The plugin has three goals: `copy`, `upload`, and `download`. `copy` can be used to copy your dependencies to the target 
folder so they're available at runtime. This saves you the time of building an uber jar and removes the jar merging 
complexities, but it doesn't reduce the size of your build artifacts ([Example]()).

Just using the `copy` goal has a lot of advantages and is a big win in its own right, but there's still room for improvement.
At HubSpot, for example, we tar up the build directory and upload it to S3 at the end of the build. Then we download and 
untar it on the application servers when someone wants to deploy. Using the `copy` goal doesn't reduce the size of these 
tarballs so we're still uploading the same amount to S3 on build and downloading the same amount on deploy. This adds 
time to builds and deploys, uses up lots of bandwidth, and costs money for storing these large artifacts in S3. 

But fear not! This is what the `upload` and `download` goals are for. The `upload` goal binds to the deploy phase by default
and will upload all of the project's dependencies to S3. It only uploads the dependency if it doesn't already exist in S3,
so after the initial build this step should mostly be a no-op and go very fast.

Example of the `upload` goal:

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
              <goal>upload</goal>
            </goals>
            <phase>deploy</phase>
            <configuration>
              <s3Bucket>my-bucket</s3Bucket>
              <s3ArtifactRoot>jars</s3ArtifactRoot>
              <s3AccessKey>abc</s3AccessKey>
              <s3SecretKey>123</s3SecretKey>
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

You probably don't want to hard-code these values in your pom though, instead you can use the `properties-maven-plugin`
to read them from a file that is managed by puppet or your configuration management tool of choice. If you have
a file located at `/etc/slimfast.properties` with contents like:

```properties
s3.bucket=my-bucket
s3.artifact.root=jars
s3.access.key=abc
s3.secret.key=123
```

Then you could invoke SlimFast like this:

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
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>properties-maven-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
          <execution>
            <goals>
              <goal>read-project-properties</goal>
            </goals>
            <phase>initialize</phase>
            <configuration>
              <files>
                <file>/etc/slimfast.properties</file>
              </files>
            </configuration>
          </execution>
        </executions>
      </plugin>      
      <plugin>
        <groupId>com.hubspot.maven.plugins</groupId>
        <artifactId>slimfast-plugin</artifactId>
        <version>0.10</version>
        <executions>
          <execution>
            <goals>
              <goal>upload</goal>
            </goals>
            <phase>deploy</phase>
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

## Examples ##

### Copy Goal ###

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

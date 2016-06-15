# slimfast-hadoop

## Overview ##

One place where fat jars can be convenient is for frameworks like Hadoop that ship your application jar around.
If you build a fat jar, your dependencies automatically go along for the ride and everything works great. If
you stop building a fat jar, however, you need to handle this manually so that your dependencies are available
when the application starts up on the other side. 

This microlibrary makes that easier by finding all of the dependency jars, writing them to HDFS (if they don't 
already exist), and adding them to the job's classpath so everything should work transparently. Because it only 
writes jars to HDFS that don't already exist, after your job runs the first time this step should usually be a 
no-op. And because the job jar is so much smaller, we found that switching away from fat jars drastically 
improved our hadoop job launch speeds.

## Usage ##

First you build a `SlimfastHadoopConfiguration`. This contains the path to the main application jar, the root
folder on hdfs you want to use to store dependency jars (defaults to `/jars` if not specified), and the Hadoop 
`Configuration`. Then you pass this configuration to the `HadoopHelper` which finds all the dependency jars, 
writes them to HDFS, and adds them to the job's classpath. A minimal invocation would look like:

```java
SlimfastHadoopConfiguration slimfastConfiguration = SlimfastHadoopConfiguration.newBuilder()
    .setJarByClass(MyJob.class)
    .setConfiguration(configuration)
    .build();
    
HadoopHelper.writeJarsToHdfsAndAddToClasspath(slimfastConfiguration);    
```

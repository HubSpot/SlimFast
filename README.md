# SlimFast

![slimfast](https://tremblyfitness.files.wordpress.com/2011/06/slim.jpg)

## Overview ##

SlimFast is a tool for Java apps to help them stop building fat jars for deployment (massive jars containing
all of the app's dependencies). Building fat jars is slow and adds a lot of complexity (subtle or not so subtle
bugs can occur when jars being merged have duplicate files for example). 

The first part is the maven plugin, which can be used in place of the maven-assembly-plugin or maven-shade-plugin
(which are often used to build fat jars). The other part is a helper library for hadoop to write each job's dependencies
to hdfs and add them to the job's classpath.

## Usage ##

See [here](slimfast-plugin/README.md) for usage instructions for the maven plugin or [here](slimfast-hadoop/README.md) for the hadoop library.

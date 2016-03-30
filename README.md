[![Build Status](https://img.shields.io/travis/essobedo/gitlab-version-manager/master.svg)](https://travis-ci.org/essobedo/gitlab-version-manager)
[![License](https://img.shields.io/badge/license-LGPLv2.1-green.svg)](http://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.essobedo/gitlab-version-manager/badge.svg?color=blue&prefix=v)](http://www.javadoc.io/doc/com.github.essobedo/gitlab-version-manager)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.essobedo/gitlab-version-manager.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.essobedo/gitlab-version-manager)

## What is it for?

This project is actually an implementation of a Version Manager as defined in the project https://github.com/essobedo/application-manager based on the gitlab public API.
It provides an abstract class called [AbstractVersionManager](https://github.com/essobedo/gitlab-version-manager/blob/master/src/main/java/com/github/essobedo/gitlabvm/AbstractVersionManager.java)
that follows the *Template method* pattern by covering most part of the logic to implement the methods of the interface [VersionManager](https://github.com/essobedo/application-manager/blob/master/src/main/java/com/github/essobedo/appma/spi/VersionManager.java)
and by only delegating the [connection configuration](https://github.com/essobedo/gitlab-version-manager/blob/master/src/main/java/com/github/essobedo/gitlabvm/ConnectionConfiguration.java) to the sub classes.


## How to build it?

This project relies on *maven*, so you will need to install maven 3 with a JDK 8, then simply launch the famous
command *mvn clean install* and that's it!

To avoid signing the artifacts you can launch *mvn clean install -Pfast*.
To check the quality of the code, you can launch *mvn clean install -Pcheck*.

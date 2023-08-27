# Bridge ASM
[![Build Status](https://dev.me1312.net/jenkins/job/Bridge/badge/icon)](https://dev.me1312.net/jenkins/job/Bridge/)
[![Build Verison](https://img.shields.io/badge/dynamic/xml.svg?label=build&url=https%3A%2F%2Fdev.me1312.net%2Fmaven%2Fnet%2FME1312%2FASM%2Fbridge-plugin%2Fmaven-metadata.xml&query=%2F%2Fversioning%2Frelease&colorB=blue)](https://dev.me1312.net/jenkins/job/Bridge/)<br><br>
Bridge is a post-compile maven plugin that injects new advanced functionality into the Java language despite using existing semantics. Currently, we add the following features:
* [**Redirection of constructors, methods, &amp; fields**](https://github.com/ME1312/Bridge/wiki/Features#bridges) *with* `@Bridge`
* [**Unsafe native referencing of classes, constructors, methods, &amp; fields**](https://github.com/ME1312/Bridge/wiki/Features#invocations) *with* `Invocation`
* [**Unrestricted `goto` execution jumping**](https://github.com/ME1312/Bridge/wiki/Features#jumps) *with* `Label` &amp; `Jump`
* [**Automatic multi-release class forking**](https://github.com/ME1312/Bridge/wiki/Features#forks) *with* `Invocation.LANGUAGE_LEVEL`
* [**Native unchecked exception throwing &amp; handling**](https://github.com/ME1312/Bridge/wiki/Features#unchecked-exceptions) *with* `Unchecked`
* [**Post-compile class hierarchy modification**](https://github.com/ME1312/Bridge/wiki/Features#type-adoption) *with* `@Adopt`
* [**Public implementation hiding**](https://github.com/ME1312/Bridge/wiki/Features#appending-the-synthetic-modifier) *with* `@Synthetic`
* [**Optional stripping of debug metadata**](https://github.com/ME1312/Bridge/wiki/Features#removing-debug-metadata)

*This project could always benefit from your submission of [additional automated testing](https://github.com/ME1312/Bridge/issues/1)!*
<br>

```xml
<!-- required to access the api -->
<repositories>
    <repository>
        <id>ME1312.net</id>
        <url>https://dev.me1312.net/maven</url>
    </repository>
</repositories>

<!-- required to access the maven plugin -->
<pluginRepositories>
    <pluginRepository>
        <id>ME1312.net</id>
        <url>https://dev.me1312.net/maven</url>
    </pluginRepository>
</pluginRepositories>

<!-- ensures the api and maven plugin use the same version -->
<properties><!-- remember to replace this value this with a real build id! -->
    <bridge.version>00w00a</bridge.version>
</properties>

<!-- provides you an api to compile against that isn't required at runtime -->
<dependencies>
    <dependency>
        <groupId>net.ME1312.ASM</groupId>
        <artifactId>bridge</artifactId>
        <version>${bridge.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<!-- runs a maven plugin to build your bridges -->
<build>
    <plugins>
        <plugin>
            <groupId>net.ME1312.ASM</groupId>
            <artifactId>bridge-plugin</artifactId>
            <version>${bridge.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>bridge</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```
<br><br>
<br><br>

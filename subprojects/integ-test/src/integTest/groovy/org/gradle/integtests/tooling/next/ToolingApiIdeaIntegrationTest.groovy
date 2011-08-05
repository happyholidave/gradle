/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests.tooling.next

import org.gradle.integtests.fixtures.MavenRepository
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.tooling.model.idea.*

class ToolingApiIdeaModelIntegrationTest extends ToolingApiSpecification {

    def setup() {
//        toolingApi.withConnector {
//            it.useInstallation(new File(dist.gradleHomeDir.absolutePath))
//            it.embedded(false)
//            it.daemonMaxIdleTime(300, TimeUnit.SECONDS)
//            DaemonGradleExecuter.registerDaemon(dist.userHomeDir)
//        }
    }

    def "builds the model even if idea plugin not applied"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = '''
apply plugin: 'java'
description = 'this is a project'
'''
        projectDir.file('settings.gradle').text = 'rootProject.name = \"test project\"'

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }

        then:
        project.parent == null
        project.name == 'test project'
        project.description == null
        project.children.size() == 1
        project.children[0] instanceof IdeaModule
        project.children == project.modules
    }

    def "provides basic project information"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = """
apply plugin: 'java'
apply plugin: 'idea'

idea.project {
  languageLevel = '1.5'
  jdkName = '1.6'
}
"""

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }

        then:
        project.languageLevel.level == "JDK_1_5"
        project.jdkName == '1.6'
    }

    def "provides all modules"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = '''
subprojects {
    apply plugin: 'java'
}
'''
        projectDir.file('settings.gradle').text = "include 'api', 'impl'"

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }

        then:
        project.children.size() == 3
        project.children.any { it.name == 'api' }
        project.children.any { it.name == 'impl' }
    }

    def "provides basic module information"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = """
apply plugin: 'java'
apply plugin: 'idea'

idea.module.inheritOutputDirs = false
idea.module.outputDir = file('someDir')
idea.module.testOutputDir = file('someTestDir')
"""

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }
        def module = project.children[0]

        then:
        module.contentRoots.size() == 1
        module.contentRoots[0].rootDirectory == projectDir
        module.parent instanceof IdeaProject
        module.parent == project
        module.parent == module.project
        module.children.empty
        module.description == null

        !module.compilerOutput.inheritOutputDirs
        module.compilerOutput.outputDir == projectDir.file('someDir')
        module.compilerOutput.testOutputDir == projectDir.file('someTestDir')
    }

    def "provides source dir information"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "apply plugin: 'java'"

        projectDir.create {
            src {
                main {
                    java {}
                    resources {}
                }
                test {
                    java {}
                    resources {}
                }
            }
        }

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }
        IdeaModule module = project.children[0]
        IdeaContentRoot root = module.contentRoots[0]

        then:
        root.sourceDirectories.size() == 2
        root.sourceDirectories.any { it.directory == projectDir.file('src/main/java') }
        root.sourceDirectories.any { it.directory == projectDir.file('src/main/resources') }

        root.testDirectories.size() == 2
        root.testDirectories.any { it.directory == projectDir.file('src/test/java') }
        root.testDirectories.any { it.directory == projectDir.file('src/test/resources') }
    }

    def "provides exclude dir information"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = """
apply plugin: 'java'
apply plugin: 'idea'

idea.module.excludeDirs += file('foo')
"""

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }
        def module = project.children[0]

        then:
        module.contentRoots[0].excludeDirectories.any { it.path.endsWith 'foo' }
    }

    def "provides dependencies"() {
        def projectDir = dist.testDir
        def fakeRepo = projectDir.file("repo")

        new MavenRepository(fakeRepo).module("foo.bar", "coolLib", 1.0).publishArtifact()
        new MavenRepository(fakeRepo).module("foo.bar", "coolLib", 1.0, 'sources').publishArtifact()
        new MavenRepository(fakeRepo).module("foo.bar", "coolLib", 1.0, 'javadoc').publishArtifact()


        projectDir.file('build.gradle').text = """
subprojects {
    apply plugin: 'java'
}

project(':impl') {
    apply plugin: 'idea'

    repositories {
        mavenRepo urls: "${fakeRepo.toURI()}"
    }

    dependencies {
        compile project(':api')
        testCompile 'foo.bar:coolLib:1.0'
    }

    idea.module.downloadJavadoc = true
}
"""
        projectDir.file('settings.gradle').text = "include 'api', 'impl'"

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }
        def module = project.children.find { it.name == 'impl' }

        then:
        def libs = module.dependencies
        IdeaSingleEntryLibraryDependency lib = libs.find {it instanceof IdeaSingleEntryLibraryDependency}

        lib.file.exists()
        lib.file.path.endsWith('coolLib-1.0.jar')

        lib.source.exists()
        lib.source.path.endsWith('coolLib-1.0-sources.jar')

        lib.javadoc.exists()
        lib.javadoc.path.endsWith('coolLib-1.0-javadoc.jar')

        lib.scope.scope == 'TEST'

        IdeaModuleDependency mod = libs.find {it instanceof IdeaModuleDependency}
        mod.dependencyModule == project.modules.find { it.name == 'api'}
        mod.scope.scope == 'COMPILE'
    }

    def "makes sure module names are unique"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = """
subprojects {
    apply plugin: 'java'
}

project(':impl') {
    dependencies {
        compile project(':api')
    }
}

project(':contrib:impl') {
    dependencies {
        compile project(':contrib:api')
    }
}
"""
        projectDir.file('settings.gradle').text = "include 'api', 'impl', 'contrib:api', 'contrib:impl'"

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }

        then:
        def allNames = project.modules*.name
        allNames.unique().size() == 6

        IdeaModule impl = project.modules.find { it.name == 'impl' }
        IdeaModule contribImpl = project.modules.find { it.name == 'contrib-impl' }

        impl.dependencies[0].dependencyModule        == project.modules.find { it.name == 'api' }
        contribImpl.dependencies[0].dependencyModule == project.modules.find { it.name == 'contrib-api' }
    }

    def "module has access to gradle project and its tasks"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = """
subprojects {
    apply plugin: 'java'
}

task rootTask {}

project(':impl') {
    task implTask {}
}
"""
        projectDir.file('settings.gradle').text = "include 'api', 'impl'; rootProject.name = 'root'"

        when:
        IdeaProject project = withConnection { connection -> connection.getModel(IdeaProject.class) }

        then:
        def impl = project.modules.find { it.name == 'impl'}
        def root = project.modules.find { it.name == 'root'}

        root.gradleProject.tasks.find { it.name == 'rootTask' && it.path == ':rootTask' && it.project == root.gradleProject }
        !root.gradleProject.tasks.find { it.name == 'implTask' }

        impl.gradleProject.tasks.find { it.name == 'implTask' && it.path == ':impl:implTask' && it.project == impl.gradleProject}
        !impl.gradleProject.tasks.find { it.name == 'rootTask' }
    }
}
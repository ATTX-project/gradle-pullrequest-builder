# ATTX gradle-pullrequest-builder

## Purpose

An Github Pull Request builder based on https://github.com/dpreussler/gradle-pullrequest-builder and modified for ATTX-project https://github.com/ATTX-project
Commits unchanged files to Git (local git installation needed) and creates Pull request.
Meant as part of CI jobs that need to change code.

### Applying the Plugin

It requires an artifactrepository URL e.g. Archiva

```groovy
buildscript {
  repositories {
    maven { url "${artifactRepoURL}/repository/attx-releases"}
  }
  dependencies {
    classpath "org.uh.hulib.attx.gradle:gradle-pullrequest-builder:1.6-SNAPSHOT"
  }
}

apply plugin: "attx.pullrequest.gradle.github"
```

### Configuration


```groovy
gitPullRequest {
    // where to publish to (repo must exist)
    githubUri = 'https://github.com/ATTX-project/ATTX-project.github.io'

    // github user name to use
    user = "${user}"

    // github password or better access token
    accessToken = "${token}"

    // the name the branch should start with (will be added by timestamp to avoid collisions)
    branchSuffix = 'dev'

    // the target branch to send pull request to
    master = 'master'

    // // optional, the folder to commit all new or changed files from (default=src)
    // source = '.'

    // optional, the title of the pull request
    title = "Automatic pull request: Jenkins build#${jenkinsBuild}"

    // // optional, the message of the commit
    message = "Commited by Jenkins build#${jenkinsBuild}."

}
```

### Tasks and Execution

Generally, you'll just run `gitPullRequest`

* `gitPullRequest` - runs 'gitCommit' and then creates Pull Request
* `gitDeleteRequest` - deletes the branch specified in the configuration
* `gitCommit` - commits and pushes



### Configuring Jenkins

Configuring a Jenkins pipleline:
```groovy
pipeline {
    agent any
    stages {
        stage('Checkout') { // for display purposes
            steps {
                // Get some code from a GitHub repository
                git branch: 'dev', url: 'https://<user>:<user_token>@github.com/ATTX-project/ATTX-project.github.io.git'
            }
        }
        stage('Compile/Package/Test') {
            steps {
                echo sh (script: "${GRADLE_HOME}/bin/gradle --console=plain -b ${workspace}/build.gradle -PartifactRepoURL=http://archiva:8080 -Puser=<user> -Ptoken=<user_token> -PjenkinsBuild=${BUILD_NUMBER} clean gitPullRequest", returnStdout: true)
            }
        }
    }

}
```

Licensed under MIT license
(c) 2017 Danny Preussler

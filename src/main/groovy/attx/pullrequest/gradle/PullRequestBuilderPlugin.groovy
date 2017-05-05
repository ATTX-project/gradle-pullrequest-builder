package attx.pullrequest.gradle

import com.github.kittinunf.fuel.Fuel
import groovy.transform.PackageScope
import kotlin.Pair
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.nio.charset.Charset

class PullRequestBuilderPlugin implements Plugin<Project> {

    @PackageScope static final String GIT_TASK = 'gitCommit'
    @PackageScope static final String PR_TASK = 'gitPullRequest'
    @PackageScope static final String DELETE_TASK = 'gitDeleteRequest'
    String branchName

    @Override
    void apply(Project project) {
        PullRequestBuilderExtension extension = project.extensions.create('gitPullRequest', PullRequestBuilderExtension, project)

        Task git = createGitTask(project, extension)
        Task gitHub = createPullRequestTask(project, extension)
        Task delete = deleteRequestTask(project, extension)
        gitHub.dependsOn git
    }

    private Task createGitTask(Project project, PullRequestBuilderExtension extension) {
        Task task = project.tasks.create(GIT_TASK)
        task.outputs.upToDateWhen { false }
        def sout = new StringBuilder(), serr = new StringBuilder()
        task.with {
            group = 'publishing'
            description = 'Commits and pushes changes to git.'
            doFirst {
                branchName = extension.branchSuffix
            }
            doLast {
                "git add .".execute().waitFor()
                def components = ["${extension.message}"]
                def cmd = ["git", "commit", "-m"]
                for (component in components) {
                    cmd.add(component)
                }
                def commit = cmd.execute()
                commit.consumeProcessOutput(sout, serr)
                commit.waitFor()
                def push =  "git push -u origin ${branchName}".execute()
                push.consumeProcessOutput(sout, serr)
                push.waitFor()
                println "out> $sout err> $serr"
            }
        }
        return task
    }

    private Task deleteRequestTask(Project project, PullRequestBuilderExtension extension) {
        Task task = project.tasks.create(DELETE_TASK)
        task.outputs.upToDateWhen { false }
        task.with {
            group = 'publishing'
            description = 'Deletes Pull request on github.'
            doFirst {
                branchName = extension.branchSuffix
            }
            doLast {
                println Fuel.delete("${extension.githubUri.replaceAll("github.com", "api.github.com/repos") + "/git/refs/heads/" + "${branchName}" }")
                        .authenticate("${extension.user}", "${extension.accessToken}")
                        .header(new Pair<String, Object>("Content-Type", "application/json"))
                        .response().toString()
            }
        }
        return task
    }

    private Task createPullRequestTask(Project project, PullRequestBuilderExtension extension) {
        Task task = project.tasks.create(PR_TASK)
        task.outputs.upToDateWhen { false }
        task.with {
            group = 'publishing'
            description = 'Creates Pull request on github.'
            onlyIf { dependsOnTaskDidWork() }
            doFirst {
                "git push".execute().waitFor()
            }
            doLast {
                println Fuel.post("${extension.githubUri.replaceAll("github.com", "api.github.com/repos") + "/pulls" }")
                .authenticate("${extension.user}", "${extension.accessToken}")
                        .body("{ \"title\" : \"${extension.title}\", " +
                        "\"body\" : \"${extension.message}\"," +
                        "\"head\" : \"${branchName}\"," +
                        "\"base\" : \"${extension.master}\" }", Charset.forName("utf-8"))
                .header(new Pair<String, Object>("Content-Type", "application/json"))
                .response().toString()
            }
        }
        return task
    }
}

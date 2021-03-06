# How To
## How to get a list of installed Jenkins plugins with name and version pair
You can retrieve the information using the Jenkins Script Console which is accessible by visiting http://<jenkins-url>/script.

```bash
Jenkins.instance.pluginManager.plugins.each{ plugin -> 
  println ("${plugin.getShortName()}:${plugin.getVersion()}")
}
```

# Jenkins Master Configuration
This repo is used to build a customized OpenShift Jenkins 2 image with [source to image (S2I)](https://github.com/openshift/source-to-image). The base OpenShift Jenkins S2I can be found at `registry.access.redhat.com/openshift3/jenkins-2-rhel7`. The resulting image is a Jenkins master, and should be used in a master / slaves architecture. This image is configured to provide slaves as k8s pods via the [k8s Jenkins plugin](https://docs.openshift.com/container-platform/3.5/using_images/other_images/jenkins.html#using-the-jenkins-kubernetes-plug-in-to-run-jobs). Thus, this repo doesn't define any build tools or the like, as they are the responsibility of the slaves.

## Building and Testing Locally
With `s2i` installed; you can run the following to build and test your changes to the S2I locally.
```bash
s2i build --loglevel 5 jenkins-master openshift/jenkins-2-centos7 jenkins-s2i:latest
```

## How This Repo Works

The directory structure is dictated by [OpenShift Jenkins S2I image](https://docs.openshift.com/container-platform/3.5/using_images/other_images/jenkins.html#jenkins-as-s2i-builder). In particular:

- [plugins.txt](plugins.txt) is used to install plugins during the S2I build. If you want the details, here is the [S2I assemble script](https://github.com/dimjost/jenkins/blob/master/2/contrib/s2i/assemble), which calls the [install jenkins plugins script](https://github.com/dimjost/jenkins/blob/master/2/contrib/jenkins/install-plugins.sh).
- files in the [configuration](configuration) directory will have comments describing exactly what they do

## Slack Integration

To Integrate with slack follow the steps at https://github.com/jenkinsci/slack-plugin. Particularly, create a webhook at  https://customteamname.slack.com/services/new/jenkins-ci. After the webhook setup is complete at slack, record and add the below environmental variables. You can retrieve the values on your [slack dashboard](https://my.slack.com/services/new/jenkins-ci). Make sure you are logged into the correct team.
1. The base url as `SLACK_BASE_URL`
2. The slack token as `SLACK_TOKEN`
3. The slack room you selected as the default slack channel as `SLACK_ROOM`
4. optionally, a jenkins credential can be used for the token and referenced by a custom id at `SLACK_TOKEN_CREDENTIAL_ID`. This takes precedences over the `SLACK_TOKEN`

## SonarQube Integration
 
By default the deployment will attempt to connect to SonarQube and configure its setup including an authentication token. The default url is http://sonarqube:9000. This can be overriden adding an environment variable named `SONARQUBE_URL`. To disable SonarQube entirely set an environment variable named `DISABLE_SONAR` with any value.

## Git Creds
Inject the `git` credentials to Jenkins-s2i when it is being built by editing `configuration/init.groovy.d/configure-credentials.groovy` or by exposing a new environment Variable to the Jenkins deployment tempate.

## Jenkins DSL Seed for MultiBranch Pipelines (GitLab)

A DSL Seed job is included in the s2i. The purpose of this job is to automatically generate multi branc pipelines for each project in a given GitLab namespace that has a `Jenkinsfile`. To set this up, configure the Deployment Config for your Jenkins with the following `ENVIRONMENT` variables or just edit the `configuration/jobs/seed-multibranch-pipelines/config.xml` file. If you don't want or need this job, just delete it from the `configuration/jobs` directory.
```
GITLAB_HOST is the Http address of the GitLab Project eg 'https://gitlab.apps.proj.example.com'
GITLAB_TOKEN is the GitLab API token to access repos and projects eg 'token123'
GITLAB_GROUP_NAME is the GitLab group name where projects are stored eg 'rht-labs'
```

## Shared Library

An optional shared global library can be used to add method calls to pipelines which can help to simplify and organize a pipeline. The global library will be implicitly available to all pipelines.

To configure a library environment variables need to be made available to your image. In OCP, add environment variables to your deployment config. The following variables can be set
1. SHARED_LIB_REPO - If this variable is set then the deployment will attempt to configure a shared global library. This value should reference a git repository. If this value is not set, no shared global library will be set.
2. SHARED_LIB_REF - A value that that points to a git reference such as a branch or tag of a repository. The default value is `master`
3. SHARED_LIB_NAME - A name for the library. It can be anything.
4. SHARED_LIB_SECRET - If the git repo is private, this value should be a reference to a secret available to the project. If this value is not set, it is assumed that the git repo is publicly available. This value assumes a deployment on openshift so it prepends that value of the namespace to the secret. 

## Contributing

There are some [helpers](helpers/README.MD) to get configuration out of a running Jenkins. 

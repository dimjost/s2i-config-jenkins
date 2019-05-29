#!/usr/bin/env groovy
import jenkins.model.*
import com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView
import groovy.json.JsonSlurper
import hudson.tools.InstallSourceProperty

import java.util.logging.Level
import java.util.logging.Logger
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud
import jenkins.model.JenkinsLocationConfiguration

import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import java.nio.file.Files
import jenkins.model.Jenkins
import net.sf.json.JSONObject
import org.jenkinsci.plugins.plaincredentials.impl.*
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO,  'running configure-jenkins.groovy' )

// create Global Environment Variables
public createGlobalEnvironmentVariables(String key, String value){
	
	Jenkins instance = Jenkins.getInstance();
	
	DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = instance.getGlobalNodeProperties();
	List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);
	
	EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
	EnvVars envVars = null;
	
	if ( envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0 ) {
		newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
		globalNodeProperties.add(newEnvVarsNodeProperty);
		envVars = newEnvVarsNodeProperty.getEnvVars();
	} else {
		envVars = envVarsNodePropertyList.get(0).getEnvVars();
	}
	envVars.put(key, value)
	instance.save()
}

def sout = new StringBuilder(), serr = new StringBuilder()
def proc = "ssh-keygen -t rsa -C 'your.email@example.com' -b 4096 -q -N '' -f /tmp/id_rsa".execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(3000)
// println "out> $sout err> $serr"

String pivateKey = new File('/tmp/id_rsa').getText('UTF-8')
String publicKey = new File('/tmp/id_rsa.pub').getText('UTF-8')
createGlobalEnvironmentVariables('pivateKey',pivateKey)
createGlobalEnvironmentVariables('publicKey',publicKey)

// parameters
def jenkinsMasterKeyParameters = [
  description:  'Jenkins Master SSH Key',
  id:           'jenkins-master-key',
  secret:       '',
  userName:     'cip_build_devops-expert-tech',
  key:          new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(pivateKey)
]

// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()

// get credentials domain
def domain = Domain.global()

// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

// define private key
def privateKey = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  jenkinsMasterKeyParameters.id,
  jenkinsMasterKeyParameters.userName,
  jenkinsMasterKeyParameters.key,
  jenkinsMasterKeyParameters.secret,
  jenkinsMasterKeyParameters.description
)

// add credential to store
store.addCredentials(domain, privateKey)

// save to disk
jenkins.save()






try {
    // delete default OpenShift job
    Jenkins.instance.items.findAll {
        job -> job.name == 'OpenShift Sample'
    }.each {
        job -> job.delete()
    }
} catch (NullPointerException npe) {
   LOG.log(Level.INFO, 'Failed to delete OpenShift Sample job')
}
// create a default build monitor view that includes all jobs
// https://wiki.jenkins-ci.org/display/JENKINS/Build+Monitor+Plugin
if ( Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.size == 0){
  view = new BuildMonitorView('Build Monitor','Build Monitor')
  view.setIncludeRegex('.*')
  Jenkins.instance.addView(view)
}



// support custom CSS for htmlreports
// https://stackoverflow.com/questions/35783964/jenkins-html-publisher-plugin-no-css-is-displayed-when-report-is-viewed-in-j
System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "")

// This is a helper to delete views in the Jenkins script console if needed
// Jenkins.instance.views.findAll{ view -> view instanceof com.smartcodeltd.jenkinsci.plugins.buildmonitor.BuildMonitorView }.each{ view -> Jenkins.instance.deleteView( view ) }

println("WORKAROUND FOR BUILD_URL ISSUE, see: https://issues.jenkins-ci.org/browse/JENKINS-28466")

def hostname = System.getenv('HOSTNAME')
println "hostname> $hostname"

def sout = new StringBuilder(), serr = new StringBuilder()
def proc = "oc get pod ${hostname} -o jsonpath={.metadata.labels.name}".execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(3000)
println "out> $sout err> $serr"

def sout2 = new StringBuilder(), serr2 = new StringBuilder()
proc = "oc get route ${sout} -o jsonpath={.spec.host}".execute()
proc.consumeProcessOutput(sout2, serr2)
proc.waitForOrKill(3000)
println "out> $sout2 err> $serr2"

def jlc = jenkins.model.JenkinsLocationConfiguration.get()
jlc.setUrl("https://" + sout2.toString().trim())

println("Configuring container cap for k8s, so pipelines won't hang when booting up slaves")

try{
    def kc = Jenkins.instance.clouds.get(0)

    println "cloud found: ${Jenkins.instance.clouds}"

    kc.setContainerCapStr("100")
}
finally {
    //if we don't null kc, jenkins will try to serialise k8s objects and that will fail, so we won't see actual error
    kc = null
}


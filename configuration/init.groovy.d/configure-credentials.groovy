#!/usr/bin/env groovy
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.GlobalConfiguration

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

// disable Job DSL script approval
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()

import jenkins.*
import hudson.model.*
import hudson.security.*

import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")

LOG.log(Level.INFO,  'running configure-credentials.groovy' )

// create jenkins creds for commiting tags back to repo. Can use Env vars on the running image or just insert below.
domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
gitUsername = System.getenv("GIT_USERNAME") ?: "jenkins-user"
gitPassword = System.getenv("GIT_PASSWORD") ?: "password-for-user"
usernameAndPassword = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  "jenkins-git-creds", "Git creds for Jenkins",
  gitUsername,
  gitPassword
)
store.addCredentials(domain, usernameAndPassword)

// Add annoymouse access for git webhooks to trigger builds
def strategy = new GlobalMatrixAuthorizationStrategy()
//  Setting Anonymous Permissions
strategy.add(hudson.model.Item.BUILD,'anonymous')
strategy.add(hudson.model.Item.CANCEL,'anonymous')
def instance = Jenkins.getInstance()
instance.setAuthorizationStrategy(strategy)
instance.save()



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
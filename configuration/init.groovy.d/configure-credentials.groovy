import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration
import jenkins.model.GlobalConfiguration

// disable Job DSL script approval
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).useScriptSecurity=false
GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class).save()

import jenkins.*
import hudson.model.*
import hudson.security.*
import hudson.util.Secret;

import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")
LOG.log(Level.INFO,  "########## Running configure-credentials.groovy ##########" )

//////////////////////////
//
// Technical User for Git
//
//////////////////////////
LOG.log(Level.INFO,  "Add technical user" )
// USE THE SAME GIT USER LIKE FOR SHARED-LIBRARYS
// create jenkins creds for commiting tags back to repo. Can use Env vars on the running image or just insert below.
domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
// gitUsername = System.getenv("GIT_USERNAME") ?: "jenkins-user"
// gitPassword = System.getenv("GIT_PASSWORD") ?: "password-for-user"
gitUsername = "cip_build_devops-expert-tech"
gitPassword = "changeit"
usernameAndPassword = new UsernamePasswordCredentialsImpl(
		CredentialsScope.GLOBAL,
		gitUsername, "Dummy Git creds for Jenkins, will be changed by _initJenkins job",
		gitUsername,
		gitPassword
		)
store.addCredentials(domain, usernameAndPassword)



//////////////////////////
//
// Annoymouse access for git webhooks to trigger builds
//
//////////////////////////
LOG.log(Level.INFO,  "Add annoymouse user" )
def strategy = new GlobalMatrixAuthorizationStrategy()
//  Setting Anonymous Permissions
strategy.add(hudson.model.Item.BUILD,'anonymous')
strategy.add(hudson.model.Item.CANCEL,'anonymous')
def instance = Jenkins.getInstance()
instance.setAuthorizationStrategy(strategy)
instance.save()



//////////////////////////
//
// Admin user
//
//////////////////////////
LOG.log(Level.INFO,  "Add admin user" )
jenkins = Jenkins.instance
jenkins.securityRealm = new HudsonPrivateSecurityRealm(false,false,null)
jenkins.securityRealm.createAccount('admin', 'admin')
// jenkins.securityRealm.createAccount('agent', 'agent')

class BuildPermission {
	static buildNewAccessList(userOrGroup, permissions) {
		def newPermissionsMap = [:]
		permissions.each {
			  newPermissionsMap.put(Permission.fromId(it), userOrGroup)
		}
		newPermissionsMap
	}
}
strategy = new ProjectMatrixAuthorizationStrategy()
jenkins.authorizationStrategy = strategy

administratorPermissions = ["hudson.model.Hudson.Administer"]
agentPermissions = ["hudson.model.Computer.Build", "hudson.model.Computer.Configure", "hudson.model.Computer.Connect", "hudson.model.Computer.Create", "hudson.model.Computer.Delete", "hudson.model.Computer.Disconnect"]
anonymousPermissions = ["hudson.model.Hudson.Read", "hudson.model.Item.Discover"]

BuildPermission.buildNewAccessList("admin", administratorPermissions).each { p, u -> strategy.add(p, u) }
// BuildPermission.buildNewAccessList("anonymous", anonymousPermissions).each { p, u -> strategy.add(p, u) }
// BuildPermission.buildNewAccessList("agent", agentPermissions).each { p, u -> strategy.add(p, u) }

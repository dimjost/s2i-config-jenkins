#!/usr/bin/env groovy
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

import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")
LOG.log(Level.INFO,  "########## Running configure-credentials.groovy ##########" )

// USE THE SAME GIT USER LIKE FOR SHARED-LIBRARYS
// create jenkins creds for commiting tags back to repo. Can use Env vars on the running image or just insert below.
domain = Domain.global()
store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
gitUsername = "cip_build_devops-expert-tech"
gitPassword = "changeit"
usernameAndPassword = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  gitUsername, "Dummy Git creds for Jenkins, will be changed by _initJenkins job",
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
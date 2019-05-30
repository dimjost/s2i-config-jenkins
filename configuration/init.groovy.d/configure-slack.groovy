#!/usr/bin/env groovy
import jenkins.model.Jenkins
import net.sf.json.JSONObject

import java.util.logging.Level
import java.util.logging.Logger

final def LOG = Logger.getLogger("LABS")
LOG.log(Level.INFO,  "########## Running configure-slack.groovy ##########")

def slackBaseUrl = System.getenv('SLACK_BASE_URL')
LOG.log(Level.INFO,  "slack.SLACK_BASE_URL: '${slackBaseUrl}'")

if(slackBaseUrl != null && slackBaseUrl?.trim()) {

  LOG.log(Level.INFO,  "Configuring slack... for: ${slackBaseUrl}" )
  
  def slackToken = System.getenv('SLACK_TOKEN')
  LOG.log(Level.INFO,  "slack.SLACK_TOKEN: ${slackToken}")
  
  def slackRoom = System.getenv('SLACK_ROOM')
  LOG.log(Level.INFO,  'slack.SLACK_ROOM: ' + slackRoom)
  
  def slackSendAs = ''
  def slackTeamDomain = ''
  def slackTokenCredentialId = System.getenv('SLACK_TOKEN_CREDENTIAL_ID')
  LOG.log(Level.INFO,  'slack.SLACK_TOKEN_CREDENTIAL_ID: ' + slackTokenCredentialId)

  if(slackTokenCredentialId == null) {
    slackTokenCredentialId = ''
  }

  JSONObject formData = ['slack': ['tokenCredentialId': slackTokenCredentialId]] as JSONObject
  LOG.log(Level.INFO,  'slack.formData: ' + formData)
  
  def slack = Jenkins.instance.getExtensionList(
    jenkins.plugins.slack.SlackNotifier.DescriptorImpl.class
  )[0]
  LOG.log(Level.INFO,  'slack.slack: ' + slack)
  
  def params = [
    slackBaseUrl: slackBaseUrl,
    slackTeamDomain: slackTeamDomain,
    slackToken: slackToken,
    slackRoom: slackRoom,
    slackSendAs: slackSendAs
  ]
  LOG.log(Level.INFO,  'slack.params: ' + params)
  
  def req = [
    getParameter: { name -> params[name] }
  ] as org.kohsuke.stapler.StaplerRequest
  LOG.log(Level.INFO,  'slack.req: ' + req)
  
  slack.configure(req, formData)

  LOG.log(Level.INFO,  'Configured slack' )

  slack.save()
}
else {
	LOG.log(Level.INFO,  "Skip Slack Configuring..." )
}
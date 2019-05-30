import hudson.model.*;
import jenkins.model.*;
import hudson.tools.*;
import hudson.plugins.groovy.*;

def env = System.getenv();
def groovyVersion = "2.5.6" // can be defined as a single version or a comma separated string of versions (like maven)
def groovyVersionList = groovyVersion.split(',')

def instance = Jenkins.getInstance()

println "Configuring Groovy"

def groovyDescriptor = instance.getDescriptor("hudson.plugins.groovy.GroovyInstallation")
def groovyInstallations = groovyDescriptor.getInstallations()

groovyVersionList.eachWithIndex { version, index ->
	def installer = new GroovyInstaller(version)
	def installSourceProperty = new InstallSourceProperty([installer])

	def name = "Groovy_" + version

	if (index == 0) {
		name = "Groovy"
	}

	def installation = new GroovyInstallation(
			name,
			"", [installSourceProperty])

	def groovyIntExists = false
	groovyInstallations.each {
		currentInstallation = (GroovyInstallation) it
		if (installation.getName() == currentInstallation.getName()) {
			groovyIntExists = true
			println("Found existing installation: " + installation.getName())
		}
	}

	if (!groovyIntExists) {
		groovyInstallations += installation
	}
}

groovyDescriptor.setInstallations((GroovyInstallation[]) groovyInstallations)

instance.save()
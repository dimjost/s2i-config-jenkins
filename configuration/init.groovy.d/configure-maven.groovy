import hudson.model.*;
import jenkins.model.*;
import hudson.tools.*;
import hudson.tasks.Maven.MavenInstaller;
import hudson.tasks.Maven.MavenInstallation;

def env = System.getenv()

// Variables
// MAVEN_VERSION can be defined as a single version or a comma separated string of versions
// eg.
// MAVEN_VERSION=3.0.5
// MAVEN_VERSION=3.0.5,3.2.5

// TO automatic installation
def maven_version = "3.6.0"
def maven_version_list = maven_version.split(',')

def maven_version_on_slave = "apache-maven-3.6.1,mvn3.6.1"
def maven_version_on_slave_list = maven_version_on_slave.split(',')

// Constants
def instance = Jenkins.getInstance()


// Maven automatic installation
println "Configuring Maven (automatic installation)"
def desc_MavenTool = instance.getDescriptor("hudson.tasks.Maven")
def maven_installations = desc_MavenTool.getInstallations()

maven_version_list.eachWithIndex { version, index ->
	def mavenInstaller = new MavenInstaller(version)
	def installSourceProperty = new InstallSourceProperty([mavenInstaller])
		
	def name="maven_" + version

	// This makes the solution backwards-compatible, and will treat the first version in the array as "Maven"
//	if (index == 0) {
//		name="Maven"
//	}

	def maven_inst = new MavenInstallation(
		name, // Name
		"", // Home
		[installSourceProperty]
	)

	// Only add a Maven installation if it does not already exist - do not overwrite existing config
	def maven_inst_exists = false
	maven_installations.each {
		installation = (MavenInstallation) it
		if ( maven_inst.getName() ==  installation.getName() ) {
				maven_inst_exists = true
				println("Found existing installation: " + installation.getName())
		}
	}
		
	if (!maven_inst_exists) {
		maven_installations += maven_inst
	}
}

desc_MavenTool.setInstallations((MavenInstallation[]) maven_installations)
desc_MavenTool.save()
	
	
println "Configuring Maven (installed on slave)"
maven_version_on_slave_list.eachWithIndex { version, index ->
	installedMavenDescriptor = instance.getExtensionList(hudson.tasks.Maven.DescriptorImpl.class)[0];
	installedMavenList = (installedMavenDescriptor.installations as List);
	installedMavenList.add(new hudson.tasks.Maven.MavenInstallation(version, "/opt/tools/maven/" + version, []));
	installedMavenDescriptor.installations=installedMavenList
	installedMavenDescriptor.save()
}

// Save the state
instance.save()

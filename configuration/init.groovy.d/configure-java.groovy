import jenkins.model.*
import hudson.model.*
import hudson.tools.*



def java_version_on_slave = "java-11-openjdk"
def java_version_on_slave_list = java_version_on_slave.split(',')

def inst = Jenkins.getInstance()
def desc = inst.getDescriptor("hudson.model.JDK")
def installations = [];


println "Configuring Java (installed on slave)"
java_version_on_slave_list.eachWithIndex { version, index ->
	// def installer = new JDKInstaller(version, false)
	// def installerProps = new InstallSourceProperty([installer])
	def installation = new JDK(version, "/opt/java/" + version, [])
	installations.push(installation)
}


desc.setInstallations(installations.toArray(new JDK[0]))

desc.save()
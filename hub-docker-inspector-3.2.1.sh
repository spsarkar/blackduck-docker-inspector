#!/bin/bash
#
# This script runs on the host machine, and uses hub-docker-inspector images/containers
# to inspect the given Docker image.
#
# Run this script from the directory that contains the application.properties, configured
# with your Hub connection details (hub.url, hub.username, and hub.password),
# and Docker Hub connection details (docker.registry.username and docker.registry.password).

function printUsage() {
	echo ""
    echo "Usage: $0 [options] <image>"
    echo "<image> can be in either of two forms:"
    echo "	<docker image name>[:<docker image version>]"
    echo "	<saved image tarfile; must have .tar extension>"
    echo "options: any property from application.properties can be set by adding an option of the form:"
    echo "  --<property name>=<value>"
    echo ""
    echo "Run this command from the directory that contains the application.properties,"
    echo "configured with your Hub connection details (hub.url, hub.username, and hub.password),"
	echo "and Docker Hub connection details (docker.registry.username and docker.registry.password)."
	echo ""
	echo "For greater security, the Hub password can be set via the environment variable BD_HUB_PASSWORD"
	echo ""
	echo "For example:"
	echo "  export BD_HUB_PASSWORD=mypassword"
	echo "  $0 --hub.url=http://hub.mydomain.com:8080/ --hub.username=myusername ubuntu"
	echo ""
	echo "Documentation: https://blackducksoftware.atlassian.net/wiki/spaces/INTDOCS/pages/48435867/Hub+Docker+Inspector"
}

# Write message to stdout
log() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: $@"
}

# Write warning to stdout
warn() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: WARNING: $@"
}

# Write an error message to stderr
err() {
  echo "[$(date +'%Y-%m-%dT%H:%M:%S%z')]: ERROR: $@" >&2
}

# Expand tilde
function expandPath() {
	echo "${@/#~/$HOME}"
}

# escape spaces
function escapeSpaces() {
	echo "${@// /%20}"
}

# Look through args for ones this script needs to act on
function preProcessOptions() {
	cmdlineargindex=0
	for cmdlinearg in "$@"
	do
		if [[ "$cmdlinearg" == --spring.config.location=* ]]
		then
			propdir=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			if [[ "${propdir}" == file:* ]]
			then
				# trim off the file: prefix
				propdir="${propdir:5}"
			else
				warn "This format for the value of --spring.config.location has been deprecated. The value should be: file:<dir>/application.properties"
			fi
			if [[ "${propdir}" == */application.properties ]]
			then
				# trim off the /application.properties suffix
				oldLength=${#propdir}
				newLength=$(($oldLength-23))
				propdir="${propdir:0:$newLength}"
			else
				warn "This format for the value of --spring.config.location has been deprecated. The value should be: file:<dir>/application.properties"
			fi
			propdir=$(expandPath "${propdir}")
			if [[ "$propdir" == */ ]]
			then
				propdir=$(echo "$propdir" | rev | cut -c 2- | rev)
			fi
		elif [[ "$cmdlinearg" == --hub.password=* ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			hub_password_set_on_cmd_line=true
		elif [[ "$cmdlinearg" == --output.path=* ]]
		then
			outputPath=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			outputPath=$(expandPath "${outputPath}")
			options[${cmdlineargindex}]="--output.path=${outputPath}"
		elif [[ "$cmdlinearg" == --working.dir.path=* ]]
		then
			workingDir=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			workingDir=$(expandPath "${workingDir}")
			options[${cmdlineargindex}]="--working.dir.path=${workingDir}"
		elif [[ "$cmdlinearg" == --jar.path=* ]]
		then
			jarPath=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			jarPath=$(expandPath "${jarPath}")
			jarPathEscaped=$(escapeSpaces "${jarPath}")
			options[${cmdlineargindex}]="--jar.path=${jarPathEscaped}"
			jarPathAlreadySet=true
		elif [[ "$cmdlinearg" == --no.prompt=true ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			noPromptMode=true
		elif [[ "$cmdlinearg" == --dry.run=true ]]
		then
			options[${cmdlineargindex}]="${cmdlinearg}"
			dryRunMode=true
		elif [[ "$cmdlinearg" == --hub.username=* ]]
		then
			hubUsername=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			hubUsernameEscaped=$(escapeSpaces "${hubUsername}")
			hubUsernameArgument="--hub.username=${hubUsernameEscaped}"
		elif [[ "$cmdlinearg" == --hub.project.name=* ]]
		then
			hubProjectName=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			hubProjectNameEscaped=$(escapeSpaces "${hubProjectName}")
			hubProjectNameArgument="--hub.project.name=${hubProjectNameEscaped}"
		elif [[ "$cmdlinearg" == --hub.project.version=* ]]
		then
			hubProjectVersion=$(echo "$cmdlinearg" | cut -d '=' -f 2)
			hubProjectVersionEscaped=$(escapeSpaces "${hubProjectVersion}")
			hubProjectVersionArgument="--hub.project.version=${hubProjectVersionEscaped}"
		else
			if [[ ${cmdlineargindex} -eq $(( $# - 1)) ]]
			then
				image="${cmdlinearg}"
			else
				options[${cmdlineargindex}]="${cmdlinearg}"
			fi
		fi
		(( cmdlineargindex += 1 ))
	done
}

# Prompt user for Hub Password
function promptForHubPassword() {
	read -s -p "Hub Password has not been set. Please enter Hub password: " hubPassword
	echo ""
	export BD_HUB_PASSWORD="${hubPassword}"
}

# Inform user on whether password is set via env var
function checkForPassword() {
	if [ $hub_password_set_on_cmd_line == true -o -z "${BD_HUB_PASSWORD}" ]
	then
   	    log Environment variable BD_HUB_PASSWORD is not set or is being overridden on the command line
	else
        log BD_HUB_PASSWORD is set
	fi
	passwordFromConfigFile=$(getProperty "${propfile}" "hub.password")
	if [ $hub_password_set_on_cmd_line == false -a -z "${BD_HUB_PASSWORD}" -a $dryRunMode == false -a -z "${passwordFromConfigFile}" ]
	then
   	    
		if [ $noPromptMode == false ]
		then
			promptForHubPassword
		else
			err "The Hub password has not been provided, and \"no prompt\" mode is enabled"
			exit -1
		fi
	fi
}

# Get a property value from the given properties file
# Usage: getProperty FILE KEY
function getProperty {
	grep "^$2=" "$1" 2> /dev/null | cut -d'=' -f2
}

# Pull the latest jar down to the current working directory
function pullJar {
	log "Getting hub-docker-inspector.jar from github"
	jarUrl="https://blackducksoftware.github.io/hub-docker-inspector/hub-docker-inspector-${version}.jar"
	curl --fail -O  ${jarUrl}
	curlStatus=$?
	if [[ "${curlStatus}" != "0" ]]
	then
		err "curl returned ${curlStatus} fetching ${jarUrl}. If you have the hub-docker-inspector .jar file, you can set the jar.path property to the path to the .jar file"
		exit ${curlStatus}
	fi
}

##################
# Start script
##################
version="3.2.1"
encodingSetting="-Dfile.encoding=UTF-8"
outputPath=""
propdir=.
hub_password_set_on_cmd_line=false
noPromptMode=false
dryRunMode=false
workingDir=""
createdWorkingDir=false
jarPath=""
jarPathAlreadySet=false
hubUsernameArgument=""
hubProjectNameArgument=""
hubProjectVersionArgument=""

if [ $# -lt 1 ]
then
    printUsage
    exit -1
fi

if [ \( "$1" = -v \) -o \( "$1" = --version \) ]
then
	echo "$(basename $0) ${version}"
	exit 0
fi

if [ \( "$1" = -h \) -o \( "$1" = --help \) ]
then
    printUsage
    exit 0
fi

if [ \( "$1" = -j \) -o \( "$1" = --pulljar \) ]
then
    pullJar
    exit 0
fi

preProcessOptions "$@"
propfile="${propdir}/application.properties"
log "Properties file: ${propfile}"

if [ -z "${outputPath}" ]
then
	outputPath=$(getProperty "${propfile}" "output.path")
fi
log "Output path: ${outputPath}"

if [ -z "${workingDir}" ]
then
	workingDir=$(getProperty "${propfile}" "working.dir.path")
fi
if [ -z "${workingDir}" ]
then
	workingDir="$(mktemp -d)"
	createdWorkingDir=true
fi

if [ -z "${jarPath}" ]
then
	jarPath=$(getProperty "${propfile}" "jar.path")
	jarPath=$(expandPath "${jarPath}")
fi
if [ -z "${jarPath}" ]
then
	pushd "${workingDir}" > /dev/null
	pullJar
	popd > /dev/null
	jarPath="${workingDir}/hub-docker-inspector-${version}.jar"
fi
log "Jar path: ${jarPath}"

checkForPassword
newJarPathAssignment=""
if [[ $jarPathAlreadySet == false ]]
then
	newJarPathAssignment="--jar.path=${jarPath}"
fi

if [ -e "${propfile}" ]
then
	log "Copying ${propfile} to ${workingDir}"
	cp "${propfile}" "${workingDir}"
	options+=("--spring.config.location=file:${workingDir}/application.properties")
else
	log "${propfile} does not exist"
fi

log "jarPath: ${jarPath}"
log "Options: ${options[*]}"

if [[ "$image" == *.tar ]]
then
	log "Inspecting image tar file: $image"
	if [ ! -r "${image}" ]
	then
		err "Tar file ${image} does not exist"
		exit -1
	fi
	java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarPath}" "${newJarPathAssignment}" ${hubUsernameArgument} ${hubProjectNameArgument} ${hubProjectVersionArgument} "--docker.tar=$image" "--host.working.dir.path=${workingDir}" ${options[*]}
	status=$?
else
	log Inspecting image: $image
	java "${encodingSetting}" ${DOCKER_INSPECTOR_JAVA_OPTS} -jar "${jarPath}" "${newJarPathAssignment}" ${hubUsernameArgument} ${hubProjectNameArgument} ${hubProjectVersionArgument} "--docker.image=$image" "--host.working.dir.path=${workingDir}" ${options[*]}
	status=$?
fi

if [[ "${status}" -ne "0" ]]
then
	exit "${status}"
fi

if [ ! -z "${outputPath}" ]
then
	if [ -f "${outputPath}" ]
	then
		err "Unable to copy BDIO output file to ${outputPath} because it is an existing file"
		exit -2
	fi
	if [ ! -e "${outputPath}" ]
	then
		mkdir -p "${outputPath}"
	fi
	log "Copying output to ${outputPath}"
	cp "${workingDir}"/output/* "${outputPath}"
fi

numSuccessMessages=$(grep "\"succeeded\":[ 	]*true" "${workingDir}"/output//result.json | wc -l)
if [[ "${numSuccessMessages}" -eq "1" ]]
then
	log "Succeeded"
	status=0
else
	err "Failed"
	status=1
fi

if [ $createdWorkingDir == true ]
then
	log "Removing ${workingDir}"
	rm -rf ${workingDir}
fi

exit ${status}
def createJob = freeStyleJob("${PROJECT_NAME}/Environment_Provisioning/Set_Provisioning_Parameters") 
def scmProject = "git@gitlab:${WORKSPACE_NAME}/peoplesoft_855_provisioning.git"
def scmCredentialsId = "adop-jenkins-master"

folder("${PROJECT_NAME}/Environment_Provisioning") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

createJob.with {
    description('')
    parameters {
        choiceParam('APPLICATION_NAME', ['HCM', 'FSCM'], 'PeopleSoft Application to install.')
        choiceParam('PEOPLESOFT_USER_ID', ['PS', 'VP1'], 'Application Specific User ID. (Note: PS will be use for HCM. VP1 will be use for FSCM)')
        choiceParam('INITIAL_APPLICATION', ['TRUE', 'FALSE'], 'Specify whether the application you\'re installing is the initial or first application being install.')
        stringParam('DATABASE_HOST_IP', '', 'Target Database Server Hostname.')
        stringParam('APPLICATION_HOST_IP', '', 'Target Application Server Hostname')
        stringParam('PEOPLESOFT_WEBSITE_NAME', '', 'Unique name added to PS application web url. ( eg. http://publicIP:port/<peoplesoft_website_name>/signon.html)')
        choiceParam('WEB_PROFILE_NAME', ['DEV', 'TEST', 'PROD', 'KIOSK'], 'PeopleSoft Internet Architecture web profile name')
        choiceParam('MULTITENANT', ['TRUE', 'FALSE'], 'Select \'TRUE\' if you are installing Database in Multitenant Architecture. Select \'FALSE\' otherwise.')
        stringParam('CONTAINER_DATABASE_NAME', '', 'Container database name that will host the pluggable database or your peoplesoft database.')
        stringParam('PLUGGABLE_DATABASE_NAME', '', 'Pluggable database name. Leave blank if you are installing a Non-Multitenant Database.')
        stringParam('APP_DOMAIN_ID', '', 'Application server\'s domain ID. (eg. TESTSERV)')
        stringParam('PIA_DOMAIN', '', 'PeopleSoft Internet Architecture Domain ID. (eg. peoplesoft)')
        stringParam('HTTP_PORT', '8000', '	By default, port 8000 will be used. If changing ports, please specify a port that is not used.')
        stringParam('HTTPS_PORT', '4435', 'By default, port 4435 will be used. If changing ports, please specify a port that is not used.')
        stringParam('WSL_PORT', '7000', 'By default, port 7000 will be used. If changing ports, please specify a port that is not used.')
        stringParam('JSL_PORT', '9000', 'By default, port 9000 will be used. If changing ports, please specify a port that is not used.')
        stringParam('JRAD_PORT', '9100', 'By default, port 9100 will be used. If changing ports, please specify a port that is not used.')
        stringParam('DB_PORT', '1521', 'By default, port 1521 will be used. If changing ports, please specify a port that is not used.')
    }
    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('ansible')

    scm {
        git {
            remote {
                url(scmProject)
                credentials(scmCredentialsId)
            }
            branch('*/master')
        }
    }
    wrappers {
        preBuildCleanup() 
        colorizeOutput('css')
        sshAgent('ansible-user-key')
    }
    steps {
        shell('''#!/bin/bash

cat > target-hosts <<-EOF
[app]
${APPLICATION_HOST_IP}
[db]
${DATABASE_HOST_IP}
EOF

# ==> Create Properties File

if [ ${MULTITENANT} == TRUE ];
then
    echo "MAIN=xtenant" > props

elif [ ${MULTITENANT} == FALSE ];
then
    echo "MAIN=ntenant" > props
fi

echo "APP_HOST=${APPLICATION_HOST_IP}" >> props
echo "DB_HOST=${DATABASE_HOST_IP}" >> props
echo "INITIAL_APP=${INITIAL_APPLICATION}" >> props
echo "APP_NAME=${APPLICATION_NAME}" >> props
echo "CUSTOM_WORKSPACE=${WORKSPACE}" >> props
echo "ANSIBLE_CFG=${WORKSPACE}/ssh_ansible.cfg" >> props
        ''')
        environmentVariables {
            propertiesFile('props')
        }
    }
    publishers {

        archiveArtifacts('**/*')

        flexiblePublish {
            conditionalAction {
                condition {
                    stringsMatch('${INITIAL_APP}', 'TRUE', true)
                    publishers {
                        downstreamParameterized {
                            trigger('Install_Database, Install_Application_Components') {
                                condition('SUCCESS')
                                parameters {
                                    currentBuild()
                                    propertiesFile('props', true)
                                }
                            }
                        }
                    }
                }
            }
            conditionalAction {
                condition {
                    stringsMatch('${INITIAL_APP}', 'FALSE', true)
                    publishers {
                        downstreamParameterized {
                            trigger('Install_Application') {
                                condition('SUCCESS')
                                parameters {
                                    currentBuild()
                                    propertiesFile('props', true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
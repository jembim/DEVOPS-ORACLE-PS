def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Install_Application')

createJob.with {
    description('')
    parameters {
        stringParam('APPLICATION_NAME', '', '')
        stringParam('PEOPLESOFT_USER_ID', '', '')
        stringParam('INITIAL_APPLICATION', '', '')
        stringParam('DATABASE_HOST_IP', '', '')
        stringParam('APPLICATION_HOST_IP', '', '')
        stringParam('PEOPLESOFT_WEBSITE_NAME', '', '')
        stringParam('WEB_PROFILE_NAME', '', '')
        stringParam('MULTITENANT', '', '')
        stringParam('CONTAINER_DATABASE_NAME', '', '')
        stringParam('PLUGGABLE_DATABASE_NAME', '', '')
        stringParam('APP_DOMAIN_ID', '', '')
        stringParam('PIA_DOMAIN', '', '')
        stringParam('HTTP_PORT', '', '')
        stringParam('HTTPS_PORT', '', '')
        stringParam('WSL_PORT', '', '')
        stringParam('JSL_PORT', '', '')
        stringParam('JRAD_PORT', '', '')
        stringParam('DB_PORT', '', '')
        stringParam('CUSTOM_WORKSPACE', '', '')
        stringParam('ANSIBLE_CFG', '', '')
        stringParam('MAIN', '', '')
    }

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('ansible')
    customWorkspace('$CUSTOM_WORKSPACE')

    wrappers {
        preBuildCleanup() 
        colorizeOutput('css')
        sshAgent('ansible-user-key')
    }

    steps {

        copyArtifacts('Set_Provisioning_Parameters') {
            includePatterns('**/*')
            fingerprintArtifacts(true)
            buildSelector {
                upstreamBuild(true)
                latestSuccessful(false)
            }
        }

        shell('''#!/bin/bash
export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

if [ ${APPLICATION_NAME} == HCM ];
then
	echo "Running HCM Installation"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app multi_architecture=${MULTITENANT}" \
    --tags "hcm_install" ${MAIN}.yml

elif [ ${APPLICATION_NAME} == FSCM ];
then
	echo "Running FSCM Installation"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app multi_architecture=${MULTITENANT}" \
    --tags "fscm_install" ${MAIN}.yml

else
	echo "Application Not Found"
fi
        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Create_PS_Database') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
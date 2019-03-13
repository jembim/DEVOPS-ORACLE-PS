def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Install_PeopleSoft_Internet_Architecture')

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

        copyArtifacts('Configure_Application_Server') {
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

echo "# ====================================================================================== #"
echo "# ====================> INSTALLING PEOPLESOFT INTERNET ARCHITECTURE < ================== #"
echo "# ====================================================================================== #"

if [ ${APPLICATION_NAME} == HCM ];
then
	echo "Installing PIA for HCM...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    app_host=${APPLICATION_HOST_IP} pia_domain=${PIA_DOMAIN} \
    website_name=${PEOPLESOFT_WEBSITE_NAME} web_profile_name=${WEB_PROFILE_NAME} \
    http_port=${HTTP_PORT} https_port=${HTTPS_PORT} jsl_port=${JSL_PORT}" \
    --tags "pia_install_hcm" ${MAIN}.yml

elif [ ${APPLICATION_NAME} == FSCM ];
then
	echo "Installing PIA for FSCM...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    app_host=${APPLICATION_HOST_IP} pia_domain=${PIA_DOMAIN} \
    website_name=${PEOPLESOFT_WEBSITE_NAME} web_profile_name=${WEB_PROFILE_NAME} \
    http_port=${HTTP_PORT} https_port=${HTTPS_PORT} jsl_port=${JSL_PORT}" \
    --tags "pia_install_fscm" ${MAIN}.yml
fi

sleep 60

echo "# =============================================================================== #"
echo "# ================> STARTING PEOPLESOFT INTERNET ARCHITECTURE <================== #"
echo "# =============================================================================== #"

ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app pia_domain=${PIA_DOMAIN}" --tags "start_pia" ${MAIN}.yml
        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Setup_Process_Scheduler') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
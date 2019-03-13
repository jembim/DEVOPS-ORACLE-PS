def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Configure_Application_Server')

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

        copyArtifacts('Completing_Database_Setup') {
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

echo "# ================================================================= #"
echo "# ===============> CONFIGURING APPLICATION SERVER <================ #"
echo "# ================================================================= #"

if [ ${APPLICATION_NAME} == HCM ];
then
	echo "Configuring Application Server for HCM...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    app_domain_id=${APP_DOMAIN_ID} wsl_port=${WSL_PORT} jsl_port=${JSL_PORT} \
    jrad_port=${JRAD_PORT} database_sid=${CONTAINER_DATABASE_NAME} multi_architecture=${MULTITENANT}" \
    --tags "var_condi,config_app_hcm" ${MAIN}.yml

elif [ ${APPLICATION_NAME} == FSCM ];
then
	echo "Configuring Application Server for FSCM...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    app_domain_id=${APP_DOMAIN_ID} wsl_port=${WSL_PORT} jsl_port=${JSL_PORT} \
    jrad_port=${JRAD_PORT} database_sid=${CONTAINER_DATABASE_NAME} multi_architecture=${MULTITENANT}" \
    --tags "var_condi,config_app_fscm" ${MAIN}.yml
    
fi
        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Install_PeopleSoft_Internet_Architecture') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
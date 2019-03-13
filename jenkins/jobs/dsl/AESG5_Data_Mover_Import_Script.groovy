def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Data_Mover_Import_Script')

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

        copyArtifacts('Create_PS_Database') {
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

echo "# ====================================================================== #"
echo "# ============> Running Data Mover Import Script <====================== #"
echo "# ====================================================================== #"

ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app" --tags "ps_env" ${MAIN}.yml

if [ ${APPLICATION_NAME} == HCM ];
then
	echo "Running HCM Specific DMS...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_sid=${CONTAINER_DATABASE_NAME} \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME}" --tags "import_db_hcm" ${MAIN}.yml

elif [ ${APPLICATION_NAME} == FSCM ];
then
	echo "Running FSCM Specific DMS...."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_sid=${CONTAINER_DATABASE_NAME} \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME}" --tags "import_db_fscm" ${MAIN}.yml
fi

        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Completing_Database_Setup') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
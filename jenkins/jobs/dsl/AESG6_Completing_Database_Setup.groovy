def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Completing_Database_Setup')

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

        copyArtifacts('Data_Mover_Import_Script') {
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

echo "# ======================================================================== #"
echo "# ====================> COMPLETING DATABASE SETUP <======================= #"
echo "# ======================================================================== #"

if [ ${APPLICATION_NAME} == HCM ];
then
	echo "--------- TAG: SYS_TABLES -----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "sys_tables_hcm" ${MAIN}.yml

	echo "--------- TAG: UPDATE_DB ------------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    database_sid=${CONTAINER_DATABASE_NAME} db_host=${DATABASE_HOST_IP}" \
    --tags "update_db_hcm" ${MAIN}.yml
    
	echo "--------- TAG: UPDATE_DATA ----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    db_host=${DATABASE_HOST_IP} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "update_data_hcm" ${MAIN}.yml

	echo "--------- TAG: APP_ENGINE ----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    database_sid=${CONTAINER_DATABASE_NAME}" --tags "app_engine_hcm" ${MAIN}.yml

	echo "--------- TAG: SQR_REPORTS ---------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "sqr_reports_hcm" ${MAIN}.yml

elif [ ${APPLICATION_NAME} == FSCM ];
then
	echo "--------- TAG: SYS_TABLES -----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "sys_tables_fscm" ${MAIN}.yml

	echo "--------- TAG: UPDATE_DB ------------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    database_sid=${CONTAINER_DATABASE_NAME} db_host=${DATABASE_HOST_IP}" \
    --tags "update_db_fscm" ${MAIN}.yml
    
	echo "--------- TAG: UPDATE_DATA ----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    db_host=${DATABASE_HOST_IP} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "update_data_fscm" ${MAIN}.yml

	echo "--------- TAG: APP_ENGINE ----------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    ps_user_id=${PEOPLESOFT_USER_ID} database_pdb_name=${PLUGGABLE_DATABASE_NAME} \
    database_sid=${CONTAINER_DATABASE_NAME}" --tags "app_engine_fscm" ${MAIN}.yml

	echo "--------- TAG: SQR_REPORTS ---------"
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
    database_pdb_name=${PLUGGABLE_DATABASE_NAME} database_sid=${CONTAINER_DATABASE_NAME}" \
    --tags "sqr_reports_fscm" ${MAIN}.yml
fi
        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Configure_Application_Server') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}


def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Create_PS_Database')

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

        copyArtifacts('Install_Application') {
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

if [ ${MULTITENANT} == TRUE ];
then

    if [ ${INITIAL_APPLICATION} == TRUE ];
    then
	    echo "Creating INIT.ORA File..."
	    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "init_ora" ${MAIN}.yml


    elif [ ${INITIAL_APPLICATION} == FALSE ];
    then
    	echo "Adding Entry to LISTENER.ORA and TNSNAMES.ORA"
	    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_pdb_name=${PLUGGABLE_DATABASE_NAME} db_host=${DATABASE_HOST_IP} db_port=${DB_PORT}" \
        --tags "tns_db" ${MAIN}.yml

        echo "Adding Entry to LISTENER.ORA and TNSNAMES.ORA"
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app \
        database_pdb_name=${PLUGGABLE_DATABASE_NAME} db_host=${DATABASE_HOST_IP} db_port=${DB_PORT}" \
        --tags "tns_app" ${MAIN}.yml
    fi

elif [ ${MULTITENANT} == FALSE ];
then

	echo "Creating INIT.ORA File..."
	ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
    database_sid=${CONTAINER_DATABASE_NAME}" --tags "init_ora" ${MAIN}.yml

fi


echo "# ================================================================ #"
echo "# ===================> PREPARING SQL SCRIPT <===================== #"
echo "# ================================================================ #"

if [ ${MULTITENANT} == TRUE ];
then

    if [ ${APPLICATION_NAME} == HCM ];
    then
        echo "Migrating HCM Specific Sql Scripts" 
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app" \
        --tags "migrate_hcm" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db" \
        --tags "migrate_db" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "prepare_script" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "prepare_hcm" ${MAIN}.yml

    elif [ ${APPLICATION_NAME} == FSCM ];
    then
        echo "Migrating FSCM Specific Sql Scripts" 
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app" \
        --tags "migrate_fscm" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db" \
        --tags "migrate_db" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "prepare_script" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "prepare_fscm" ${MAIN}.yml
    else
        echo "SCRIPTS NOT FOUND!!"
    fi

elif [ ${MULTITENANT} == FALSE ];
then

    if [ ${APPLICATION_NAME} == HCM ];
    then
        echo "Migrating HCM Specific Sql Scripts" 
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app" \
        --tags "migrate_hcm" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db" \
        --tags "migrate_db" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "prepare_script" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "prepare_hcm" ${MAIN}.yml

    elif [ ${APPLICATION_NAME} == FSCM ];
    then
        echo "Migrating FSCM Specific Sql Scripts" 
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=app" \
        --tags "migrate_fscm" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db" \
        --tags "migrate_db" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "prepare_script" ${MAIN}.yml
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "prepare_fscm" ${MAIN}.yml
    else
        echo "SCRIPTS NOT FOUND!!"
    fi

fi
    
        ''')

        shell('''#!/bin/bash
export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

echo "#===================================================================================#"
echo "#===================> CREATING ROOT AND PLUGGABLE DATABASE <========================#"
echo "#===================================================================================#"

if [ ${MULTITENANT} == TRUE ];
then

    if [ ${INITIAL_APPLICATION} == TRUE ];
    then
        echo "Creating Root Container Database...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "create_cdb" ${MAIN}.yml

        echo "Creating PDB For ${APPLICATION_NAME}"
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "create_pdb" ${MAIN}.yml

        echo "Start Listener"
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "start_lsnr" ${MAIN}.yml

    elif [ ${INITIAL_APPLICATION} == FALSE ];
    then
        echo "Shutdown Running PDB...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "stop_pdb" ${MAIN}.yml
        
        echo "Creating New PDB For ${APPLICATION_NAME}"
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "create_pdb" ${MAIN}.yml
        
        echo "Starting All PDB...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "start_pdb" ${MAIN}.yml
    fi

elif [ ${MULTITENANT} == FALSE ];
then

    echo "Creating Oracle Instance...."
    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
    database_sid=${CONTAINER_DATABASE_NAME}" --tags "create_db_a" ${MAIN}.yml

fi
        ''')

        shell('''#!/bin/bash
export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

echo "#==============================================================================#"
echo "#================> CREATING SPECIFIC APPLICATION SCHEMA <======================#"
echo "#==============================================================================#"

if [ ${MULTITENANT} == TRUE ];
then

    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
    database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" --tags \
    "create_db_a" ${MAIN}.yml

elif [ ${MULTITENANT} == FALSE ];
then

    echo "Continuing to Application Specific Schema Creation"

fi

if [ ${MULTITENANT} == TRUE ];
then
    if [ ${APPLICATION_NAME} == HCM ];
    then
        echo "Running HCM Specific SQL Configurations...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "hcm_db" ${MAIN}.yml

    elif [ ${APPLICATION_NAME} == FSCM ];
    then
        echo "Running FSCM Specific SQL Configurations...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" \
        --tags "fscm_db" ${MAIN}.yml
    fi

elif [ ${MULTITENANT} == FALSE ];
then

    if [ ${APPLICATION_NAME} == HCM ];
    then
        echo "Running HCM Specific SQL Configurations...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "hcm_db" ${MAIN}.yml

    elif [ ${APPLICATION_NAME} == FSCM ];
    then
        echo "Running FSCM Specific SQL Configurations...."
        ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
        database_sid=${CONTAINER_DATABASE_NAME}" --tags "fscm_db" ${MAIN}.yml
    fi

fi

if [ ${MULTITENANT} == TRUE ];
then

    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
    database_sid=${CONTAINER_DATABASE_NAME} database_pdb_name=${PLUGGABLE_DATABASE_NAME}" --tags \
    "create_db_b" ${MAIN}.yml

elif [ ${MULTITENANT} == FALSE ];
then

    ansible-playbook -i target-hosts -u opc --become --become-user root -e "target_server=db \
    database_sid=${CONTAINER_DATABASE_NAME}" --tags "create_db_b" ${MAIN}.yml

fi
        ''')
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Data_Mover_Import_Script') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}
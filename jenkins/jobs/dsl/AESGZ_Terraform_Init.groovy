def createJob = freeStyleJob("${PROJECT_NAME}/Environment_Provisioning/Initiate_Server_Creation")
def scmProject = "git@gitlab:${WORKSPACE_NAME}/adop_opc_orchestration.git"
def scmCredentialsId = "adop-jenkins-master"

Closure passwordParam(String paramName, String paramDescription, String paramDefaultValue) {
    return { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'hudson.model.PasswordParameterDefinition' {
            'name'(paramName)
      		'description'(paramDescription)
        	'defaultValue'(paramDefaultValue)
        }
    }
}

createJob.with {
    description('')
    parameters {
        stringParam('OPC_USERNAME', '', 'Account\'s username in OPC (Oracle Public Cloud).')
        stringParam('DOMAIN', 'a424647', 'Oracle Public Cloud Account\'s Identity Domain. (eg. a424647)')
        stringParam('ENDPOINT_URL', 'https://api-z42.compute.us2.oraclecloud.com/', 'Oracle Public Cloud Endpoint URL. (eg. https://api-z42.compute.us2.oraclecloud.com/)')
        stringParam('SSH_KEY', 'devops_key', 'This key must be created already in OPC. (eg. oracle_key)')
        stringParam('DATABASE_INSTANCE_NAME', '', 'Name of the DB instance. (eg. demo_database_server)')
        stringParam('DB_VOLUME_NAME', '', 'Name of the DB storage. (eg. demo_database_storage)')
        stringParam('DATABASE_VOLUME_SIZE', '150', 'Storage size in GigaByte.')
        stringParam('APP_INSTANCE_NAME', '', 'Name of the APP instance. (eg. demo_app_server)')
        stringParam('APP_VOLUME_NAME', '', 'Name of the APP storage. (eg. demo_app_storage)')
        stringParam('APP_VOLUME_SIZE', '100', 'Storage size in GigaByte.') 
    }
    configure passwordParam("OPC_PASSWORD", "Account\'s password in OPC (Oracle Public Cloud)", "")

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('ansible')

    wrappers {
        preBuildCleanup() 
        colorizeOutput('css')
        sshAgent('ansible-user-key')
    }

    steps {
        shell('''#!/bin/bash

cat > credentials.tfvars <<-EOF
user = "${OPC_USERNAME}"
password = "${OPC_PASSWORD}"
domain = "${DOMAIN}"
endpoint = "${ENDPOINT_URL}"
sshkey = "/Compute-${DOMAIN}/${OPC_USERNAME}/${SSH_KEY}"
seclists1 = "/Compute-${DOMAIN}/${OPC_USERNAME}/defaultALL"
seclists2 = "/Compute-${DOMAIN}/${OPC_USERNAME}/defaultHTTP"
seclists3 = "/Compute-${DOMAIN}/${OPC_USERNAME}/defaultHTTPS"
EOF

cat > variables.tf <<-EOF
variable user {}
variable password {}
variable domain {}
variable endpoint {}
variable sshkey {}
variable seclists1 {}
variable seclists2 {}
variable seclists3 {}
EOF
        ''')

        shell('''#!/bin/bash

cat > main.tf <<-EOF
provider "opc" {
  user = "\\\${var.user}"
  password = "\\\${var.password}"
  identity_domain = "\\\${var.domain}"
  endpoint = "\\\${var.endpoint}"
}

resource "opc_compute_storage_volume" "dbvolume" {
        size = "${DATABASE_VOLUME_SIZE}"
        description = "PS Database Volume"
        name = "${DB_VOLUME_NAME}"
        bootable = true
        image_list = "/oracle/public/OL_6.8_UEKR4_x86_64"
        image_list_entry = 2
}

resource "opc_compute_storage_volume" "appvolume" {
        size = "${APP_VOLUME_SIZE}"
        description = "PS Application Volume"
        name = "${APP_VOLUME_NAME}"
        bootable = true
        image_list = "/oracle/public/OL_6.8_UEKR4_x86_64"
        image_list_entry = 2
}

resource "opc_compute_instance" "dbinstance" {
        name = "${DATABASE_INSTANCE_NAME}"
        label = "PS Database Instance"
        image_list = "/oracle/public/OL_6.8_UEKR4_x86_64"
        shape = "oc3"
        ssh_keys = [ "\\\${var.sshkey}" ]
        networking_info {
                index = 0
                shared_network = true
                nat = ["ippool:/oracle/public/ippool"]
                sec_lists = [ "\\\${var.seclists1}" ]
                sec_lists = [ "\\\${var.seclists2}" ]
                sec_lists = [ "\\\${var.seclists3}" ]
        }
        storage {
                index = 1
                volume = "\\\${opc_compute_storage_volume.dbvolume.name}"
        }
        boot_order = [ 1 ]
}

resource "opc_compute_instance" "appinstance" {
        name = "${APP_INSTANCE_NAME}"
        label = "PS Application Instance"
        image_list = "/oracle/public/OL_6.8_UEKR4_x86_64"
        shape = "oc3"
        ssh_keys = [ "\\\${var.sshkey}" ]
        networking_info {
                index = 0
                shared_network = true
                nat = ["ippool:/oracle/public/ippool"]
                sec_lists = [ "\\\${var.seclists1}" ]
                sec_lists = [ "\\\${var.seclists2}" ]
                sec_lists = [ "\\\${var.seclists3}" ]
        }
        storage {
                index = 1
                volume = "\\\${opc_compute_storage_volume.appvolume.name}"
        }
        boot_order = [ 1 ]
}
EOF
        ''')

        shell('''#!/bin/bash

echo "CUSTOM_WORKSPACE=${WORKSPACE}" > props
        ''')

        environmentVariables {
            propertiesFile('props')
        }
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Execute_Server_Creation') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                    propertiesFile('props', true)
                }
            }
        }
    }
}
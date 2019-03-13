def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"
def createJob = freeStyleJob(containerFolder + '/Execute_Server_Creation')

createJob.with {
    description('')
    parameters {
        stringParam('OPC_USERNAME', '', '')
        stringParam('OPC_PASSWORD', '', '')
        stringParam('DOMAIN', '', '')
        stringParam('ENDPOINT_URL', '')
        stringParam('SSH_KEY', 'devops_key', '')
        stringParam('DATABASE_INSTANCE_NAME', '', '')
        stringParam('DB_VOLUME_NAME', '', '')
        stringParam('DATABASE_VOLUME_SIZE', '', '')
        stringParam('APP_INSTANCE_NAME', '', '')
        stringParam('APP_VOLUME_NAME', '', '')
        stringParam('APP_VOLUME_SIZE', '', '')
        stringParam('CUSTOM_WORKSPACE', '', '')
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

        copyArtifacts('Initiate_Server_Creation') {
            includePatterns('**/*')
            fingerprintArtifacts(true)
            buildSelector {
                upstreamBuild(true)
                latestSuccessful(false)
            }
        }

        shell('''#!/bin/bash
terraform init
terraform plan -var-file="credentials.tfvars"
terraform apply -var-file="credentials.tfvars"

        ''')
    }
}
def containerFolder = "${PROJECT_NAME}/Environment_Provisioning"

buildPipelineView(containerFolder + '/Environment_Provisioning_Pipeline') {
    title('Provision Environments')
    displayedBuilds(10)
    selectedJob('Set_Provisioning_Parameters')
	showPipelineDefinitionHeader()
    showPipelineParameters()
	consoleOutputLinkStyle(OutputStyle.NewWindow)
    refreshFrequency(3)
}
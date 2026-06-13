package ru.vm5277.plugin.executing;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.flash.FlashToolParam;

import java.util.List;

public class ExecuteConfiguration extends RunConfigurationBase<ExecuteParameters> {

	protected ExecuteConfiguration(Project project, ConfigurationFactory factory, String name) {
		super(project, factory, name);
	}

	@Override
	protected ExecuteParameters getOptions() {
			return (ExecuteParameters) super.getOptions();
		}

	@Override
	public Class<? extends RunConfigurationOptions> getOptionsClass() {
	return ExecuteParameters.class;
}

	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
			return new ExecuteSettingsEditor(getProject());
		}

	@Override
	public RunProfileState getState(Executor executor, ExecutionEnvironment environment) throws ExecutionException {
		return new ExecuteProfileState(environment, this);
	}
	public void setMavenProfileId(String profileId) { getOptions().setMavenProfileId(profileId); }
	public String getMavenProfileId() { return getOptions().getMavenProfileId(); }
	public void setRunMode(RunMode runMode) {getOptions().setRunMode(runMode);}
	public RunMode getRunMode() { return getOptions().getRunMode();}
	public void setCpuFreq(int cpuFreqKHz) {getOptions().setCpuFreq(cpuFreqKHz);}
	public int getCpuFreq() { return getOptions().getCpuFreq();}
	public void setStdioPort(String stdioPort) {getOptions().setStdioPort(stdioPort);}
	public String getStdioPort() { return getOptions().getStdioPort();}
	public void setTargetMode(String targetMode) {getOptions().setTargetMode(targetMode);}
	public String getTargetMode() { return getOptions().getTargetMode();}
	public void setPlatformType(PlatformType platformType) {getOptions().setPlatformType(platformType);}
	public PlatformType getPlatformType() { return getOptions().getPlatformType();}
	public void setDeviceId(String deviceId) {getOptions().setDeviceId(deviceId);}
	public String getDeviceId() { return getOptions().getDeviceId();}
	public void setFlashToolName(String flashToolName) {getOptions().setFlashToolName(flashToolName);}
	public String getFlashToolName() { return getOptions().getFlashToolName();}
	public void setOptimizationType(String _optimizationType) {
		System.out.println("EC, set:" + _optimizationType);
		getOptions().setOptimizationType(_optimizationType);}
	public String getOptimizationType() {
		System.out.println("EC, get:" + getOptions().getOptimizationType());
		return getOptions().getOptimizationType();}
	public void setStrictLevel(String strictLevel) {getOptions().setStrictLevel(strictLevel);}
	public String getStrictLevel() { return getOptions().getStrictLevel();}
	public void setSoftReset(boolean softReset) {getOptions().setSoftReset(softReset);}
	public boolean isSoftReset() {	return getOptions().isSoftReset();}
	public void setBldrApiReuse(boolean bldrApiReuse) {getOptions().setBldrApiReuse(bldrApiReuse);}
	public boolean isBldrApiReuse() {	return getOptions().isBldrApiReuse();}
	public void setVerboseOutput(boolean verboseOutput) {getOptions().setVerboseOutput(verboseOutput);}
	public boolean isVerboseOutput() {	return getOptions().isVerboseOutput();}
	public void setSyncPom(boolean syncPom) {getOptions().setSyncPom(syncPom);}
	public boolean isSyncPom() { return getOptions().isSyncPom();}
	public void setFlashToolParams(String flashToolParams) {getOptions().setFlashToolParams(flashToolParams);}
	public String getFlashToolParams() { return getOptions().getFlashToolParams();}


}
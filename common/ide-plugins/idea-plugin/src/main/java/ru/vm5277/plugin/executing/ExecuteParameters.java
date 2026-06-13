package ru.vm5277.plugin.executing;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.flash.FlashToolParam;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.Property;

import java.util.ArrayList;
import java.util.List;

public class ExecuteParameters extends RunConfigurationOptions {
	private final StoredProperty<String> mavenProfileId = string("").provideDelegate(this, "mavenProfileId");
	private final StoredProperty<String> runMode = string("Build J8B").provideDelegate(this, "runMode");
	private final StoredProperty<Integer> cpuFreq = property(16000000).provideDelegate(this, "cpuFreq");
	private final StoredProperty<String> stdioPort = string("").provideDelegate(this, "stdioPort");
	private final StoredProperty<String> targetMode = string("onw_wire").provideDelegate(this, "targetMode");
	private final StoredProperty<String> platformType = string("avr").provideDelegate(this, "platformType");
	private final StoredProperty<String> deviceId = string("atmega328").provideDelegate(this, "deviceId");
	private final StoredProperty<String> flashToolName = string("avrdude").provideDelegate(this, "flashToolName");
	private final StoredProperty<String> optimizationType = string("size").provideDelegate(this, "optimization");
	private final StoredProperty<String> strictLevel = string("light").provideDelegate(this, "strict");
	private final StoredProperty<Boolean> softReset = property(true).provideDelegate(this, "softReset");
	private final StoredProperty<Boolean> bldrApiReuse = property(true).provideDelegate(this, "bldrApiReuse");
	private final StoredProperty<Boolean> verboseOutput = property(false).provideDelegate(this, "verboseOutput");
	private final StoredProperty<Boolean> pomExport = property(false).provideDelegate(this, "pomExport");
	private final StoredProperty<String> flashToolParams= string("").provideDelegate(this, "flashToolParams");

	public void setMavenProfileId(String profileId) { this.mavenProfileId.setValue(this, profileId); }
	public String getMavenProfileId() { return mavenProfileId.getValue(this); }
	public void setRunMode(RunMode runMode) { this.runMode.setValue(this, runMode.name());}
	public RunMode getRunMode() {
		return RunMode.valueOf(runMode.getValue(this));
	}
	public void setCpuFreq(int cpuFreq) { this.cpuFreq.setValue(this, cpuFreq);}
	public int getCpuFreq() {return cpuFreq.getValue(this);}
	public void setStdioPort(String _stdioPort) {this.stdioPort.setValue(this, _stdioPort);}
	public String getStdioPort() {return stdioPort.getValue(this);}
	public void setTargetMode(String targetMode) { this.targetMode.setValue(this, targetMode);}
	public String getTargetMode() {return targetMode.getValue(this);}
	public void setPlatformType(PlatformType platformType) { this.platformType.setValue(this, null==platformType ? null : platformType.name());}
	public PlatformType getPlatformType() {
		if(null==platformType.getValue(this)) return null;
		return PlatformType.valueOf(platformType.getValue(this));
	}
	public void setDeviceId(String deviceId) { this.deviceId.setValue(this, deviceId);}
	public String getDeviceId() {return deviceId.getValue(this);}
	public void setFlashToolName(String flashToolName) { this.flashToolName.setValue(this, flashToolName);}
	public String getFlashToolName() {return flashToolName.getValue(this);}
	public void setOptimizationType(String _optimizationType) {
		System.out.println("EP, set:" + _optimizationType);
		this.optimizationType.setValue(this, _optimizationType);
	}
	public String getOptimizationType() {
		System.out.println("EP, get:" + optimizationType.getValue(this));
		return optimizationType.getValue(this);
	}
	public void setStrictLevel(String _strictLevel) { this.strictLevel.setValue(this, _strictLevel);}
	public String getStrictLevel() {return strictLevel.getValue(this);}
	public void setSoftReset(Boolean softReset) {this.softReset.setValue(this, softReset);}
	public Boolean isSoftReset() {
		return softReset.getValue(this);
	}
	public void setBldrApiReuse(Boolean bldrApiReuse) {this.bldrApiReuse.setValue(this, bldrApiReuse);}
	public Boolean isBldrApiReuse() {
		return bldrApiReuse.getValue(this);
	}
	public void setVerboseOutput(Boolean verboseOutput) {this.verboseOutput.setValue(this, verboseOutput);}
	public Boolean isVerboseOutput() {
		return verboseOutput.getValue(this);
	}
	public void setSyncPom(Boolean pomExport) {this.pomExport.setValue(this, pomExport);}
	public Boolean isSyncPom() {return pomExport.getValue(this);}
	public void setFlashToolParams(String flashToolParams) { this.flashToolParams.setValue(this, flashToolParams);}
	public String getFlashToolParams() {return flashToolParams.getValue(this);}

}
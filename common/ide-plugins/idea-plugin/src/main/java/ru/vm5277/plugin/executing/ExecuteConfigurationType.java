package ru.vm5277.plugin.executing;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;

public class ExecuteConfigurationType implements ConfigurationType {
	private static final String ID = "VM5277_RUN_CONFIG";
	private static final String DISPLAY_NAME = "vm5277 Build & Flash";
	private static final String DESCRIPTION = "Build J8B, Build ASM, Flash and run interactive mode configuration for vm5277 toolkit";

	private final ConfigurationFactory myFactory;

	public ExecuteConfigurationType() {
		myFactory = new ExecuteConfigurationFactory(this);
	}

	@Override
	@NotNull
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public String getConfigurationTypeDescription() {
		return DESCRIPTION;
	}

	@Override
	public Icon getIcon() {
		return IconLoader.getIcon("config.png", ExecuteConfigurationType.class);
	}

	@Override
	@NotNull
	public String getId() {
		return ID;
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories() {
		return new ConfigurationFactory[]{myFactory};
	}

	public static ExecuteConfigurationType getInstance() {
		return ConfigurationType.CONFIGURATION_TYPE_EP.findExtension(ExecuteConfigurationType.class);
	}
}
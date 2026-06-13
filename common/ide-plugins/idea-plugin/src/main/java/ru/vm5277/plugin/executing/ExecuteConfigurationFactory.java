package ru.vm5277.plugin.executing;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ExecuteConfigurationFactory extends ConfigurationFactory {

	private static final String FACTORY_ID = "VM5277_RUN_CONFIG_FACTORY";

	protected ExecuteConfigurationFactory(@NotNull ConfigurationType type) {
		super(type);
	}

	@Override
	@NotNull
	public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
		return new ExecuteConfiguration(project, this, "vm5277 Configuration");
	}

	@Override
	@NotNull
	public String getId() {
		return FACTORY_ID;
	}

	@Override
	@NotNull
	public RunConfiguration createConfiguration(String name, @NotNull RunConfiguration template) {
		ExecuteConfiguration newConfiguration = new ExecuteConfiguration(template.getProject(), this, name);

				// Копируем настройки из шаблона в НОВЫЙ объект ExecuteParameters
		//ExecuteConfiguration templateConfig = (ExecuteConfiguration) template;
		//copySettings(templateConfig, newConfiguration);

		return newConfiguration;
	}
}
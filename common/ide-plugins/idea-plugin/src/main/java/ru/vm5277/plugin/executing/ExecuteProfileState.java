package ru.vm5277.plugin.executing;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.flash.FlashToolParam;
import ru.vm5277.common.flash.InternalFlashTool;
import ru.vm5277.common.mvn.MavenUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ExecuteProfileState extends CommandLineState {
	private static final Logger LOG = Logger.getInstance(ExecuteProfileState.class);
	private final ExecuteConfiguration configuration;

	protected ExecuteProfileState(ExecutionEnvironment environment, ExecuteConfiguration configuration) {
		super(environment);
		this.configuration = configuration;
	}

	@NotNull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException {
		GeneralCommandLine commandLine = createCommandLine();

		OSProcessHandler processHandler = new ColoredProcessHandler(commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE));
		ProcessTerminatedListener.attach(processHandler);

		return processHandler;
	}

	@NotNull
	@Override
	public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
		ProcessHandler processHandler = startProcess();

		// Добавляем вывод в консоль
		ConsoleView console = createConsole(executor);
		console.attachToProcess(processHandler);

		return new DefaultExecutionResult(console, processHandler);
	}

	private GeneralCommandLine createCommandLine() throws ExecutionException {
		Project project = getEnvironment().getProject();
		String projectPath = project.getBasePath();

		if(null==projectPath) {
			throw new ExecutionException("Project path is not available");
		}

		RunMode runMode = configuration.getRunMode();
		GeneralCommandLine commandLine = new GeneralCommandLine();

		// Настройка Maven команды в зависимости от режима
		Path mvnPath = MavenUtils.getPath(null, FSUtils.getToolkitPath(), false);
		if(null==mvnPath) {
			throw new ExecutionException("Maven tool is not available");
		}
		commandLine.setExePath(mvnPath.toString());
		commandLine.addParameters(runMode.getMvnCommand().split("\\s+"));

		commandLine.addParameters("-P" + configuration.getName().replaceAll("\\s+", "_"));

		commandLine.addParameters("-Dj8b.targetFreq=" + configuration.getCpuFreq()/1000d);
		if(configuration.getTargetMode().equals("specify")) {
			commandLine.addParameters("-Dj8b.target=" + configuration.getPlatformType().name().toLowerCase() + ":" + configuration.getDeviceId());
			if(configuration.getFlashToolName().equals(InternalFlashTool.NAME)) {
				commandLine.addParameters("-Dj8b.targetStdio=" + configuration.getStdioPort());
			}
			else {
				if (null != configuration.getStdioPort() && !configuration.getStdioPort().isEmpty()) {
					commandLine.addParameters("-Dj8b.targetStdio=" + configuration.getStdioPort());
				}
				commandLine.addParameters("-Dj8b.flashTool=" + configuration.getFlashToolName());
				if(null!=configuration.getFlashToolParams() && !configuration.getFlashToolParams().isEmpty()) {
					String parts[] = configuration.getFlashToolParams().split(",");
					for(String base64param : parts) {
						String param = new String(StrUtils.parseBase64Binary(base64param), StandardCharsets.UTF_8);
						if(!param.isEmpty()) {
							String origParams[] = param.split("=", 0x02);
							if(0x02==origParams.length) {
								if(!origParams[0x00].isEmpty() && !origParams[0x01].isEmpty()) {
									commandLine.addParameters("-Dj8b.flashParam." + origParams[0x00].toLowerCase() + "=\"" + origParams[0x01] + "\"");
								}
							}
						}
					}
				}
			}
		}
		if(configuration.getTargetMode().equals("auto") || InternalFlashTool.NAME.equals(configuration.getFlashToolName())) {
			commandLine.addParameters("-Dj8b.softReset=" + configuration.isSoftReset());
			commandLine.addParameters("-Dj8b.bldrApiReuse=" + configuration.isBldrApiReuse());
		}
		commandLine.addParameters("-Dj8b.optimization=" + configuration.getOptimizationType().toLowerCase());
		commandLine.addParameters("-Dj8b.strict=" + configuration.getStrictLevel().toLowerCase());
		commandLine.addParameters("-Dj8b.verbose=" + configuration.isVerboseOutput());
		commandLine.setWorkDirectory(projectPath);

		return commandLine;
	}
}
package ru.vm5277.plugin.executing;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenImportListener;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Toolkit;
import ru.vm5277.common.mvn.MavenUtils;
import ru.vm5277.plugin.asm.AsmSyntaxHighlighterFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MavenStartup implements ProjectActivity {
	public static VirtualFile projectsDir;
	public static Path toolkitPath;
	public static Path mavenPath;
//	public static String mavenVersion = "---";

	private static final Logger LOG = Logger.getInstance(MavenStartup.class);

	public MavenStartup() {
		refreshPaths();
	}

	public static void refreshPaths() {
		Path projectsDirPath = Paths.get(FSUtils.getHomeDir(), "j8b-projects").normalize();
		if (!projectsDirPath.toFile().exists()) {
			projectsDirPath.toFile().mkdirs();
		}
		projectsDir = LocalFileSystem.getInstance().findFileByPath(projectsDirPath.toString());

		if(Toolkit.checkToolkit(null, Toolkit.DONWLOAD_URL, false)) {
			toolkitPath = FSUtils.getToolkitPath();
			if(null!=toolkitPath && toolkitPath.toString().trim().isEmpty()) toolkitPath = null;
			mavenPath = MavenUtils.getPath(null, toolkitPath, false);
			if(null!=mavenPath && mavenPath.toString().trim().isEmpty()) mavenPath = null;
		}
		else {
			toolkitPath = null;
			mavenPath = null;
		}
	}

	@Override
	public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
		project.getMessageBus().connect().subscribe(
			ProjectManager.TOPIC,
			new ProjectManagerListener() {
				@Override
				public void projectClosed(@NotNull Project closedProject) {
					AsmSyntaxHighlighterFactory.removeLspServer(project);
				}
			}
		);
		MavenProjectsManager mavenManager = MavenProjectsManager.getInstance(project);
		if (mavenManager.isMavenizedProject()) {
			mavenManager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
		}

		project.getMessageBus().connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
			@Override
			public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings) {
				RunConfiguration config = settings.getConfiguration();
				if (config instanceof ExecuteConfiguration executeConfig) {
					// 1. Проверяем валидность (чтобы не сохранять мусор)
					try {
						executeConfig.checkConfiguration();
					} catch (RuntimeConfigurationException e) {
						return; // Данные невалидны, выходим
					}
					// 2. Выполняем обновление POM
					MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
					if (!manager.getProjects().isEmpty()) {
						// Рекомендуется обернуть в WriteCommandAction, если внутри идет запись в файл
						if(executeConfig.isSyncPom()) {
							MavenProfileSynchronizer.saveConfiguration(manager.getProjects().get(0), executeConfig);
						}
					}
				}
			}
		});

		project.getMessageBus().connect().subscribe(MavenImportListener.TOPIC, new MavenImportListener() {
				@Override
				public void importFinished(@NotNull Collection<MavenProject> importedProjects, @NotNull List<Module> newModules) {
					MavenProfileSynchronizer.loadProfiles(project);
				}
			}
		);

		return Unit.INSTANCE;
	}

	public static String getSdkPath(Project project) {
		Sdk sdk = (null==project ? null : ProjectRootManager.getInstance(project).getProjectSdk());
		if(null!=sdk) return sdk.getHomePath();
		Sdk[] allJdks = ProjectJdkTable.getInstance().getAllJdks();
		for (Sdk _sdk : allJdks) {
			if (null!=_sdk.getHomePath() && !_sdk.getHomePath().isEmpty()) {
				return _sdk.getHomePath();
			}
		}
		String ideaHomePath = PathManager.getHomePath();
		if(!ideaHomePath.isEmpty()) {
			String jbrPath = ideaHomePath + File.separator + "jbr";
			if(new File(jbrPath).exists()) {
				return jbrPath;
			}
		}

		NotificationGroup notificationGroup = new NotificationGroup(
			"J8B SDK Notifications",
			NotificationDisplayType.BALLOON,
			true
		);
		Notification notification = notificationGroup.createNotification(
			"JDK Not Configured",
			"Please configure JDK in:\nFile → Project Structure → Project → SDK",
			NotificationType.ERROR
		);
		Notifications.Bus.notify(notification, project);
		return null;
	}

	public static void setJavaHome(Project project, GeneralCommandLine commandLine) {
		Map<String, String> env = commandLine.getEnvironment();
		Map<String, String> systemEnv = System.getenv();
		for (Map.Entry<String, String> entry : systemEnv.entrySet()) {
			env.putIfAbsent(entry.getKey(), entry.getValue());
		}

		String sdkPath = MavenStartup.getSdkPath(project);
		if(null!=sdkPath && !sdkPath.isEmpty()) {
			// Добавляем JAVA_HOME
			env.put("JAVA_HOME", sdkPath);

			// Добавляем JAVA_HOME/bin в PATH (с сохранением существующего PATH)
			String javaBin = sdkPath + File.separator + "bin";
			String existingPath = env.get("PATH");
			if (existingPath != null && !existingPath.isEmpty()) {
				env.put("PATH", javaBin + File.pathSeparator + existingPath);
			} else {
				env.put("PATH", javaBin);
			}
		}
	}
}
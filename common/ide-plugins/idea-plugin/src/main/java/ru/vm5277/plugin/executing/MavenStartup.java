package ru.vm5277.plugin.executing;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

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

		if(Toolkit.checkToolkit(null, FSUtils.getToolkitPath(), Toolkit.DONWLOAD_URL, false)) {
			toolkitPath = FSUtils.getToolkitPath();
			mavenPath = MavenUtils.getPath(null, toolkitPath, false);
		}
		else {
			toolkitPath = null;
			mavenPath = null;
		}
	}

	@Override
	public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
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
}
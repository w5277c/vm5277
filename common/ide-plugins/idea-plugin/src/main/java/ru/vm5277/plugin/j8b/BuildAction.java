package ru.vm5277.plugin.j8b;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.jetbrains.idea.maven.indices.MavenIndicesManager;

import java.util.Arrays;
import java.util.Collections;

public class BuildAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getProject();
		if (null==project) return;

		try {
			// Параметры Maven
			MavenRunnerParameters params = new MavenRunnerParameters(
				true,                                       // isPomExecution
				project.getBasePath(),                                   // workingDirPath
				"pom.xml",                                               // pomFileName
				Arrays.asList("clean", "compile"),                       // goals
				Collections.singletonList("arduino-uno"),               // explicitEnabledProfiles
				Collections.emptyList()                                  // explicitDisabledProfiles
			);

			MavenRunner runner = MavenRunner.getInstance(project);
			MavenRunnerSettings settings = runner.getSettings();

			// Запуск с callback по завершению
			runner.run(params, settings, null);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void update(AnActionEvent e) {
		e.getPresentation().setEnabled(e.getProject() != null);
		e.getPresentation().setVisible(true);
	}
}
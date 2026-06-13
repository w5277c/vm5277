package ru.vm5277.plugin.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.module.Module;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Toolkit;
import ru.vm5277.common.mvn.MavenUtils;
import ru.vm5277.plugin.executing.ExecuteSettingsEditor;
import ru.vm5277.plugin.executing.MavenStartup;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewProjectModuleBuilder extends ModuleBuilder {
	public static final int LABEL_WIDTH = 150;
	public static final int FIELD_WIDTH = 340;
	private static final String TOOLKIT_DOWNLOAD_TEXT = "Download toolkit";
	private static final String MAVEN_DOWNLOAD_TEXT = "Download Maven";
	public final static String RETRY_DOWNLOAD_BUTTON_TEXT = "Retry download";
	private final static Logger LOG = Logger.getInstance(NewProjectModuleBuilder.class);
	private final JTextField toolkitPathField = new JTextField();
	private final JButton toolkitDownloadButton = new JButton(TOOLKIT_DOWNLOAD_TEXT);
	private final JTextField mavenPathField = new JTextField();
	private final JButton mavenDownloadButton = new JButton(MAVEN_DOWNLOAD_TEXT);
	private final TextFieldWithBrowseButton projectsPathField = new TextFieldWithBrowseButton();
	private final JTextField groupIdField = new JTextField("com.example", 20);
	private final JTextField artifactIdField = new JTextField("my-project", 20);
	private final JTextField versionField = new JTextField("1.0-SNAPSHOT", 20);
	private VirtualFile projectsDir;
	private volatile boolean disposed = false;

	public NewProjectModuleBuilder() {
		toolkitDownloadButton.setVisible(false);
		mavenDownloadButton.setVisible(false);

		toolkitPathField.setEditable(false);
		mavenPathField.setEditable(false);
		projectsPathField.setEditable(false);

		refresh();
	}

	private void refresh() {
		new Thread() {
			public void run() {
				MavenStartup.refreshPaths();

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						boolean toolkitExist = null!=MavenStartup.toolkitPath;
						toolkitDownloadButton.setVisible(!toolkitExist);
						toolkitPathField.setText(toolkitExist ? MavenStartup.toolkitPath.toString() : "-----");

						boolean mavenExist = null!=MavenStartup.mavenPath;
						mavenDownloadButton.setVisible(toolkitExist && !mavenExist);
						mavenPathField.setText(mavenExist ? MavenStartup.mavenPath.toString() : "-----");

						projectsPathField.setEnabled(toolkitExist && mavenExist);
						groupIdField.setEnabled(toolkitExist && mavenExist);
						artifactIdField.setEnabled(toolkitExist && mavenExist);
						versionField.setEnabled(toolkitExist && mavenExist);

						if(null==projectsDir) {
							projectsDir = MavenStartup.projectsDir;
							projectsPathField.setText(null == projectsDir ? "-----" : projectsDir.getCanonicalPath());
						}
					}
				});
			}
		}.start();
	}

	@Override
	public ModuleType<?> getModuleType() {
		return ModuleType.EMPTY;
	}

	@Override
	public String getPresentableName() {
		return "VM5277";
	}

	@Override
	public Icon getNodeIcon() {
		return IconLoader.getIcon("/config.png", NewProjectModuleBuilder.class);
	}

	@Override
	public String getDescription() {
		return "VM5277 Project with Maven archetype";
	}

	@Override
	public ModuleWizardStep modifyProjectTypeStep(@NotNull SettingsStep settingsStep) {
		projectsPathField.setPreferredSize(new Dimension(FIELD_WIDTH, projectsPathField.getPreferredSize().height));
		projectsPathField.setText(null==projectsDir ? "-----" : projectsDir.getCanonicalPath());
		projectsPathField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false)
					.withTitle("Select J8B projects directory");
				VirtualFile newDir = FileChooser.chooseFile(descriptor, settingsStep.getContext().getProject(), projectsDir);
				if (null != newDir) {
					projectsDir = newDir;
					projectsPathField.setText(newDir.getPath());
				}
			}
		});

		JPanel toolkitPanel = new JPanel(new BorderLayout());
		toolkitPathField.setPreferredSize(new Dimension(FIELD_WIDTH - 120, toolkitPathField.getPreferredSize().height));
		toolkitPanel.add(toolkitPathField, BorderLayout.CENTER);
		toolkitDownloadButton.setPreferredSize(new Dimension(120, toolkitDownloadButton.getPreferredSize().height));
		toolkitPanel.add(toolkitDownloadButton, BorderLayout.EAST);

		JPanel mavenPanel = new JPanel(new BorderLayout());
		mavenPathField.setPreferredSize(new Dimension(FIELD_WIDTH - 120, mavenPathField.getPreferredSize().height));
		mavenPanel.add(mavenPathField, BorderLayout.CENTER);
		mavenDownloadButton.setPreferredSize(new Dimension(120, mavenDownloadButton.getPreferredSize().height));
		mavenPanel.add(mavenDownloadButton, BorderLayout.EAST);

		toolkitDownloadButton.addActionListener(e -> {
			toolkitDownloadButton.setEnabled(false);
			toolkitDownloadButton.setText("Downloading...");
			new Thread(new Runnable() {
				@Override
				public void run() {
					ru.vm5277.common.Logger log = new ru.vm5277.common.Logger() {
						@Override public void info(String s) {
							LOG.info(s);
						}
						@Override public void warn(String s) {
							LOG.warn(s);
						}
						@Override public void error(String s) {
							LOG.error(s);
						}
						@Override public void debug(String s) {
							LOG.debug(s);
						}
						@Override public boolean progress(int percents, String status) {
							SwingUtilities.invokeLater(() -> {
								if (!disposed) {
									toolkitDownloadButton.setText(status + "... " + percents + "%");
								}
							});
							return !disposed;
						}
					};
					ru.vm5277.common.Toolkit.checkToolkit(log, MavenStartup.toolkitPath, Toolkit.DONWLOAD_URL, true);
					refresh();
				}
			}).start();
		});

		mavenDownloadButton.addActionListener(e -> {
			mavenDownloadButton.setEnabled(false);
			mavenDownloadButton.setText("Downloading...");
			new Thread(new Runnable() {
				@Override
				public void run() {
					ru.vm5277.common.Logger log = new ru.vm5277.common.Logger() {
						@Override public void info(String s) {
							LOG.info(s);
						}
						@Override public void warn(String s) {
							LOG.warn(s);
						}
						@Override public void error(String s) {
							LOG.error(s);
						}
						@Override public void debug(String s) {
							LOG.debug(s);
						}
						@Override public boolean progress(int percents, String status) {
							SwingUtilities.invokeLater(() -> {
								if (!disposed) {
									mavenDownloadButton.setText(status + "... " + percents + "%");
								}
							});
							return !disposed;
						}
					};
					MavenUtils.getPath(log, MavenStartup.toolkitPath, true);
					refresh();
				}
			}).start();
		});

		settingsStep.addSettingsField("Toolkit path:", toolkitPanel);
		settingsStep.addSettingsField("Maven path:", mavenPanel);

		settingsStep.addSettingsField("Projects directory:", projectsPathField);
		settingsStep.addSettingsField("Group ID:", groupIdField);
		settingsStep.addSettingsField("Artifact ID:", artifactIdField);
		settingsStep.addSettingsField("Version:", versionField);

		return new ModuleWizardStep() {
			@Override
			public JComponent getComponent() {
				return null;
			}

			@Override
			public void updateDataModel() {}

			@Override
			public boolean validate() throws ConfigurationException {
				if(null==MavenStartup.toolkitPath) {
					throw new ConfigurationException("Please download VM5277 toolkit first");
				}
				if(null==MavenStartup.mavenPath) {
					throw new ConfigurationException("Please download Maven utility first");
				}
				if(null==projectsDir) {
					throw new ConfigurationException("Please specify projects directory");
				}
				return true;
			}
		};
	}

	public @Nullable ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
		settingsStep.getModuleNameLocationSettings().setModuleContentRoot(projectsDir.getPath() + File.separator + artifactIdField.getText());
		settingsStep.getModuleNameLocationSettings().setModuleName(artifactIdField.getText());
		return this.modifyStep(settingsStep);
	}

	@Override
	public @Nullable Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
		Module module = super.commitModule(project, model);

		Path mvnPath = MavenUtils.getPath(null, FSUtils.getToolkitPath(), false);
		if(null==mvnPath) {
			LOG.error("Maven tool is not available");
			return module;
		}

		if (module != null && getContentEntryPath() != null) {
			String moduleDir = getContentEntryPath();
			ApplicationManager.getApplication().executeOnPooledThread(() -> {
				try {
					GeneralCommandLine cmd = new GeneralCommandLine();
					cmd.setExePath(mvnPath.toString());
					cmd.setWorkDirectory(new File(moduleDir));
					cmd.addParameters(
						"archetype:generate",
						"-DgroupId=" + getGroupId(),
						"-DartifactId=" + getArtifactId(),
						"-DarchetypeGroupId=ru.vm5277",
						"-DarchetypeArtifactId=j8b-archetype",
						"-DinteractiveMode=false",
						"-Dversion=" + getVersion()
					);
					ExecUtil.execAndGetOutput(cmd);

					File mavenDir = Paths.get(moduleDir).resolve(getArtifactId()).normalize().toFile();
					FSUtils.moveUp(mavenDir);
					mavenDir.delete();

					ApplicationManager.getApplication().invokeLater(new Runnable() {
						@Override
						public void run() {
							VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}

		return module;
	}

	public String getGroupId() { return groupIdField.getText(); }
	public String getArtifactId() { return artifactIdField.getText(); }
	public String getVersion() { return versionField.getText(); }
}
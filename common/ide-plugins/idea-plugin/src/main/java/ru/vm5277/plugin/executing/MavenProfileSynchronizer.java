package ru.vm5277.plugin.executing;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.enums.StrictLevel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenProfileSynchronizer {
	private static final Logger LOG = Logger.getInstance(MavenProfileSynchronizer.class);

	public static void loadProfiles(Project project) {
		try {
			MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
			if (manager.getProjects().isEmpty()) return;

			MavenProject mavenProject = manager.getProjects().getFirst();
			if(!"j8b".equals(mavenProject.getPackaging())) return;

			VirtualFile virtualFile = mavenProject.getFile();
			File pomFile = new File(virtualFile.getPath());
			for(String profileId : mavenProject.getProfilesIds()) {
				MavenXpp3Reader reader = new MavenXpp3Reader();
				// Собираем все модели по цепочке parent'ов
				Model currentModel = reader.read(new FileReader(pomFile));
				List<Model> hierarchy = new ArrayList<>();
				hierarchy.add(currentModel);

				File currentFile = pomFile;
				while(null!=currentModel.getParent()) {
					String parentPath = null;
					if(null!=currentModel.getParent()) {
						String relativePath = currentModel.getParent().getRelativePath();
						if(null==relativePath) relativePath = "../pom.xml";

						File parentFile = new File(currentFile.getParentFile(), relativePath);
						try {parentPath = parentFile.getCanonicalPath();} catch (Exception ignored) {}
					}

					if(null==parentPath) break;
					currentFile = new File(parentPath);
					if(!currentFile.exists()) break;
					currentModel = reader.read(new FileReader(currentFile));
					hierarchy.add(0, currentModel);
				}

				Properties profileProps = new Properties();
				for(Model model : hierarchy) {
					if(null==model.getProfiles()) continue;

					for(Profile profile : model.getProfiles()) {
						if(profile.getId().equals(profileId)) {
							profileProps.putAll(profile.getProperties());
							break;
						}
					}
				}
				if(!profileProps.isEmpty()) {
					createOrUpdateConfiguration(project, profileId, profileProps);
				}
			}
		}
		catch (Exception e) {
			LOG.warn("Failed to sync Maven profiles", e);
		}
	}

	private static void createOrUpdateConfiguration(Project project, String profileId, Properties props) {
		int cpuFreqKHz = -1;
		String stdioPort = null;

		if(props.containsKey("j8b.targetFreq")) {
			String cpuFreq = props.getProperty("j8b.targetFreq");
			if (null!=cpuFreq && !cpuFreq.trim().isEmpty() && FloatDocumentFilter.isValid(cpuFreq)) {
				try {
					cpuFreqKHz = (int) (Double.parseDouble(cpuFreq) * 1000d);
				} catch (NumberFormatException ignored) {}
			}
		}
		if(props.containsKey("j8b.targetStdio")) {
			String tmp = props.getProperty("j8b.targetStdio");
			if(null!=tmp && !tmp.trim().isEmpty() && PortDocumentFilter.isValid(tmp)) {
				stdioPort = tmp;
			}
		}

		PlatformType platformType = null;
		String deviceId = null;
		if(props.containsKey("j8b.target")) {
			String tmp = props.getProperty("j8b.target");
			if(null!=tmp && !tmp.trim().isEmpty()) {
				if(tmp.contains(":")) {
					int index = tmp.indexOf(":");
					platformType = PlatformType.fromName(tmp.substring(0x00, index).trim());
					deviceId = tmp.substring(index + 0x01).trim();
				}
				if(null==platformType || deviceId.isEmpty()) {
					LOG.warn("Invalid target: " + tmp + ", profile:" + profileId);
					platformType = null;
					deviceId = null;
				}
			}
		}

		if(-1==cpuFreqKHz) LOG.warn("Empty or invalid CPU frequency, profile:" + profileId);
		if(null==stdioPort) LOG.warn("Empty or invalid STDIO port(s), profile:" + profileId);
		if(-1==cpuFreqKHz || null==stdioPort) return;

		RunManager runManager = RunManager.getInstance(project);
		ExecuteConfigurationType configType = ExecuteConfigurationType.getInstance();
		ConfigurationFactory factory = configType.getConfigurationFactories()[0];

		RunnerAndConfigurationSettings settings = runManager.findConfigurationByTypeAndName(configType, profileId);
		ExecuteConfiguration config;
		if(null==settings) {
			// Создаем новые settings
			settings = runManager.createConfiguration(profileId, factory);
			config = (ExecuteConfiguration) settings.getConfiguration();
			runManager.addConfiguration(settings);
		}
		else {
			// Получаем конфигурацию из существующих settings
			config = (ExecuteConfiguration) settings.getConfiguration();
			if(!config.isSyncPom()) return;
		}

		config.setMavenProfileId(profileId);
		config.setRunMode(RunMode.RUN_J8B);
		config.setCpuFreq(cpuFreqKHz);
		config.setStdioPort(stdioPort);

		if(null==platformType) {
			config.setTargetMode("auto");
		}
		else {
			config.setTargetMode("specify");
			config.setPlatformType(platformType);
			config.setDeviceId(deviceId);
			//TODO необходимо расширить параметры maven profile - для указания flash tool и его параметров
			config.setFlashToolName("internal");
		}

		if(props.containsKey("j8b.optimization")) {
			String tmp = props.getProperty("j8b.optimization");
			if(null!=tmp && !tmp.trim().isEmpty() && null!= OptimizationType.fromName(tmp.toUpperCase())) {
				config.setOptimizationType(tmp);
			}
		}

		if(props.containsKey("j8b.strict")) {
			String tmp = props.getProperty("j8b.strict");
			if(null!=tmp && !tmp.trim().isEmpty() && null!= StrictLevel.fromName(tmp.toUpperCase())) {
				config.setStrictLevel(tmp);
			}
		}

		if(props.containsKey("j8b.softReset")) {
			config.setSoftReset(props.getProperty("j8b.softReset", "true").equalsIgnoreCase("true"));
		}

		if(props.containsKey("j8b.bldrApiReuse")) {
			config.setBldrApiReuse(props.getProperty("j8b.bldrApiReuse", "true").equalsIgnoreCase("true"));
		}

		if(props.containsKey("verboseOutput")) {
			config.setVerboseOutput(props.getProperty("verboseOutput", "false").equalsIgnoreCase("true"));
		}
	}

	public static void saveConfiguration(MavenProject mavenProject, ExecuteConfiguration config) {
		if (!"j8b".equals(mavenProject.getPackaging())) return;

		String profileId = config.getMavenProfileId();
		if(null==profileId || profileId.isEmpty()) {
			LOG.warn("Cannot export configuration without Maven profile ID");
			return;
		}

		File pomFile = null;
		try {
			pomFile = new File(mavenProject.getFile().getPath());
			MavenXpp3Reader reader = new MavenXpp3Reader();
			Model model = reader.read(new FileReader(pomFile));

			//TODO учитывать вложенность pom, профиль писать в самый верхний (дочерний)
			Profile profile = null;
			if (model.getProfiles() != null) {
				for (Profile _profile : model.getProfiles()) {
					if (_profile.getId().equals(profileId)) {
						profile =  _profile;
						break;
					}
				}
			}
			if(null==profile) {
				profile = new Profile();
				profile.setId(profileId);
				model.addProfile(profile);
			}

			// Обновляем свойства профиля
			Properties props = profile.getProperties();
			if(null==props) {
				props = new Properties();
				profile.setProperties(props);
			}

			props.setProperty("j8b.targetFreq", String.valueOf(config.getCpuFreq() / 1000d));
			props.setProperty("j8b.targetStdio", null==config.getStdioPort() ? "" : config.getStdioPort().trim());
			String target = "";
			if(null!=config.getPlatformType() && null!=config.getDeviceId()) {
				target = config.getPlatformType().name().toLowerCase() + ":" + config.getDeviceId().toLowerCase();
			}
			props.setProperty("j8b.target", target);

			props.setProperty("j8b.optimization", config.getOptimizationType());
			props.setProperty("j8b.strict", config.getStrictLevel());
			props.setProperty("j8b.softReset", String.valueOf(config.isSoftReset()));
			props.setProperty("j8b.bldrApiReuse", String.valueOf(config.isBldrApiReuse()));
			props.setProperty("j8b.verbose", String.valueOf(config.isVerboseOutput()));

			// Создаем backup оригинального файла
			File backupFile = new File(pomFile.getAbsolutePath() + ".backup");
			Files.copy(pomFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			// Записываем обновленный POM
			try(FileWriter writer = new FileWriter(pomFile)) {
				MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
				mavenWriter.write(writer, model);
			}

			// Читаем только что записанный файл и заменяем пробелы (MavenXpp3Writer не позволяет задать indent)
			String content = new String(Files.readAllBytes(pomFile.toPath()));
			Pattern p = Pattern.compile("(?m)^(?:  )+");
			Matcher m = p.matcher(content);
			StringBuffer sb = new StringBuffer();
			while(m.find()) {
				int tabs = m.group().length()/2;
				m.appendReplacement(sb, "\t".repeat(tabs));
			}
			m.appendTail(sb);
			Files.write(pomFile.toPath(), sb.toString().getBytes());

			if(!backupFile.delete()) {
				backupFile.deleteOnExit();
			}
		}
		catch (Exception ex1) {
			LOG.error("Failed to save configuration to Maven profile", ex1);
			if(null!=pomFile) {
				try {
					File backupFile = new File(pomFile.getAbsolutePath() + ".backup");
					if(backupFile.exists()) {
						Files.copy(backupFile.toPath(), pomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						if(!backupFile.delete()) {
							backupFile.deleteOnExit();
						}
					}
				}
				catch (Exception ex2) {
					LOG.error("Failed to restore POM from backup", ex2);
				}
			}
		}
	}
}
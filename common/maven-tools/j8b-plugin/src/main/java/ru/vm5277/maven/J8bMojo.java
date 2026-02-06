/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.project.MavenProject;

public abstract class J8bMojo extends AbstractMojo {
	@Parameter(property = "j8b.skip", defaultValue = "false")
	protected boolean skip;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(property = "j8b.target", defaultValue = "avr:atmega328p")
	protected String target;

	@Parameter(property = "j8b.targetFreq", defaultValue = "8.0")
	protected String targetFreq;
	
	@Parameter(property = "j8b.targetStdio", defaultValue = "")
	protected String targetStdio;
	
	@Parameter(property = "j8b.verboseOutput", defaultValue = "false")
	protected boolean verboseOutput;

	@Parameter(defaultValue = "Main.j8b")
	protected String mainFile;

	@Parameter(property = "j8b.toolkitPath", defaultValue = "${user.home}/vm5277")
	protected String toolkitPath;

	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	protected File projectBaseDir;

	@Parameter(defaultValue = "${project.basedir}/src")
	protected File sourceDirectory;

	@Parameter(defaultValue = "${project.basedir}/target/")
	protected File outputDirectory;

	@Parameter(defaultValue = "size")
	protected String optimization;

	@Parameter(property = "j8b.autoDownload", defaultValue = "true")
	protected boolean autoDownload;
	
	@Parameter(property = "j8b.downloadUrl", defaultValue = "https://vm5277.ru/releases/vm5277-release-latest.zip")
	protected String downloadUrl;
	
	public void doCompile(boolean devMode) throws MojoExecutionException {
		if(skip) {
			getLog().info("J8B compilation is skipped");
			return;
		}
		
		if("pom".equals(project.getPackaging())) {
			getLog().info("Skipping j8b compilation for parent POM project");
			return;
		}
		checkToolkit(devMode);
		if(!devMode) getLog().info("=== J8B Maven plugin v" + readVersion() + " - COMPILE ===");
		
		if(null==target || target.isEmpty() || !target.contains(":")) {
			throw new MojoExecutionException("[ERROR] Target parameter is absent or incorrect, please specify j8b.target parameter. Example: avr:atmega328p");
		}

		int index = target.indexOf(":");
		String platformStr = target.substring(0x00, index);
		String mcuStr = target.substring(index+0x01);

		File mainJ8bFile = new File(sourceDirectory, mainFile);
		if(!mainJ8bFile.exists()) {
			throw new MojoExecutionException(	"Main source file not found: " + mainJ8bFile.getAbsolutePath() + 
												"\nCreate " + mainFile + " in directory: " + sourceDirectory);
		}

		getLog().info("Main source file: " + mainJ8bFile.getAbsolutePath());

		// 4. Создаем выходную директорию
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		String mainName = mainFile.substring(0, mainFile.lastIndexOf("."));

		Path libsPath = Paths.get(toolkitPath, "bin", "libs");

		//getLog().info("Compilation...");
		int exitCode = 1;
		try {
			List<String> args = new ArrayList<>();
			args.add(target);
			args.add(mainJ8bFile.getAbsolutePath());
			args.add("-P");
			args.add(toolkitPath);
			args.add("-F");
			args.add(targetFreq);
			args.add("-o");
			args.add(outputDirectory.getAbsolutePath());
			if(devMode) {
				args.add("-d");
				args.add("-q");
			}
			if(verboseOutput || devMode) {
				args.add("--dump-ir");
			}
			if(!targetStdio.isEmpty()) {
				args.add("-D");
				args.add("STDIO_PORT=" + targetStdio);
			}
			
			exitCode = runJ8BTool(libsPath, "ru.vm5277.compiler.Main", args);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		if(0!=exitCode) {
			throw new MojoExecutionException("Compilation failed with exit code: " + exitCode);
		}

		//getLog().info("Assembling...");
		File mainAsmFile = new File(outputDirectory, mainName + ".asm");
		if(!mainAsmFile.exists()) {
			throw new MojoExecutionException("Main asm file not found: " + mainAsmFile.getAbsolutePath() + "\n");
		}

		Path rtosPath = Paths.get(toolkitPath, "rtos", platformStr);
		if(!rtosPath.toFile().exists()) {
			throw new MojoExecutionException("RTOS directory not found: " + rtosPath.toString() + "\n");
		}

		Path defsPath = Paths.get(toolkitPath, "defs", platformStr);
		if(!defsPath.toFile().exists()) {
			throw new MojoExecutionException("defs directory not found: " + defsPath.toString() + "\n");
		}

		try {
			List<String> args = new ArrayList<>();
			args.add(mainAsmFile.getAbsolutePath());
			args.add("-P");
			args.add(toolkitPath);
			args.add("-I");
			args.add(defsPath.toAbsolutePath().toString());
			args.add("-I");
			args.add(rtosPath.toAbsolutePath().toString());

			if(verboseOutput || devMode) {
				String mapFileName = mainName + ".map";
				String listFileName = mainName + ".lst";

				args.add("-m");
				args.add(mapFileName);
				args.add("-l");
				args.add(listFileName);
				args.add("-q");
			}
			
			exitCode = runJ8BTool(libsPath, "ru.vm5277." + platformStr + "_asm.Assembler", args);
		}
		catch(Exception ex) {
		}
		if(0!=exitCode) {
			throw new MojoExecutionException("Assembling failed with exit code: " + exitCode);
		}

		if(!devMode) getLog().info("=== Compilation finished successfully ===");
	}

	public String readVersion() {
		Class<?> clazz = this.getClass();
		String packagePath = clazz.getPackage().getName().replace('.', '/');
		String resourcePath = packagePath + "/version.properties";
		try (InputStream input = clazz.getClassLoader().getResourceAsStream(resourcePath)) {
			if(input != null) {
				Properties prop = new Properties();
				prop.load(input);
				return prop.getProperty("version");
			}
		}
		catch (IOException e) {
		}
		return " UNKNOWN";
	}

	protected void checkToolkit(boolean quiet) throws MojoExecutionException{
		File toolkitDir = new File(toolkitPath);
		if(!toolkitDir.exists()) {
			String env = System.getenv("vm5277");
			if(null!=env) {
				toolkitPath = env;
				toolkitDir = new File(toolkitPath);
			}
			if(!toolkitDir.exists()) {
				if(autoDownload) {
					getLog().info("Toolkit not found, downloading...");
					downloadAndExtractToolkit(toolkitDir);
				}
				else {
					throw new MojoExecutionException(	"Toolkit not found at: " + toolkitDir.getAbsolutePath() + "\n" +
														"Possible solutions:\n"	+
														"1. Set autoDownload=true in plugin configuration\n" +
														"2. Manually download from: " + downloadUrl + "\n" +
														"3. Set toolkit path via j8b.toolkitPath parameter\n" +
														"4. Set vm5277 environment variable");
				}
			}
		}

		if(!quiet) getLog().info("Using toolkit at: " + toolkitDir.getAbsolutePath());
	}

	protected void downloadAndExtractToolkit(File targetDir) throws MojoExecutionException {
		getLog().info("Downloading toolkit from: " + downloadUrl);

		try {
			// Создаем временный файл для скачивания
			File tempFile = File.createTempFile("vm5277", ".zip");
			tempFile.deleteOnExit();

			// Скачиваем файл
			downloadFile(downloadUrl, tempFile);

			// Очищаем целевую директорию, если она существует
			if(targetDir.exists()) {
				getLog().info("Cleaning existing toolkit directory: " + targetDir.getAbsolutePath());
				deleteDirectory(targetDir);
			}

			// Создаем целевую директорию
			targetDir.mkdirs();

			// Распаковываем архив
			getLog().info("Extracting toolkit to: " + targetDir.getAbsolutePath());
			extractZip(tempFile, targetDir);

			// Проверяем успешность установки
			if(!targetDir.exists()) {
				throw new MojoExecutionException("Failed to install toolkit. Installation corrupted.");
			}

			getLog().info("Toolkit successfully installed to: " + targetDir.getAbsolutePath());

			// Удаляем временный файл
			tempFile.delete();

		}
		catch (IOException e) {
			throw new MojoExecutionException("Failed to download toolkit: " + e.getMessage(), e);
		}
	}

	protected void downloadFile(String urlString, File outputFile) throws MojoExecutionException, IOException {
		try {
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "vm5277 j8b-maven-plugin v"+ readVersion());

			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new MojoExecutionException("Server returned HTTP " + responseCode + ": " + urlString);
			}

			try (InputStream in = connection.getInputStream();
				 FileOutputStream out = new FileOutputStream(outputFile)) {

				byte[] buffer = new byte[8192];
				int bytesRead;
				long totalBytes = 0;

				while(-1!=(bytesRead = in.read(buffer))) {
					out.write(buffer, 0, bytesRead);
					totalBytes += bytesRead;

					if(totalBytes % (1024*1024) == 0) { // Каждый MB
						getLog().debug("Downloaded " + (totalBytes / (1024 * 1024)) + " MB");
					}
				}

				getLog().info("Downloaded " + (totalBytes / 1024) + " KB");
			}
		}
		catch (MalformedURLException e) {
			throw new MojoExecutionException("Invalid download URL: " + urlString, e);
		}
	}

	protected void extractZip(File zipFile, File targetDir) throws IOException {
		String os = System.getProperty("os.name").toLowerCase();

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			byte[] buffer = new byte[8192];

			while(null!=(entry = zis.getNextEntry())) {
				File newFile = new File(targetDir, entry.getName());

				// Защита от Zip Slip
				if(!newFile.toPath().normalize().startsWith(targetDir.toPath().normalize())) {
					throw new IOException("Malicious zip entry: " + entry.getName());
				}

				if (entry.isDirectory()) {
					newFile.mkdirs();
					continue;
				}

				// Создаем родительские директории если нужно
				newFile.getParentFile().mkdirs();

				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					int len;
					while (0<(len = zis.read(buffer))) {
						fos.write(buffer, 0, len);
					}
				}

				if(!os.contains("win") && entry.getName().startsWith("bin/") && entry.getName().toLowerCase().endsWith(".sh")) {
					newFile.setExecutable(true, false);
				}

				zis.closeEntry();
			}
		}
	}

	protected void deleteDirectory(File dir) throws IOException {
		if(dir.exists()) {
			File[] files = dir.listFiles();
			if(files!=null) {
				for(File file : files) {
					if(file.isDirectory()) {
						deleteDirectory(file);
					}
					else {
						if(!file.delete()) {
							getLog().warn("Failed to delete file: " + file.getAbsolutePath());
						}
					}
				}
			}
			if(!dir.delete()) {
				getLog().warn("Failed to delete directory: " + dir.getAbsolutePath());
			}
		}
	}
	
	protected int runJ8BTool(Path libsPath, String mainClassName, List<String> args) throws Exception {
		getLog().debug("Running " + mainClassName + " with args: " + args.toString());

		File libsDir = libsPath.toFile();
		if(!libsDir.exists() || !libsDir.isDirectory()) {
			throw new MojoExecutionException("Libs directory not found: " + libsDir);
		}

		File[] jarFiles = libsDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".jar");
			}
		});

		if(null==jarFiles || 0==jarFiles.length) {
			throw new MojoExecutionException("No JAR files found in: " + libsDir);
		}

		List<URL> urls = new ArrayList<>();
		for (File jarFile : jarFiles) {
			try {
				urls.add(jarFile.toURI().toURL());
				getLog().debug("Added to classpath: " + jarFile.getName());
			}
			catch (Exception e) {
				getLog().warn("Failed to add JAR: " + jarFile.getName(), e);
			}
		}

		URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
		Class<?> mainClass = classLoader.loadClass(mainClassName);
		Method execMethod;
		try {
			execMethod = mainClass.getMethod("exec", String[].class);
		}
		catch (NoSuchMethodException ex) {
			throw new MojoExecutionException("Method 'exec(String[])' not found in class " + mainClassName, ex);
		}

		if(int.class!=execMethod.getReturnType()) {
			throw new MojoExecutionException("Method exec() must return int, but returns " + execMethod.getReturnType().getSimpleName());
		}

		if(!Modifier.isStatic(execMethod.getModifiers())) {
			throw new MojoExecutionException("Method exec() must be static");
		}

		// Вызываем метод
		Object result = execMethod.invoke(null, (Object) args.toArray(new String[0]));
		return (int) result;
	}

	protected void validateTarget() throws MojoExecutionException {
		if (target == null || !target.contains(":")) {
			throw new MojoExecutionException(
				"Invalid target format. Expected: platform:mcu, got: " + target);
		}
	}
}

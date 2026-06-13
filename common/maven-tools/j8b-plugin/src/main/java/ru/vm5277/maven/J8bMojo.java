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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import ru.vm5277.common.Device;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Logger;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.flash.FlashTool;
import ru.vm5277.common.flash.FlashToolProvider;
import ru.vm5277.common.flash.InternalFlashTool;

public abstract class J8bMojo extends AbstractMojo {
	@Parameter(property = "j8b.skip", defaultValue = "false")
	protected boolean skip;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
	
	@Parameter(property = "j8b.target", defaultValue = "")
	protected String target;

	@Parameter(property = "j8b.targetFreq", defaultValue = "8.0")
	protected String targetFreq;
	
	@Parameter(property = "j8b.targetStdio", defaultValue = "")
	protected String targetStdio;
	
	@Parameter(property = "j8b.verbose", defaultValue = "false")
	protected boolean verbose;

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

	@Parameter(defaultValue = "${project.basedir}/prebuild/")
	protected File prebuildDirectory;

	@Parameter(property = "j8b.optimization", defaultValue = "size")
	protected String optimization;

	@Parameter(property = "j8b.strict", defaultValue = "light")
	protected String strict;

	@Parameter(property = "j8b.flashTool", defaultValue = "internal")
	protected String flashToolName;

	@Parameter(property = "j8b.bldrApiReuse", defaultValue = "false")
	protected Boolean bldrApiReuse;

	@Parameter(property = "j8b.softReset", defaultValue = "false")
	protected Boolean softReset;
	
	@Parameter(property = "j8b.autoDownload", defaultValue = "true")
	protected boolean autoDownload;
	
	@Parameter(property = "j8b.downloadUrl", defaultValue = "https://vm5277.ru/releases/vm5277-release-latest.zip")
	protected String downloadUrl;
	
	@Parameter(property = "j8b.toolsDownloadUrl", defaultValue = "https://vm5277.ru/tools/")
	protected String toolsDownloadUrl;
    
	@Parameter(defaultValue = "${reactorProjects}", readonly = true)
    protected List<MavenProject> reactorProjects;
	
	protected final	Logger log = new Logger() {
		@Override public void info(String message) {getLog().info(message);}
		@Override public void warn(String message) {getLog().warn(message);}
		@Override public void error(String message) {getLog().error(message);}
		@Override public void debug(String message) {getLog().debug(message);}
		@Override public boolean progress(int percents, String message) {getLog().info(message + "... " + percents + "%"); return true;}
	};
	
	protected	static	final	HashMap<String, URL[]>	cachedURLs				= new HashMap<>();
	protected	static	final	HashMap<String, Device> cachedDevices			= new HashMap<>();
    public		static	final	int						HTTP_CONNECTION_TIMEOUT = 3000;
    public		static	final	int						HTTP_READ_TIMEOUT		= 3000;

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

	protected String checkTarget() throws MojoExecutionException {
		if(null!=target && !target.isEmpty() && !target.contains(":")) {
			throw new MojoExecutionException(	"[ERROR] Invalid target format. Please specify j8b.target parameter as <platform>:<mcu> " +
												"or omit it for auto-detection. Example: avr:atmega328p");
		}

		if(null==target || target.isEmpty()) {
			File binDir = Paths.get(toolkitPath, "bin").toFile();
			if(!binDir.exists() || !binDir.isDirectory()) {
				throw new MojoExecutionException("Libs directory not found: " + binDir);
			}

			try {
				getLog().info("Try to detect target...");
				String deviceInfo = runDeviceDetection(Paths.get(toolkitPath), Float.parseFloat(targetFreq));
				if(null!=deviceInfo) {
					String parts[] = deviceInfo.trim().split("\\s+", 2);
					target = parts[0x00].toLowerCase();
					getLog().info("Target detected: " + target);
					if(0x02==parts.length) {
						if(!parts[0x01].equalsIgnoreCase(targetStdio)) {
							if(null!=targetStdio && !targetStdio.isEmpty()) {
								getLog().warn("Overriding targetStdio '" + targetStdio + "' with device detection result: '" + parts[0x01] + "'");
							}
							targetStdio = parts[0x01].toLowerCase();
						}
					}
				}
			}
			catch(Exception ex) {
				throw new MojoExecutionException("[ERROR] check target error:" + ex.getMessage());
			}
			
            if(null==target) {
                throw new MojoExecutionException("[ERROR] Device not found. Please check:\n" +
                                                "  1. Device is connected and powered on\n" +
                                                "  2. Serial port is accessible (check permissions)\n" +
                                                "  3. CPU frequency is correct (current: " + targetFreq + " MHz)\n" +
                                                "  4. Use 'j8bf detect " + targetFreq + "' to manually verify device detection");
            }
		}
		return target;
	}

	protected void doClean(Logger log, File _directory) throws MojoExecutionException {
		try {
			FSUtils.deleteDirectory(log, _directory);
		}
		catch(Exception ex) {
			throw new MojoExecutionException("[ERROR] Can't remove directory, " + ex.getMessage());
		}
	}
	
	protected void doCompile() throws MojoExecutionException {
		boolean batch = (null == reactorProjects ? false : 1!=reactorProjects.size());
		
		target = checkTarget();
		
		int index = target.indexOf(":");
		String platformName = target.substring(0x00, index).trim().toLowerCase();
		String mcuStr = target.substring(index+0x01).trim().toLowerCase();

		PlatformType platformType = PlatformType.fromName(platformName);
		if(null==platformType) {
			throw new MojoExecutionException("[ERROR] Unsupported platform:" + platformName);
		}

		OptimizationType optimizationType = OptimizationType.fromName(optimization);
		if(null==optimizationType) {
			throw new MojoExecutionException("[ERROR] Unsupported optimization type:" + optimization);
		}

		StrictLevel strictLevel = StrictLevel.fromName(strict);
		if(null==strictLevel) {
			throw new MojoExecutionException("[ERROR] Unsupported strict level:" + strict);
		}
		
		File mainJ8bFile = new File(sourceDirectory, mainFile);
		if(!mainJ8bFile.exists()) {
			throw new MojoExecutionException(	"Main source file not found: " + mainJ8bFile.getAbsolutePath() + "\nCreate " + mainFile + " in directory: " +
												sourceDirectory);
		}

		if(!batch && verbose) getLog().info("Main source file: " + mainJ8bFile.getAbsolutePath());

		if (!prebuildDirectory.exists()) {
			prebuildDirectory.mkdirs();
		}

		String mainName = mainFile.substring(0, mainFile.lastIndexOf("."));

		int exitCode = buildJ8b(toolkitPath, sourceDirectory, prebuildDirectory, mainName, strict, optimization, batch, false);
		if(0!=exitCode) {
			throw new MojoExecutionException("Compilation failed with exit code: " + exitCode);
		}
	}
	
	public void doAssemble(File outputSubDirectory) throws MojoExecutionException {
		boolean batch = (null == reactorProjects ? false : 1!=reactorProjects.size());
		
		target = checkTarget();
		
		int index = target.indexOf(":");
		String platformName = target.substring(0x00, index).trim().toLowerCase();
		String mcuStr = target.substring(index+0x01).trim().toLowerCase();
		
		PlatformType platformType = PlatformType.fromName(platformName);
		if(null==platformType) {
			throw new MojoExecutionException("[ERROR] Unsupported platform:" + platformName);
		}
		
		OptimizationType optimizationType = OptimizationType.fromName(optimization);
		if(null==optimizationType) {
			throw new MojoExecutionException("[ERROR] Unsupported optimization type:" + optimization);
		}

		StrictLevel strictLevel = StrictLevel.fromName(strict);
		if(null==strictLevel) {
			throw new MojoExecutionException("[ERROR] Unsupported strict level:" + strict);
		}
		
		Device device = null;
		synchronized(cachedDevices) {
			device = cachedDevices.get(platformName.toLowerCase() + ":" + mcuStr);
			if(null==device) {
				try {
					Path _toolkitPath = Paths.get(toolkitPath).normalize().toAbsolutePath();
					Path _rtosPath = _toolkitPath.resolve("rtos").resolve(platformType.name().toLowerCase()).normalize();
					device = new Device(_toolkitPath, _rtosPath, platformType, mcuStr, null, optimizationType, strictLevel);
					cachedDevices.put(platformName.toLowerCase() + ":" + mcuStr, device);
				}
				catch (Exception ex) {
					throw new MojoExecutionException("[ERROR] initialization error:" + ex.getMessage());
				}
			}
		}

		String mainName = mainFile.substring(0, mainFile.lastIndexOf("."));

		File mainAsmFile = new File(prebuildDirectory, mainName + ".asm");
		if(!mainAsmFile.exists()) {
			throw new MojoExecutionException("Main asm file not found: " + mainAsmFile.getAbsolutePath());
		}

		if(!batch && verbose) getLog().info("Main asm file: " + mainAsmFile.getAbsolutePath());
		try {
			copyFile(new File(prebuildDirectory.getAbsolutePath() + File.separator + mainName + ".dbg"),
						new File(outputSubDirectory.getAbsolutePath() + File.separator + mainName + ".dbg"));
		}
		catch(Exception ex) {
		}

		Long flashSize = device.getDefNum("FLASH_SIZE");
		if(null==flashSize) {
			throw new MojoExecutionException("[ERROR] absent 'FLASH SIZE' define for " + device.getUniqueName());
		}
		boolean develMode = false;
//		getLog().info("=== device " + device.getUniqueName() + ", flash size:" + flashSize + ", develMode:" + develMode + " ===");

		int exitCode = buildAsm(toolkitPath, sourceDirectory, prebuildDirectory, outputSubDirectory, platformName, mainName, batch);
		if(0!=exitCode) {
			throw new MojoExecutionException("Assembling failed with exit code: " + exitCode);
		}
	}

	public void doFlash(File outputSubDirectory, boolean interactive) throws MojoExecutionException {
		String mainName = mainFile.substring(0, mainFile.lastIndexOf("."));
		
		int index = target.indexOf(":");
		String platformName = target.substring(0x00, index).trim().toLowerCase();
		String mcuStr = target.substring(index+0x01).trim().toLowerCase();
		
//    Properties projectProperties = project.getProperties();
		int exitCode = 1;
		if(InternalFlashTool.NAME.equalsIgnoreCase(flashToolName)) {
			exitCode = internalFlashHex(toolkitPath, outputSubDirectory, mainName + "_cseg", interactive);
		}
		else {
			FlashTool flashTool = FlashToolProvider.getExternalTool(log, Paths.get(toolkitPath), flashToolName);
			if(null==flashTool) {
				throw new MojoExecutionException("Unsupported flash tool: " + flashToolName);
			}
			Path toolPath = flashTool.resolveTool(true);
			if(null==toolPath) {
				throw new MojoExecutionException("Can't find flash tool: " + flashToolName);
			}
			
			String mcu = flashTool.convertDeviceId(mcuStr);
			
			List<String> args = new ArrayList<>();
			args.add("-p");
			args.add(mcu);

			for(Map.Entry<Object, Object> entry : session.getUserProperties().entrySet()) {
				if(entry.getKey().toString().startsWith("j8b.flashParam.")) {
					String param = entry.getKey().toString().substring("j8b.flashParam.".length());
					String value = entry.getValue().toString();
					if(value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(0x01, value.length()-0x01);
					}
					args.add(param);
					args.add(value);
				}
			}
			
			args.add("-U");
			args.add("flash:w:" + Paths.get(outputSubDirectory.getAbsolutePath(), mainName + "_cseg.hex").normalize().toString() + ":i");
			exitCode = FlashTool.exec(toolPath.toString(), args, !verbose);
		}
		
		if(0!=exitCode) {
			throw new MojoExecutionException("Flashing failed with exit code: " + exitCode);
		}
/*		}
		else {
			log.info("!!! Devel mode unavailable for this device (no bootloader).");
			int length = projectBaseDir.getAbsolutePath().length();
			String filename = (outputDirectory + File.separator + mainName + "_cseg.hex").substring(length+1);
			
			Path toolPath = null;
			String mcuExample = null;
			if(platformType.AVR==platformType) {
				mcuExample = getAvrdudeMcuName(mcuStr);
				toolPath = AvrdudeFlashTool.getAvrdudePath(log, Paths.get(toolkitPath).normalize().toAbsolutePath(), true);
			}
			if(null!=toolPath && null!=mcuExample && platformType.AVR==platformType) {
				List<String> args = new ArrayList<>();
				args.add("-c");
				args.add(programmerTool);
				args.add("-p");
				args.add(mcuExample);
				args.add("-U");
				args.add("flash:w:" + filename + ":i");
				
				//File binDir = Paths.get(toolkitPath, "bin").toFile();
				exitCode = AvrdudeFlashTool.runAvrdude(toolPath.toString(), args, false);
				if(0!=exitCode) {
					throw new MojoExecutionException("Flashing failed with exit code: " + exitCode);
				}
			}
			else {
				log.info("!!! Please flash file " + filename + " manually");
				if(platformType.AVR==platformType) {
					log.info("!!! For example: avrdude -c usbasp -p " + (null==mcuExample ? "m328p" : mcuExample)  + " -U flash:w:" + filename + ":i");
				}
			}
		}*/
	}

/*	Хорошее решение, но не работает с jssc, который используется в flasher'е
	
	protected int runJ8BTool(File libsDir, String mainClassName, List<String> args) throws Exception {
		getLog().debug("Running " + mainClassName + " with args: " + args.toString());

		URL[] urls = null;
		synchronized(cachedURLs) {
			urls = cachedURLs.get(libsDir.getAbsoluteFile());
			if(null==urls) {
				File[] jarFiles = libsDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".jar");
					}
				});

				if(null==jarFiles || 0==jarFiles.length) {
					throw new MojoExecutionException("No JAR files found in: " + libsDir.getAbsolutePath());
				}

				List<URL> _urls = new ArrayList<>();
				for (File jarFile : jarFiles) {
					try {
						_urls.add(jarFile.toURI().toURL());
						getLog().debug("Added to classpath: " + jarFile.getName());
					}
					catch (Exception e) {
						getLog().warn("Failed to add JAR: " + jarFile.getName(), e);
					}
				}
				urls = _urls.toArray(new URL[0]);
				cachedURLs.put(libsDir.getAbsolutePath(), urls);
			}
		}

		URLClassLoader classLoader = new URLClassLoader(urls, null);
		//TODO Убрал getClass().getClassLoader() - кешировал статические объекты запускаемых классов
		//TODO Нужно во всех компонентах обеспечить stateless режим для статики, затем разрешить общий parent или вообще кешировать создаваемый loader
		Method execMethod;
		try {
			execMethod = classLoader.loadClass(mainClassName).getMethod("exec", String[].class);
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
		int result = (int)execMethod.invoke(null, (Object)args.toArray(new String[0]));
		classLoader.close();
		return result;
	}
*/
	protected String runDeviceDetection(Path toolkitPath, float cpuFreq) throws Exception {
		String result = null;
		try {
			StringBuilder stdout = new StringBuilder();
			
			List<String> args = new ArrayList<>();
			args.add("detect");
			args.add(targetFreq);
			args.add("-q");
			if(0==FSUtils.runJ8BTool(Paths.get(this.toolkitPath), "ru.vm5277.flasher.Main", args, stdout)) {
				result = stdout.toString();
			}
		}
		catch(Exception ex) {
			getLog().error(ex);
		}
		return result;
	}
	
	protected void validateTarget() throws MojoExecutionException {
		if (target == null || !target.contains(":")) {
			throw new MojoExecutionException(
				"Invalid target format. Expected: platform:mcu, got: " + target);
		}
	}
	
	protected void copyFile(File source, File destination) throws IOException {
		if(!source.exists()) {
			return;
		}

		try(InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(destination)) {
			byte[] buffer = new byte[8192];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		}
	}
	
	public int buildJ8b(String toolkitPath, File sourceDir, File prebuildDir, String mainName, String strictMode, String optMode,
						boolean isBatch, boolean devMode)																		throws MojoExecutionException {
		int exitCode = 1;
		
		File mainJ8bFile = new File(sourceDir, mainName + ".j8b");
		if(!mainJ8bFile.exists()) {
			throw new MojoExecutionException("Main j8b file not found: " + mainJ8bFile.getAbsolutePath() + "\n");
		}
		
		try {
			List<String> args = new ArrayList<>();
			args.add(target);
			args.add(mainJ8bFile.getAbsolutePath());
			args.add("-P");
			args.add(toolkitPath);
			args.add("-F");
			args.add(targetFreq);
			args.add("-o");
			args.add(prebuildDir.getAbsolutePath());
			if(isBatch || !verbose) {
				args.add("-q");
			}
			else {
				args.add("--dump-ir");
			}
			if(null!=optMode) {
				args.add("--opt");
				args.add(optMode);
			}
			if(null!=strictMode) {
				args.add("--strict");
				args.add(strictMode);
			}
			if(null!=targetStdio && !targetStdio.isEmpty()) {
				args.add("-D");
				args.add("STDIO_PORT=" + targetStdio);
			}

			if(softReset) {
				args.add("-D");
				args.add("SOFT_RESET=1");
			}
			
			if(bldrApiReuse) {
				args.add("-D");
				args.add("BLDR_API_REUSE=1");
			}

			exitCode = FSUtils.runJ8BTool(Paths.get(toolkitPath), "ru.vm5277.compiler.Main", args, null);
		}
		catch(Exception ex) {
			getLog().error(ex);
		}
		return exitCode;
	}
	
	public int buildAsm(String toolkitPath, File sourceDir, File prebuildDir, File outputDir, String platformName, String mainName, boolean isBatch)
																																throws MojoExecutionException {
		int exitCode = 1;
		
		Path rtosPath = Paths.get(toolkitPath, "rtos", platformName);
		if(!rtosPath.toFile().exists()) {
			throw new MojoExecutionException("RTOS directory not found: " + rtosPath.toString() + "\n");
		}

		File mainAsmFile = new File(prebuildDir, mainName + ".asm");
		if(!mainAsmFile.exists()) {
			throw new MojoExecutionException("Main asm file not found: " + mainAsmFile.getAbsolutePath() + "\n");
		}

		if(!outputDir.exists()) {
			outputDir.mkdirs();
		}

		try {
			List<String> args = new ArrayList<>();
			args.add(mainAsmFile.getAbsolutePath());
			args.add("-P");
			args.add(toolkitPath);
			args.add("-I");
			args.add(rtosPath.toAbsolutePath().toString());
			args.add("-o");
			args.add(outputDir.getAbsolutePath() + File.separator + mainName);
			if(isBatch || !verbose) {
				args.add("-q");
			}
			if(verbose) {
				String mapFileName = mainName + ".map";
				String listFileName = mainName + ".lst";

				args.add("-m");
				args.add(mapFileName);
				args.add("-l");
				args.add(listFileName);
			}
			exitCode = FSUtils.runJ8BTool(Paths.get(toolkitPath), "ru.vm5277." + platformName + "_asm.Assembler" , args, null);
		}
		catch(Exception ex) {
			getLog().error(ex);
		}
		return exitCode;
	}
	
	public int internalFlashHex(String toolkitPath, File outputDir, String mainName, boolean intercativeMode) throws MojoExecutionException {
		int exitCode = 1;
		
		File mainHexFile = new File(outputDir, mainName + ".hex");
		if(!mainHexFile.exists()) {
			throw new MojoExecutionException("Hex file not found: " + mainHexFile.getAbsolutePath() + "\n");
		}

		try {
			List<String> args = new ArrayList<>();
			args.add(target);
			args.add(targetFreq);
			args.add(mainHexFile.getAbsolutePath());
			args.add("-P");
			args.add(toolkitPath);
			args.add("-s");
			args.add("-F");
			args.add("-R");
			if(intercativeMode) {
				args.add("-I");
			}
			args.add("-w");
			if(!verbose) {
				args.add("-q");
			}
			exitCode = FSUtils.runJ8BTool(Paths.get(toolkitPath), "ru.vm5277.flasher.Main", args, null);
		}
		catch(Exception ex) {
			getLog().error(ex);
		}
		return exitCode;
	}
	
	public String getAvrdudeMcuName(String mcuName) {
		if(mcuName.startsWith("atmega")) {
			return "m" + mcuName.substring(6);
		}
		else if(mcuName.startsWith("attiny")) {
			return "t" + mcuName.substring(6);
		}
		return null;
	}
}

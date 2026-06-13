/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.common.flash;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Logger;

public class AvrdudeFlashTool implements FlashTool {
    private	static	final	int						HTTP_CONNECTION_TIMEOUT = 3000;
    private	static	final	int						HTTP_READ_TIMEOUT		= 3000;
	public	static			String					DOWNLOAD_URL			= "https://vm5277.ru/tools/";
	public	static	final	String					NAME					= "avrdude";
	private					Logger					log						= null;
	private					Path					toolkitPath				= null;
	private					Path					path					= null;
	private					List<FlashToolParam>	params					= null;
	
	public AvrdudeFlashTool(Logger log, Path toolkitPath) {
		this.log = log;
		this.toolkitPath = toolkitPath;
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
	
	@Override
	public Path resolveTool(boolean canDownload) {
		if(null==toolkitPath) return null;
		Path _path = getAvrdudePath(log, toolkitPath, canDownload);
		if(null==path && null!=_path) {
			path = _path;
			params = null;
		}
		return path;
	}
	
	@Override
	public List<FlashToolParam> getParams() {
		if(null!=params) return params;
		
		params = new ArrayList<>();
		List<String> programmers = getSupportedProgrammers();
		params.add(new FlashToolParam("Programmer", programmers, "usbasp", "-c")); // usbasp - по умолчанию
		params.add(new FlashToolParam("Port", null, "", "-P"));
		return params;
	}

	@Override
	public String convertDeviceId(String deviceId) {
		String _deviceId = deviceId.toLowerCase().trim();

		if(_deviceId.startsWith("attiny")) {
			String num = _deviceId.substring(6);
			if(num.endsWith("a")) {
			num = num.substring(0, num.length()-1);
			}
			return "t" + num;
		}
		else if(_deviceId.startsWith("atmega")) {
			String num = _deviceId.substring(6);
			if(num.endsWith("a") && !num.endsWith("pa") && !num.endsWith("ua")) {
				num = num.substring(0, num.length()-1);
			}
			return "m" + num;
		}
		return deviceId;
	}
	
	public static Path getAvrdudePath(Logger log, Path toolkitPath, boolean canDownload) {
		String result = null;
		
		String avrdudeFolderName = getAvrdudeFolderName();
		if(null==avrdudeFolderName) return null;
		
		String filename = "avrdude" + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "");

		File toolsDir = toolkitPath.resolve(TOOLS_DIRNAME).toFile();
		boolean exist = false;

		if(!toolsDir.exists()) {
			toolsDir.mkdirs();
		}
		else {
			for(File file : toolsDir.listFiles()) {
				if(!file.isDirectory()) continue;

				if(file.getName().equalsIgnoreCase(avrdudeFolderName)) {
					exist = true;
					break;
				}
			}
		}
		
		if(!exist) {
			List<String> args = new ArrayList<>();
			if(0!=FlashTool.exec(filename, args, true)) {
				if(canDownload && downloadAndUnpackAvrdude(log, toolsDir, avrdudeFolderName, filename)) {
					result = toolsDir.getAbsolutePath() + File.separator + avrdudeFolderName + File.separator + filename;
				}
			}
			else {
				result = filename;
			}
		}
		else {
			result = toolsDir.getAbsolutePath() + File.separator + avrdudeFolderName + File.separator + filename;
		}
		return (null==result ? null : Paths.get(result).normalize());
	}

	private static boolean downloadAndUnpackAvrdude(Logger log, File binDir, String folderName, String filename) {
		boolean result = false;
		
		File targetDir = new File(binDir.getAbsolutePath() + File.separator + folderName);
		try {
			if(targetDir.exists()) {
				if(null!=log) log.info("Cleaning existing avrdude directory: " + targetDir.getAbsolutePath());
				FSUtils.deleteDirectory(log, targetDir);
			}
			targetDir.mkdirs();
			File tempFile = new File(binDir.getAbsolutePath() + File.separator + folderName + Long.toHexString(System.currentTimeMillis()) + ".tmp");
			tempFile.deleteOnExit();
			
			String url = DOWNLOAD_URL + folderName  + ".zip";
			if(null!=log) log.info("Downloading avrdude " + url);
			if(FSUtils.downloadFile(log, url, HTTP_CONNECTION_TIMEOUT, HTTP_READ_TIMEOUT, tempFile)) {
				if(null!=log) log.info("Extracting avrdude to: " + targetDir.getAbsolutePath());
				Set<String> filter = new HashSet<>();
				filter.add(filename);
				if(FSUtils.extractZip(log, tempFile, targetDir, filter)) {
					if(targetDir.exists()) {
						result = true;
						if(null!=log) log.info("avrdude successfully installed to: " + targetDir.getAbsolutePath());
					}
					else {
						if(null!=log) log.error("Failed to install avrdude. Installation corrupted.");
					}
				}
			}
			tempFile.delete();
		}
		catch(Exception ex) {
			if(null!=log) log.error(ex.getMessage());
			try {
				FSUtils.deleteDirectory(log, targetDir);
			}
			catch(Exception ex2) {
			}
		}
		return result;
	}

	private static String getAvrdudeFolderName() {
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();
		if(osName.contains("win")) {
			if(osArch.contains("aarch64") || osArch.contains("arm64")) {
				return "avrdude-win-arm64";
			}
			else if(osArch.contains("64")) {
				return "avrdude-win-x64";
			}
			else {
				return "avrdude-win-x32";
			}
		}
		else if(osName.contains("linux")) {
			if(osArch.contains("aarch64") || osArch.contains("arm64")) {
				return "avrdude-lin-arm64";
			}
			else if(osArch.contains("arm") || osArch.contains("armv6")) {
				return "avrdude-lin-armv6";
			}
			else if(osArch.contains("64")) {
				return "avrdude-lin-x64";
			}
			else {
				return "avrdude-lin-x32";
			}
		}
		else if(osName.contains("mac")) {
			return "avrdude-mac";
		}
		return null;
	}

	private List<String> getSupportedProgrammers() {
		List<String> programmers = new ArrayList<>();

		Path _path = resolveTool(false);
		if(null!=_path) {
			List<String> args = new ArrayList<>();
			args.add("-c");
			args.add("?");

			List<String> command = new ArrayList<>();
			command.add(_path.toString());
			command.addAll(args);

			Process process = null;
			try {
				ProcessBuilder pb = new ProcessBuilder(command);
				pb.redirectErrorStream(true);
				process = pb.start();

				StringBuilder output = new StringBuilder();
				try (java.io.BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						output.append(line).append("\n");
					}
				}
				process.waitFor();

				boolean inSection = false;
				for(String line : output.toString().split("\n")) {
					line = line.trim();

					if(!inSection && line.startsWith("Valid programmers")) {
						inSection = true;
						continue;
					}

					if(inSection) {
						String programmer = line.split("\\s+")[0].trim();
						if(!programmer.isEmpty()) {
							programmers.add(programmer);
						}
					}
				}
			}
			catch (Exception ex) {
				if(null!=log) log.error("AvrdudeFlashTool, getSupportedProgrammers exception :" + ex.getMessage());
			}

			if(null!=process) {
				process.destroyForcibly();
			}
		}
		
		return programmers;
	}
}

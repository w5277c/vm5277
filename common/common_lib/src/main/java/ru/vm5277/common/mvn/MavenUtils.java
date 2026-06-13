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

package ru.vm5277.common.mvn;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Logger;
import static ru.vm5277.common.flash.FlashTool.TOOLS_DIRNAME;

public class MavenUtils {
    private	static	final	int						HTTP_CONNECTION_TIMEOUT = 3000;
    private	static	final	int						HTTP_READ_TIMEOUT		= 3000;
	public	static			String					DOWNLOAD_URL			= "https://vm5277.ru/tools/apache-maven-3.9.15-bin.zip";

	public	final	static	String	DIRNAME	= "apache-maven-3.9.15";
	
	public static Path getPath(Logger log, Path toolkitPath, boolean canDownload) {
		String filename = "mvn" + (FSUtils.isWindows() ? ".cmd" : "");
		Path toolsDir = toolkitPath.resolve(TOOLS_DIRNAME);
		Path toolDir = toolsDir.resolve(DIRNAME).resolve("bin").normalize();
		Path toolPath = toolDir.resolve(filename).normalize();

		if(!toolPath.toFile().exists()) {
			List<String> args = new ArrayList<>();
			args.add("-v");
			if(0!=run(filename, args, true)) {
				if(canDownload && downloadAndUnpack(log, toolsDir.toFile(), DIRNAME, filename)) {
					return toolPath;
				}
				return null;
			}
			else {
				return Paths.get(filename);
			}
		}
		else {
			return toolPath;
		}
	}
	
	
	private static boolean downloadAndUnpack(Logger log, File toolsDir, String folderName, String filename) {
		boolean result = false;
		
		File targetDir = new File(toolsDir.getAbsolutePath() + File.separator + folderName);
		try {
			if(targetDir.exists()) {
				if(null!=log) log.info("Cleaning existing directory: " + targetDir.getAbsolutePath());
				FSUtils.deleteDirectory(log, targetDir);
			}
			targetDir.mkdirs();
			File tempFile = new File(toolsDir.getAbsolutePath() + File.separator + folderName + Long.toHexString(System.currentTimeMillis()) + ".tmp");
			tempFile.deleteOnExit();
			
			if(null!=log) log.info("Downloading avrdude " + DOWNLOAD_URL);
			if(FSUtils.downloadFile(log, DOWNLOAD_URL, HTTP_CONNECTION_TIMEOUT, HTTP_READ_TIMEOUT, tempFile)) {
				if(null!=log) log.info("Extracting to: " + targetDir.getAbsolutePath());
				Set<String> filter = new HashSet<>();
				filter.add("bin" + File.separator + "mvn");
				filter.add("bin" + File.separator + "mvnDebug"); //TODO костыль, похоже есть способ указать упаковщику сохранять права запуска
				filter.add("bin" + File.separator + "mvnyjp");
				if(FSUtils.extractZip(log, tempFile, targetDir, filter)) {
					if(targetDir.exists()) {
						result = true;
						if(null!=log) log.info("Successfully installed to: " + targetDir.getAbsolutePath());
					}
					else {
						if(null!=log) log.error("Failed to install. Installation corrupted.");
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

	public static int run(String toolPath, List<String> args, boolean silent) {
		try {
			List<String> command = new ArrayList<>();
			command.add(toolPath);
			command.addAll(args);

			ProcessBuilder pb = new ProcessBuilder(command);
			if (!silent) {
				pb.inheritIO();
			}
			Process process = pb.start();
			return process.waitFor();
		}
		catch(Exception ex)  {
		}
		return -1;
	}	
}

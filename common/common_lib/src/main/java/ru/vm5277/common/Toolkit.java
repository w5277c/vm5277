package ru.vm5277.common;

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

import java.io.File;
import java.nio.file.Path;

public class Toolkit {
	public	static	final	String	DONWLOAD_URL			= "https://vm5277.ru/releases/vm5277-release-latest.zip";
	private	static	final	int		HTTP_CONNECTION_TIMEOUT = 3000;
    private	static	final	int		HTTP_READ_TIMEOUT		= 3000;

	public static boolean checkToolkit(Logger log, Path toolkitPath, String downloadUrl, boolean canDownload) {
		if(null==toolkitPath) {
			toolkitPath = FSUtils.getToolkitPath();
		}
		if(!toolkitPath.toFile().exists()) {
			String env = System.getenv("vm5277");
			if(null!=env) {
				toolkitPath = FSUtils.resolveWithEnv(env);
			}
			if(!toolkitPath.toFile().exists()) {
				if(canDownload) {
					if(null!=log) log.info("Toolkit not found, downloading...");
					if(!downloadAndUnpack(log, toolkitPath, downloadUrl)) {
						return false;
					}
				}
				else {
					if(null!=log) log.error("Toolkit not found at: " + toolkitPath.toString() + "\n" +
											"Possible solutions:\n"	+
											"1. Set autoDownload=true in plugin configuration\n" +
											"2. Manually download from: " + downloadUrl + "\n" +
											"3. Set toolkit path via j8b.toolkitPath parameter (if used Maven)\n" +
											"4. Set vm5277 environment variable");
					return false;
				}
			}
		}

		if(null!=log)  log.info("Using toolkit at: " + toolkitPath.toString());
		
		return true;
	}

	
	public static boolean downloadAndUnpack(Logger log, Path toolkitPath, String downloadUrl) {
		boolean result = false;
		
		File targetDir = toolkitPath.toFile();
		File tempFile = null;
		try {
			if(targetDir.exists()) {
				if(null!=log) log.info("Cleaning existing directory: " + targetDir.getAbsolutePath());
				FSUtils.deleteDirectory(log, targetDir);
			}
			targetDir.mkdirs();
			tempFile = new File(Long.toHexString(System.currentTimeMillis()) + ".tmp");
			tempFile.deleteOnExit();
			
			if(null!=log) log.info("Downloading " + downloadUrl + " ...");
			if(FSUtils.downloadFile(log, downloadUrl, HTTP_CONNECTION_TIMEOUT, HTTP_READ_TIMEOUT, tempFile)) {
				if(null!=log) log.info("Downloading done, extracting to: " + targetDir.getAbsolutePath() + " ...");
				if(FSUtils.extractZip(log, tempFile, targetDir, null)) {
					if(targetDir.exists()) {
						result = true;
						if(null!=log) log.info("Successfully extracted to: " + targetDir.getAbsolutePath());
					}
					else {
						try {FSUtils.deleteDirectory(log, targetDir);} catch(Exception ex2) {}
						if(null!=log) log.error("Extraction failed, downloaded data is corrupted.");
					}
				}
			}
		}
		catch(Exception ex) {
			if(null!=log) log.error(ex.getMessage());
			try {FSUtils.deleteDirectory(log, targetDir);} catch(Exception ex2) {}
		}
		if(null!=tempFile) {
			tempFile.delete();
		}
		if(!result) {
			try {FSUtils.deleteDirectory(null, targetDir);} catch(Exception ex) {}
		}
		return result;
	}
}

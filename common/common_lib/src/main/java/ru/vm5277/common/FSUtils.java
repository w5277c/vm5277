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
package ru.vm5277.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.vm5277.common.exceptions.CompileException;

public class FSUtils {
	
	public static Path getToolkitPath() throws CompileException {
		String toolkitPath = System.getenv("vm5277");
		if(null==toolkitPath || toolkitPath.isEmpty()) {
			return null;
		}
		return Paths.get(toolkitPath).normalize().toAbsolutePath();
	}

	public static Path resolve(Path home, String path) {
		Path resolvedPath = resolveWithEnv(path);
		if(resolvedPath.isAbsolute()) {
			return resolvedPath.normalize();
		}

		Path currentDirPath = Paths.get("").toAbsolutePath().resolve(resolvedPath).normalize();
		if(currentDirPath.toFile().exists()) {
			return currentDirPath;
		}
		
		return home.resolve(resolvedPath).normalize();
	}
	
	public static Path resolveWithEnv(String path) {
		Pattern pattern = Pattern.compile("\\$\\{?(\\w+)\\}?");
		Matcher matcher = pattern.matcher(path);
		StringBuffer resolved = new StringBuffer();

		while (matcher.find()) {
			String varName = matcher.group(1);
			String varValue = System.getenv(varName);
			if(null != varValue) {
				matcher.appendReplacement(resolved, varValue);
			}
		}
		matcher.appendTail(resolved);
		
		return Paths.get(expandTilde(resolved.toString())).normalize();
	}
	
	public static String getBaseName(Path path) {
		String fileName = path.getFileName().toString();
		int pos = fileName.lastIndexOf(".");
		if(-1 == pos) return fileName;
		return fileName.substring(0, pos);
	}
	
	private static String expandTilde(String path) {
		if(path==null || !path.startsWith("~")) {
			return path;
		}

		if(path.equals("~")) {
			return System.getProperty("user.home");
		}
		
		return System.getProperty("user.home") + path.substring(1);
	}
}


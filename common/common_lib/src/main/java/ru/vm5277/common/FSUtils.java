/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
16.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FSUtils {
	
	public static Path getToolkitPath() {
		String toolkitPath = System.getenv("VM5277");
		if(null == toolkitPath || toolkitPath.isEmpty()) {
			File currentDir = new File("").getAbsoluteFile();
			File parentDir = currentDir.getParentFile().getParentFile();
			toolkitPath = parentDir.getAbsolutePath();
		}
		return Paths.get(toolkitPath).normalize().toAbsolutePath();
	}

	public static Path resolve(Path home, String path) {
		Path resolvedPath = resolveWithEnv(path);
		if (resolvedPath.isAbsolute()) return resolvedPath.normalize();
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
		return Paths.get(resolved.toString()).normalize();
	}
	
	public static String getBaseName(Path path) {
		String fileName = path.getFileName().toString();
		int pos = fileName.lastIndexOf(".");
		if(-1 == pos) return fileName;
		return fileName.substring(0, pos);
	}
}


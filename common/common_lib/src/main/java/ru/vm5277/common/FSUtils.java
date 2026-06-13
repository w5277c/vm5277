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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FSUtils {
	public static Path getToolkitPath() {
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
	
	public static void deleteDirectory(Logger log, File dir) throws IOException {
		if(dir.exists()) {
			File[] files = dir.listFiles();
			if(files!=null) {
				for(File file : files) {
					if(file.isDirectory()) {
						deleteDirectory(log, file);
					}
					else {
						if(!file.delete() && null!=log) {
							log.warn("Failed to delete file: " + file.getAbsolutePath());
						}
					}
				}
			}
			if(!dir.delete() && null!=log) {
				log.warn("Failed to delete directory: " + dir.getAbsolutePath());
			}
		}
	}
	
	public static boolean downloadFile(Logger log, String urlString, int connectionTimeout, int readTimeout, File outputFile) {
		HttpURLConnection connection = null;
		boolean isCancelled=false;
		try {
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "vm5277 util v1.0");
			connection.setConnectTimeout(connectionTimeout);
			connection.setReadTimeout(readTimeout);
			
			int responseCode = connection.getResponseCode();
			if(responseCode==HttpURLConnection.HTTP_OK) {
				long contentLength = connection.getHeaderFieldLong("Content-Length", -1);
				try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(outputFile)) {

					byte[] buffer = new byte[8192];
					int bytesRead;
					long totalBytes = 0;
					long timestamp = System.currentTimeMillis();
					while(!isCancelled && -1!=(bytesRead = in.read(buffer))) {
						out.write(buffer, 0, bytesRead);
						totalBytes += bytesRead;

						if(null!=log && -1!=contentLength && (System.currentTimeMillis()-timestamp)>500) {
							if(!log.progress((int)(100f/contentLength*totalBytes), "Downloading")) {
								log.info("Downloading cancelled");
								isCancelled = true;
							}
							timestamp = System.currentTimeMillis();
						}
					}

					if(!isCancelled) {
						if(null!=log) log.progress(100, "Downloading");
						if(null!=log) log.info("Downloaded " + (totalBytes / 1024) + " KB");
						connection.disconnect();
						return true;
					}
				}
			}
			else {
				if(null!=log) log.error("Server returned HTTP " + responseCode + ": " + urlString);
			}
		}
		catch(MalformedURLException e) {
			if(null!=log) log.error("Invalid download URL: " + urlString);
		}
		catch(IOException ex) {
			if(null!=log) log.error("Download error " + ex.getMessage() + " for " + urlString);
		}
		
		if(null!=connection) connection.disconnect();
		if(!outputFile.delete()) {
			outputFile.deleteOnExit();
		}
		return false;
	}

	public static boolean extractZip(Logger log, File zipFile, File targetDir, Set<String> execFilter) throws IOException {
		boolean isCancelled = false;
		try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			int totalEntries = 0;
			try(ZipFile zf = new ZipFile(zipFile)) {
				totalEntries = zf.size();
			}

			int processedEntries = 0;
			ZipEntry entry;
			byte[] buffer = new byte[8192];
			long timestamp = System.currentTimeMillis();
			
			while(!isCancelled && null!=(entry = zis.getNextEntry())) {
				File newFile = new File(targetDir, entry.getName());

				// Защита от Zip Slip
				if(!newFile.toPath().normalize().startsWith(targetDir.toPath().normalize())) {
					throw new IOException("Malicious zip entry: " + entry.getName());
				}

				if(entry.isDirectory()) {
					newFile.mkdirs();
					processedEntries++;
					continue;
				}

				// Создаем родительские директории если нужно
				newFile.getParentFile().mkdirs();

				try(FileOutputStream fos = new FileOutputStream(newFile)) {
					int len;
					while(0<(len = zis.read(buffer))) {
						fos.write(buffer, 0, len);
					}
				}

				if(!isWindows()) {
					if(	(null!=execFilter && execFilter.contains(entry.getName().toLowerCase())) ||
						(entry.getName().startsWith("bin/") && entry.getName().toLowerCase().endsWith(".sh"))) {
						
						newFile.setExecutable(true, false);
					}
				}
				processedEntries++;
				
				if(null!=log && (System.currentTimeMillis()-timestamp)>500) {
					if(!log.progress((int)(100f/totalEntries*processedEntries), "Extracting")) {
						log.info("Extracting cancelled");
						isCancelled = true;
					}
					timestamp = System.currentTimeMillis();
				}
				
				zis.closeEntry();
			}
			
			if(!isCancelled) {
				if(null!=log) log.progress(100, "Extracting");
				return true;
			}
		}
		catch(Exception ex) {
			if(null!=log) log.error("Extract " + zipFile.getName() + " error " + ex.getMessage());
		}
		
		FSUtils.deleteDirectory(null, targetDir);
		return false;
	}
	
	public static int runJ8BTool(Path toolkitPath, String mainClassName, List<String> args, StringBuilder stdout) throws IOException {
		Path binPath = toolkitPath.resolve("bin").normalize();
		if(!binPath.toFile().exists() || !binPath.toFile().isDirectory()) {
			throw new IOException("Bin directory not found: " + binPath.toString());
		}

		Path libsPath = binPath.resolve("libs").normalize();
		if(!libsPath.toFile().exists() || !libsPath.toFile().isDirectory()) {
			throw new IOException("Libs directory not found: " + libsPath.toString());
		}

		Process proc = null;
		int result = 1;
		try {			
			String javaHome = System.getProperty("java.home");
			String javaExe = javaHome + File.separator + "bin" + File.separator + "java" + (FSUtils.isWindows() ? ".exe" : "");

			List<String> cmd = new ArrayList<>();
			cmd.add(javaExe);
			cmd.add("-cp");
			cmd.add(libsPath.toString() + File.separator + "*");
			cmd.add(mainClassName);
			cmd.addAll(args);

			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);

			Thread outputReader = null;
			if(null==stdout) {
				pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
				pb.redirectInput(ProcessBuilder.Redirect.INHERIT); 
				proc = pb.start();
			}
			else {
				proc = pb.start();
				final Process _proc = proc;

				outputReader = new Thread(new Runnable() {
					@Override
					public void run() {
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(_proc.getInputStream()))) {
							String line;
							while ((line = reader.readLine()) != null) {
								stdout.append(line).append("\n");
							}
						}
						catch (IOException ignore) {
						}
					}
				});
				outputReader.setDaemon(true);
				outputReader.start();
			}
			result = proc.waitFor();
			if(null!=outputReader) {
				outputReader.join();
			}
		}
		catch(Exception ex) {
		}
		finally {
			if(null!=proc) {
				try {proc.getInputStream().close();} catch(Exception ignored) {}
				if(proc.isAlive()) {
					try {proc.destroyForcibly();} catch(Exception ignored) {}
				}
			}
		}
		return result;
	}

	public static boolean isWindows() {
		String prop = System.getProperty("os.name");
		if(null==prop) return false;
		return prop.toLowerCase().contains("win");
	}
	
	public static String getTargetFromAsmPrebild(File file) {
		String result = null;
		if(null!=file && file.exists() && !file.isDirectory()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				String line = reader.readLine();
				if(!line.trim().isEmpty() && line.toLowerCase().startsWith("; vm5277.")) {
					int pos = line.indexOf(",");
					if(-1!=pos) {
						result = line.toLowerCase().substring(9, pos);
					}
				}
			}
			catch (Exception ex) {
			}
		}
		return result;
	}
	
	public static void moveUp(File sourceDir) {
		File targetDir = sourceDir.getParentFile();

		if(sourceDir.exists() && sourceDir.isDirectory()) {
			File[] files = sourceDir.listFiles();
			if(null!=files) {
				for(File file : files) {
					file.renameTo(new File(targetDir, file.getName()));
				}
			}
		}
	}
	
	public static String getHomeDir() {
		return System.getProperty("user.home");
	}
}


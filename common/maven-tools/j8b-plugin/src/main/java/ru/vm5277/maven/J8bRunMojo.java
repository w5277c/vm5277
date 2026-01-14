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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.plugins.annotations.Execute;

@Mojo(name = "run", defaultPhase = LifecyclePhase.VERIFY)
@Execute(phase = LifecyclePhase.COMPILE, goal = "compile")
public class J8bRunMojo extends J8bMojo {
	@Override
	public void execute() throws MojoExecutionException {
		// Проверяем, что это не родительский проект
		if("pom".equals(project.getPackaging())) {
			getLog().warn("J8B run goal cannot be executed on parent POM project.");
			getLog().warn("Please run 'mvn j8b:run' from a specific module directory instead.");
			return;
		}
		checkToolkit();
		getLog().info("=== J8B Maven plugin  - RUN ===");

		if(null==target || target.isEmpty() || !target.contains(":")) {
			throw new MojoExecutionException("[ERROR] Target parameter is absent or incorrect, please specify j8b.target parameter. Example: avr:atmega328p");
		}

		String mainName = mainFile.substring(0, mainFile.lastIndexOf("."));

		Path libsPath = Paths.get(toolkitPath, "bin", "libs");

		getLog().info("Flashing...");
		File mainHexFile = new File(outputDirectory, mainName + "_cseg.hex");
		if(!mainHexFile.exists()) {
			throw new MojoExecutionException("Main hex file not found: " + mainHexFile.getAbsolutePath() + "\n");
		}

		int exitCode = 1;
		try {
			exitCode = runJ8BTool(	libsPath, "ru.vm5277.flasher.Main",
									target,
									targetFreq,
									mainHexFile.getAbsolutePath(),
									"-P", toolkitPath,
									"-s",
									"-F",
									"-V",
									"-R");
		}
		catch(Exception ex) {
		}
		if(0!=exitCode) {
			throw new MojoExecutionException("Flashing failed with exit code: " + exitCode);
		}

		getLog().info("=== Run finished successfully ===");
	}
}

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

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class J8bCompileMojo extends J8bMojo {
	@Override
	public void execute() throws MojoExecutionException {
		if(skip) {
			getLog().info("J8B compilation is skipped");
			return;
		}
		
		if("pom".equals(project.getPackaging())) {
			getLog().info("Skipping j8b compilation for parent POM project");
			return;
		}
		checkToolkit();
		getLog().info("=== J8B Maven plugin v" + readVersion() + " - COMPILE ===");
		
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

		getLog().info("Compilation...");
		int exitCode = 1;
		try {
			exitCode = runJ8BTool(	libsPath, "ru.vm5277.compiler.Main",
									target,
									mainJ8bFile.getAbsolutePath(),
									"-P", toolkitPath,
									"-F", targetFreq,
									"-o", outputDirectory.getAbsolutePath());
		}
		catch(Exception ex) {
		}
		if(0!=exitCode) {
			throw new MojoExecutionException("Compilation failed with exit code: " + exitCode);
		}

		getLog().info("Assembling...");
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
			exitCode = runJ8BTool(	libsPath, "ru.vm5277." + platformStr + "_asm.Assembler",
									mainAsmFile.getAbsolutePath(),
									"-P", toolkitPath,
									"-I", defsPath.toAbsolutePath().toString(),
									"-I", rtosPath.toAbsolutePath().toString());
		}
		catch(Exception ex) {
		}
		if(0!=exitCode) {
			throw new MojoExecutionException("Assembling failed with exit code: " + exitCode);
		}

		getLog().info("=== Compilation finished successfully ===");
	}
}

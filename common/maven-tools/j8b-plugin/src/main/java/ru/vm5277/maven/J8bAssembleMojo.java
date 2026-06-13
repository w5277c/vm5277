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

import java.io.File;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import ru.vm5277.common.Toolkit;

@Mojo(name = "assemble", threadSafe = true)
public class J8bAssembleMojo extends J8bMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(skip) return;

		// Проверяем, что это не родительский проект
		if("pom".equals(project.getPackaging())) {
			getLog().warn("J8B assemble goal cannot be executed on parent POM project.");
			getLog().warn("Please run 'mvn j8b:assemble' from a specific module directory instead.");
			return;
		}
		
		File outputSubDirectory = new File(outputDirectory, "default");
		if(!session.getRequest().getActiveProfiles().isEmpty()) outputSubDirectory = new File(outputDirectory, session.getRequest().getActiveProfiles().get(0));
		if(!outputSubDirectory.exists()) {
			outputSubDirectory.mkdirs();
		}

		doClean(log, outputSubDirectory);

		if(!Toolkit.checkToolkit(verbose ? log : null, Path.of(toolkitPath), downloadUrl, true)) {
			throw new MojoExecutionException("Toolkit not found, can't download toolkit from: " + downloadUrl + ", please try later.");
		}

		if(null==target || target.isEmpty() || !target.contains(":")) {
			throw new MojoExecutionException("[ERROR] Target parameter is absent or incorrect, please specify j8b.target parameter. Example: avr:atmega328p");
		}

		doAssemble(outputSubDirectory);
	}
}

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

package ru.vm5277.maven;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import java.nio.file.Path;
import ru.vm5277.common.Toolkit;

@Mojo(name = "flash")
public class J8bFlashMojo extends J8bMojo {
	@Override
	public void execute() throws MojoExecutionException {
		if(skip) return;

		// Проверяем, что это не родительский проект
		if("pom".equals(project.getPackaging())) {
			log.warn("J8B flash goal cannot be executed on parent POM project.");
			log.warn("Please run 'mvn j8b:flash' from a specific module directory instead.");
			return;
		}

		File outputSubDirectory = new File(outputDirectory, "default");
		if(!session.getRequest().getActiveProfiles().isEmpty()) outputSubDirectory = new File(outputDirectory, session.getRequest().getActiveProfiles().get(0));
		if(!outputSubDirectory.exists()) {
			outputSubDirectory.mkdirs();
		}

		if(!Toolkit.checkToolkit(verbose ? log : null, Path.of(toolkitPath), downloadUrl, true)) {
			throw new MojoExecutionException("Toolkit not found, can't download toolkit from: " + downloadUrl + ", please try later.");
		}
		
		doFlash(outputSubDirectory, false);
	}
}

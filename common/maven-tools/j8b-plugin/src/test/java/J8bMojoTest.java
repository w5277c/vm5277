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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.vm5277.maven.J8bMojo;

public class J8bMojoTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Rule
	public TestResources resources = new TestResources();

	@Test
	public void testMojoExecution() throws Exception {

		File parentProjectDir = new File("/home/kostas/repos/w5277c/vm5277_local/examples/j8b");
		File projectDir = new File(parentProjectDir.getAbsolutePath() + "/hello");
		File pom = new File(parentProjectDir, "pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		J8bMojo mojo = (J8bMojo) rule.lookupMojo("compile", pom);
		assertNotNull(mojo);

		// Создаем мок MavenProject
		MavenProject mockProject = mock(MavenProject.class);
		when(mockProject.getBasedir()).thenReturn(projectDir);
		when(mockProject.getBuild()).thenReturn(new Build());
		when(mockProject.getVersion()).thenReturn("0.1.0");
		when(mockProject.getArtifactId()).thenReturn("test-project");
		when(mockProject.getGroupId()).thenReturn("test.group");

		rule.setVariableValueToObject(mojo, "project", mockProject);
		rule.setVariableValueToObject(mojo, "mainFile", "Main.j8b");
		rule.setVariableValueToObject(mojo, "target", "avr:atmega328p");
		rule.setVariableValueToObject(mojo, "targetFreq", "16.0");
		rule.setVariableValueToObject(mojo, "sourceDirectory", new File(projectDir.toString() + File.separator + "src" + File.separator + "main" +
																		File.separator + "j8b"));
		rule.setVariableValueToObject(mojo, "outputDirectory", new File(projectDir.toString() + File.separator + "target"));
		rule.setVariableValueToObject(mojo, "autoDownload", true);
		rule.setVariableValueToObject(mojo, "downloadUrl", "https://vm5277.ru/release.zip");

		String userHome = System.getProperty("user.home");
		String expectedPath = userHome + File.separator + "vm5277";
		rule.setVariableValueToObject(mojo, "toolkitPath", expectedPath);

		// Запускаем выполнение
		mojo.execute();
	}
}
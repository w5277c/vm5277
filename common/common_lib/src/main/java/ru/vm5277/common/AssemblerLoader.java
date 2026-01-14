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

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class AssemblerLoader {
	public static AssemblerInterface load(Path toolkitPath, String platform) throws Exception {
        Path asmJarPath = toolkitPath.resolve("bin").resolve("libs").resolve(platform + "_assembler.jar").normalize();
		if(!asmJarPath.toFile().exists()) {
			throw new FileNotFoundException(asmJarPath.toString());
		}
		
        URLClassLoader classLoader = new URLClassLoader(new URL[]{asmJarPath.toUri().toURL()}, AssemblerInterface.class.getClassLoader());
        Class<?> clazz = classLoader.loadClass("ru.vm5277." + platform + "_asm.Assembler");
        Constructor<?> constructor = clazz.getConstructor();
        return (AssemblerInterface) constructor.newInstance();
    }
}

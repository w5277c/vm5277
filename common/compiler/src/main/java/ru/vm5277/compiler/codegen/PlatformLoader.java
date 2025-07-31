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
package ru.vm5277.compiler.codegen;

import java.io.BufferedWriter;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Map;
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.SourceType;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.messages.MessageContainer;

public class PlatformLoader {
	private	static	final	String	LIB_CG_POSTFIX		= "_codegen";
	private	static	final	String	LIB_CG_CLASSPREFIX	= "ru.vm5277.compiler.";
	private	static	final	String	LIB_CG_CLASSPOSTFIX= "_codegen.Generator";
	private	static	final	String	LIB_ASM_POSTFIX		= "_assembler";
	private	static	final	String	LIB_ASM_CLASSPREFIX	= "ru.vm5277.";
	private	static	final	String	LIB_ASM_CLASSPOSTFIX= "_asm.Assembler";
	
	public static CodeGenerator loadGenerator(	String platform, File libsDir, Map<String, NativeBinding> nbMap,
												Map<SystemParam, Object> params) throws Exception {
		// Формируем имя JAR-файла (например, "codegen-avr.jar")
		String jarName =  platform + LIB_CG_POSTFIX + ".jar";
		File jarFile = new File(libsDir + File.separator + jarName);

		if (!jarFile.exists()) throw new RuntimeException("Library not found: " + jarFile.getAbsolutePath());

		// Динамическая загрузка JAR
		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, PlatformLoader.class.getClassLoader());

		// Загружаем класс-генератор (ожидаемое имя: ru.vm5277.compiler.PLATFORM_codegen.Generator)
		String className = LIB_CG_CLASSPREFIX + platform + LIB_CG_CLASSPOSTFIX;
		Class<?> generatorClass = classLoader.loadClass(className);
		Constructor<?> constructor = generatorClass.getConstructor(String.class, Map.class, Map.class);

		// Создаем экземпляр, передавая параметры
		return (CodeGenerator) constructor.newInstance(platform + LIB_CG_POSTFIX, nbMap, params);
	}
	
	public static boolean loadAssembler(	String platform, File libsDir, MessageContainer mc, Path sourcePath, Map<Path, SourceType> sourcePaths,
													String outputFilename) throws Exception {
		// Формируем имя JAR-файла
		String jarName =  platform + LIB_ASM_POSTFIX + ".jar";
		File jarFile = new File(libsDir + File.separator + jarName);

		if (!jarFile.exists()) throw new RuntimeException("Library not found: " + jarFile.getAbsolutePath());

		// Динамическая загрузка JAR
		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, PlatformLoader.class.getClassLoader());

		// Загружаем класс (ожидаемое имя: ru.vm5277.PLATFORM_asm.Assembler)
		String className = LIB_ASM_CLASSPREFIX + platform + LIB_ASM_CLASSPOSTFIX;
		Class<?> asmClass = classLoader.loadClass(className);
		Method method = asmClass.getMethod("exec", MessageContainer.class, String.class, Path.class, Map.class, int.class, String.class, File.class, BufferedWriter.class);
		return (boolean) method.invoke(asmClass.newInstance(), mc, null, sourcePath, sourcePaths, AssemblerInterface.STRICT_LIGHT, outputFilename, null, null);
	}

}
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

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CodeGenerator;

public class PlatformLoader {
	private	static	final	String	LIB_POSTFIX		= "_codegen";
	private	static	final	String	LIB_CLASSPREFIX	= "ru.vm5277.compiler.";
	private	static	final	String	LIB_CLASSPOSTFIX= "_codegen.Generator";
	
	public static CodeGenerator loadGenerator(	String platform, File libsDir, Map<String, NativeBinding> nbMap,
												Map<SystemParam, Object> params) throws Exception {
		// Формируем имя JAR-файла (например, "codegen-avr.jar")
		String jarName =  platform + LIB_POSTFIX + ".jar";
		File jarFile = new File(libsDir + File.separator + jarName);

		if (!jarFile.exists()) throw new RuntimeException("Library not found: " + jarFile.getAbsolutePath());

		// Динамическая загрузка JAR
		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, PlatformLoader.class.getClassLoader());

		// Загружаем класс-генератор (ожидаемое имя: ru.vm5277.compiler.codegen.platform.Generator)
		String className = LIB_CLASSPREFIX + platform + LIB_CLASSPOSTFIX;
		Class<?> generatorClass = classLoader.loadClass(className);
		Constructor<?> constructor = generatorClass.getConstructor(String.class, Map.class, Map.class);

		// Создаем экземпляр, передавая параметры
		return (CodeGenerator) constructor.newInstance(platform + LIB_POSTFIX, nbMap, params);
	}
}
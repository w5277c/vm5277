/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.codegen;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.RegisterMap;

public class PlatformLoader {
	private	static	final	String	LIB_PREFIX		= "codegen_";
	private	static	final	String	LIB_DIR			= "lib/";
	private	static	final	String	LIB_CLASSPATH	= "ru.vm5277.j8b.compiler.codegen.";
	private	static	final	String	LIB_CLASSNAME	= ".Generator";
	
	public static CodeGenerator loadGenerator(String platform, Map<String, RegisterMap> regMap, Map<String, String> params) throws Exception {
		// Формируем имя JAR-файла (например, "codegen-avr.jar")
		String jarName = LIB_PREFIX + platform + ".jar";
		File jarFile = new File(LIB_DIR + jarName);

		if (!jarFile.exists()) throw new RuntimeException("Library not found: " + jarFile.getAbsolutePath());

		// Динамическая загрузка JAR
		URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, PlatformLoader.class.getClassLoader());

		// Загружаем класс-генератор (ожидаемое имя: ru.vm5277.j8b.compiler.codegen.platform.Generator)
		String className = LIB_CLASSPATH + platform + LIB_CLASSNAME;
		Class<?> generatorClass = classLoader.loadClass(className);
		Constructor<?> constructor = generatorClass.getConstructor(Map.class, Map.class);

		// Создаем экземпляр, передавая параметры
		return (CodeGenerator) constructor.newInstance(regMap, params);
	}
}
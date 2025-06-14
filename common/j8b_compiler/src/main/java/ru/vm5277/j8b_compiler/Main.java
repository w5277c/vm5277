/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Map;
import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.j8b_compiler.codegen.PlatformLoader;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.ClassNode;

public class Main {
    public	final	static	String	VERSION		= "0.0.23";
	public			static	boolean	isWindows;
	public			static	String	toolkitPath;

	public static void main(String[] args) throws IOException, Exception {
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		Map<String, String> tmp = System.getenv();
		toolkitPath = System.getenv("VM5277_HOME");
		if(null == toolkitPath || toolkitPath.isEmpty()) {
			File currentDir = new File("").getAbsoluteFile();
			File parentDir = currentDir.getParentFile().getParentFile();
			toolkitPath = parentDir.getAbsolutePath();
		}
		
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01 == args.length) {
			if(args[0x00].equalsIgnoreCase("--version")) {
				System.out.println("J8b compiler version: " + VERSION);
			}
			else {
				showHelp();
			}
			System.exit(0);
		}

		String platformsPath = (null != toolkitPath && !toolkitPath.isEmpty() ? toolkitPath + File.separator + "platform" : null);
		String platform = null;
		String mcu = null;
		String source = null;
		
		if(0x02 <= args.length) {
			String[] parts = args[0x00].split(":");
			if(0x02 == parts.length) {
				platform = parts[0];
				mcu = parts[1];
			}
			source = args[1];
			
			for(int i=2; i< args.length; i++) {
				String arg = args[i];
				if(args.length > i+1) {
					if(arg.equalsIgnoreCase("--platformspath") || arg.equals("-P")) {
						platformsPath = args[++i];
					}
				}
				else {
					System.err.println("[ERROR] Invalid parameter:" + arg);
					System.exit(0);
				}
			}
		}
		else {
			showHelp();
			System.exit(0);
		}
 
		if(null == platform || platform.isEmpty() || null == mcu || mcu.isEmpty()) {
			showInvalidDeviceFormat(args[0]);
			System.exit(0);
		}
		
		File platformsDir = null;
		if(null != platformsPath && !platformsPath.isEmpty()) {
			platformsDir = new File(platformsPath);
		}

		if(null == platformsDir || !platformsDir.exists()) {
			showInvalidPlatformDir(platformsPath);
			System.exit(0);
		}
		
		
		MessageContainer mc = new MessageContainer(8, true, false);
		
		File runtimeDir = new File(toolkitPath + File.separator + "runtime");
		RegisterMapLoader rml = new RegisterMapLoader(runtimeDir, mc);
		
		File libDir = new File(toolkitPath + File.separator + "bin" + File.separator + "libs");
		CodeGenerator cg = PlatformLoader.loadGenerator(platform, libDir, rml.getMap(), null);
		
		File sourceFile = new File(source);
		File baseDir = sourceFile.getParentFile();
		Lexer lexer = new Lexer(sourceFile, mc);
		ASTParser parser = new ASTParser(runtimeDir, baseDir, lexer.getTokens(), mc);
		ClassNode clazz = parser.getClazz();
//			new ASTPrinter(parser.getClazz());
		new SemanticAnalyzer(runtimeDir, parser.getClazz());
		new ASTPrinter(parser.getClazz());

		if(0 == mc.getErrorCntr()) {
			clazz.codeGen(cg);
		}
    }
	
	private static void showHelp() {
		System.out.println("J8b Compiler: Java-like source code compiler for vm5277 Embedded Toolkit");
		System.out.println("Version: " + VERSION + " | License: GPL-3.0-or-later");
		System.out.println("-------------------------------------------");
		System.out.println();
		System.out.println("This tool is part of the open-source project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: j8bc" + (isWindows ? ".exe" : "") + " <platform>:<mcu> <input.j8b> [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -o, --output <file>\tOutput HEX file (default: <input>.hex)");
		System.out.println("  -P, --platform <dir>\tCustom platforms directory path");
		System.out.println("  -I, --include <dir>\tAdditional include path(s)");
		System.out.println("  -v, --version\t\tDisplay version");
		System.out.println("  -h, --help\t\tShow this help");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  j8bc avr:atmega328p main.j8b -o firmware.hex -I ./libs");
	}
	
	private static void showInvalidDeviceFormat(String invalidParam) {
		System.err.println("[ERROR] Invalid device format. Expected: <platform>:<mcu>");
		System.err.println("Examples:");
		System.err.println("  avr:atmega328p");
		System.err.println("  stm8:stm8s003f3");
		System.err.println("  pic:16f877a");
		System.err.println("Detected: '" + invalidParam + "'");
		System.err.println("Supported platforms and MCUs are listed in the 'platform' directory of the project");
		System.err.println("Project location: " + Paths.get("").toAbsolutePath().resolve("platform"));
	}
	
	private static void showInvalidPlatformDir(String platformPath) {
		System.err.println("[ERROR] Platform directory not found or empty");
		System.err.println();
		System.err.println("Tried to access: " + platformPath);
		System.err.println();
		System.err.println("Possible solutions:");
		System.err.println("1. Specify custom path with --platformspath (-P) option");
		System.err.println("2. Add platform directory(VM5277_PLATFORM) to system environment variables");
		System.err.println("3. Check project source or documentation at https://github.com/w5277c/vm5277");
	}
}

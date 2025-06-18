/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.j8b_compiler.codegen.PlatformLoader;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.ClassNode;
import ru.vm5277.j8b_compiler.semantic.ClassScope;
import ru.vm5277.j8b_compiler.semantic.InterfaceSymbol;

public class Main {
    public	final	static	String	VERSION		= "0.0.23";
	public			static	boolean	isWindows;
	public			static	Path	toolkitPath;

	public static void main(String[] args) throws IOException, Exception {
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		toolkitPath = FSUtils.getToolkitPath();
		
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
		
		String platform = null;
		String mcu = null;
		Integer	core_freq = null;
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
					if(arg.equalsIgnoreCase("-P") || arg.equals("--path")) {
						toolkitPath = FSUtils.resolveWithEnv(args[++i]);
					}
					else if(arg.equalsIgnoreCase("-F") || arg.equals("--freq")) {
						String value = args[++i];
						try {
							core_freq = Integer.parseInt(value);
							if(0>=core_freq || 255<core_freq) {
								System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							}
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
						}
					}
				}
				else {
					System.err.println("[ERROR] Invalid parameter: " + arg);
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
		
		Path rtosPath = toolkitPath.resolve("rtos").resolve(platform).normalize();
		if(!rtosPath.toFile().exists()) {
			showInvalidRTOSDir(rtosPath);
			System.exit(0);
		}
		
		MessageContainer mc = new MessageContainer(8, true, false);
		
		Path runtimePath = toolkitPath.resolve("runtime").normalize();
		File sourceFile = new File(source);
		Path basePath = sourceFile.getParentFile().toPath();
		Lexer lexer = new Lexer(sourceFile, mc);
		
		ClassScope globalScope = new ClassScope();
		globalScope.addInterface(new InterfaceSymbol("Object"));
		
		ASTParser parser = new ASTParser(runtimePath, basePath, lexer.getTokens(), mc);
		ClassNode clazz = parser.getClazz();
		SemanticAnalyzer.analyze(globalScope, parser.getClazz());
		new ASTPrinter(parser.getClazz());

		if(0 == mc.getErrorCntr()) {
			File libDir = toolkitPath.resolve("bin").resolve("libs").normalize().toFile();
			NativeBindingsReader nbr = new NativeBindingsReader(runtimePath, mc);
			
			Map<SystemParam, Object> params = new HashMap<>();
			params.put(SystemParam.MCU, mcu);
			if(null != core_freq) {
				params.put(SystemParam.CORE_FREQ, core_freq);
			}
			CodeGenerator cg = PlatformLoader.loadGenerator(platform, libDir, nbr.getMap(), params);
			clazz.codeGen(cg);
			cg.postBuild();
			
			System.out.println();
			System.out.println(cg.getAsm());
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
		System.out.println("  -F, --freq <MHz>\tMCU clock frequency in MHz (default: platform specific)");
		System.out.println("  -P, --path <dir>\tCustom toolkit directory path");
		System.out.println("  -I, --include <dir>\tAdditional include path(s)");
		System.out.println("  -v, --version\t\tDisplay version");
		System.out.println("  -h, --help\t\tShow this help");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  j8bc avr:atmega328p main.j8b -f 8 -o firmware.hex -I ./libs");
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
	
	private static void showInvalidRTOSDir(Path platformPath) {
		System.err.println("[ERROR] Toolkit directory not found or empty");
		System.err.println();
		System.err.println("Tried to access: " + platformPath.toString());
		System.err.println();
		System.err.println("Possible solutions:");
		System.err.println("1. Specify custom path with --path (-P) option");
		System.err.println("2. Add toolkit directory(VM5277) to system environment variables");
		System.err.println("3. Check project source or documentation at https://github.com/w5277c/vm5277");
	}
}

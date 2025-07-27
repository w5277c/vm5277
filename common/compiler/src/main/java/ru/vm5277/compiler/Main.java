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
package ru.vm5277.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.compiler.codegen.PlatformLoader;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassBlockNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.MethodNode;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceSymbol;

public class Main {
    public	final	static	String	VERSION				= "0.0.23";
	public			static	boolean	isWindows;
	public			static	Path	toolkitPath;
	public			static	String	launchMethodName	= "main";
	
	public static void main(String[] args) throws IOException, Exception {
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		toolkitPath = FSUtils.getToolkitPath();
		
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01 == args.length) {
			if(args[0x00].equalsIgnoreCase("--version")) {
				System.out.println("Javl compiler version: " + VERSION);
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
		int cgVerbose = 0;
		
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
					else if(arg.equalsIgnoreCase("--cg-verbose")) {
						String value = args[++i];
						try {
							cgVerbose = Integer.parseInt(value);
							if(0>cgVerbose || 2<cgVerbose) {
								System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							}
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
						}
					}
					else {
						System.err.println("[ERROR] Invalid parameter: " + arg);
						System.exit(0);
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

		File libDir = toolkitPath.resolve("bin").resolve("libs").normalize().toFile();
		NativeBindingsReader nbr = new NativeBindingsReader(runtimePath, mc);

		Map<SystemParam, Object> params = new HashMap<>();
		params.put(SystemParam.MCU, mcu);
		if(null != core_freq) {
			params.put(SystemParam.CORE_FREQ, core_freq);
		}
		CodeGenerator cg = PlatformLoader.loadGenerator(platform, libDir, nbr.getMap(), params);
		CodeGenerator.verbose = cgVerbose;
		
		ASTParser parser = new ASTParser(runtimePath, basePath, lexer.getTokens(), mc);
		ClassNode clazz = parser.getClazz();
		SemanticAnalyzer.analyze(globalScope, parser.getClazz(), cg);
		//ReachableAnalyzer.analyze(parser.getClazz(), launchMethodName, mc);

		new ASTPrinter(parser.getClazz());
		if(0 == mc.getErrorCntr()) {
			try {
				MethodNode launchNode = null;
				ClassBlockNode classBlockNode = clazz.getBody();
				for(AstNode node : classBlockNode.getChildren()) {
					if(node instanceof MethodNode) {
						MethodNode mNode = (MethodNode)node;
						if(mNode.isPublic() && mNode.isStatic() && mNode.getParameters().isEmpty() && mNode.getName().equals(launchMethodName)) {
							launchNode = mNode;
							break;
						}
					}
				}

				if(null != launchNode) {
					launchNode.codeGen(cg, true);
					cg.build();
					System.out.println("\n" + cg.getAsm());
				}
				else {
					mc.add(new ErrorMessage("TODO Can't find launch point 'public static void main() {...'", null));
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
    }
	
	private static void showHelp() {
		System.out.println("Javl Compiler: Java-like source code compiler for vm5277 Embedded Toolkit");
		System.out.println("Version: " + VERSION + " | License: Apache-2.0");
		System.out.println("-------------------------------------------");
		System.out.println();
		System.out.println("This tool is part of the open-source project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: javlc" + (isWindows ? ".exe" : "") + " <platform>:<mcu> <input.javl> [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -o, --output <file>\tOutput HEX file (default: <input>.hex)");
		System.out.println("  -F, --freq <MHz>\tMCU clock frequency in MHz (default: platform specific)");
		System.out.println("  -P, --path <dir>\tCustom toolkit directory path");
		System.out.println("  -I, --include <dir>\tAdditional include path(s)");
		System.out.println("      --cg-verbose 0-2\t\tGenerate detailed codegen info");
		System.out.println("  -v, --version\t\tDisplay version");
		System.out.println("  -h, --help\t\tShow this help");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  javlc avr:atmega328p main.javl -f 8 -o firmware.hex -I ./libs");
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

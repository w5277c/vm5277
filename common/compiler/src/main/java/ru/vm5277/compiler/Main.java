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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import static ru.vm5277.common.AssemblerInterface.STRICT_LIGHT;
import static ru.vm5277.common.AssemblerInterface.STRICT_NONE;
import static ru.vm5277.common.AssemblerInterface.STRICT_STRONG;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.SemanticAnalyzePhase;
import ru.vm5277.common.SourceType;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.SystemParam;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.compiler.codegen.PlatformLoader;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassBlockNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.MethodNode;

public class Main {
	public	final	static	String					VERSION				= StrUtils.readVersion(Main.class);
	public			static	boolean					isWindows;
	public			static	Path					toolkitPath;
	public			static	String					launchMethodName	= "main";
	public			static	int						tabSize				= 4;
	private			static	int						stirctLevel			= STRICT_LIGHT;
	private			static	int						optLevel				= Optimization.SIZE;
	public	final	static	boolean					DEBUG_AST			= false;
	private	final	static	Map<Object, Integer>	DEBUG_AST_CNTR_MAP	= new HashMap<>();
	public					String					aaa					= new String("ssds");
	
	public static void main(String[] args) throws IOException, Exception {
		long startTimestamp = System.currentTimeMillis();
		showDisclaimer();
		
		Class<Main> aClass = Main.class;
		
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		toolkitPath = FSUtils.getToolkitPath();
		
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01 == args.length) {
			if(args[0x00].equalsIgnoreCase("--version")) {
				System.out.println("j8b compiler version: " + VERSION);
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
		boolean dumpIR = false;
		boolean asmMap = false;
		boolean asmList = false;
		
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
						continue;
					}
					else if(arg.equalsIgnoreCase("-F") || arg.equals("--freq")) {
						String value = args[++i];
						try {
							core_freq = Integer.parseInt(value);
							if(0>=core_freq || 255<core_freq) {
								System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
								break;
							}
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							break;
						}
						continue;
					}
					else if(arg.equals("-s") || arg.equals("--strict")) {
						String strictStr = args[++i];
						if(strictStr.equalsIgnoreCase("strong")) stirctLevel = STRICT_STRONG;
						else if(strictStr.equalsIgnoreCase("none")) stirctLevel = STRICT_NONE;
						else if(!strictStr.equalsIgnoreCase("light")) {
							showInvalidStrictLevel(strictStr);
							System.exit(0);
						}
						continue;
					}
					else if(arg.equals("-p") || arg.equals("--opt")) {
						String optStr = args[++i];
						if(optStr.equalsIgnoreCase("none")) optLevel = Optimization.NONE;
						else if(optStr.equalsIgnoreCase("front")) optLevel = Optimization.FRONT;
						else if(optStr.equalsIgnoreCase("speed")) optLevel = Optimization.SPEED;
						else if(!optStr.equalsIgnoreCase("size")) {
							showInvalidOptLevel(optStr);
							System.exit(0);
						}
						continue;
					}
					else if(arg.equalsIgnoreCase("--cg-verbose")) {
						String value = args[++i];
						try {
							cgVerbose = Integer.parseInt(value);
							if(0>cgVerbose || 2<cgVerbose) {
								System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
								break;
							}
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							break;
						}
						continue;
					}
				}
				
				if(arg.equals("--dump-ir")) {
					dumpIR = true;
				}
				else if(arg.equals("-t") || arg.equals("--tabsize")) {
					tabSize = Integer.parseInt(args[i]);
				}
				else if(arg.equalsIgnoreCase("-am") || arg.equals("--asm-map")) {
					asmMap = true;
				}
				else if(arg.equalsIgnoreCase("-al") || arg.equals("--ams-list")) {
					asmList = true;
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
		
		Path libPath = toolkitPath.resolve("defs").resolve(platform).normalize();
		if(!libPath.toFile().exists()) {
			//TODO showInvalidLibDir(libPath);
			System.exit(0);
		}

		MessageContainer mc = new MessageContainer(30, true, false); //TODO добавить параметр f? для изменения максимального количества выводимых ошибок
		
		Path runtimePath = toolkitPath.resolve("runtime").normalize();
		Path sourcePath = FSUtils.resolve(toolkitPath, source);
		Path basePath = sourcePath.getParent();
		
		File libDir = toolkitPath.resolve("bin").resolve("libs").normalize().toFile();
		NativeBindingsReader nbr = new NativeBindingsReader(runtimePath, mc);

		Map<SystemParam, Object> params = new HashMap<>();
		params.put(SystemParam.MCU, mcu);
		if(null != core_freq) {
			params.put(SystemParam.CORE_FREQ, core_freq);
		}
		CodeGenerator cg = PlatformLoader.loadGenerator(platform, libDir, optLevel, nbr.getMap(), params);
		CodeGenerator.verbose = cgVerbose;
		
		long timestamp = System.currentTimeMillis();
		System.out.println("Parsing " + sourcePath.toString()  + " ...");
		Lexer lexer = new Lexer(sourcePath.toFile(), mc);
		ASTParser parser = new ASTParser(runtimePath, basePath, lexer.getTokens(), mc);
		ClassNode clazz = (ClassNode)parser.getClazz();
		float time = (System.currentTimeMillis() - timestamp) / 1000f;
		System.out.println("Parsing done, time:" + String.format(Locale.US, "%.3f", time) + " s");
		timestamp = System.currentTimeMillis();
		if(null == clazz) {
			mc.add(new ErrorMessage("Main class or interface not found", null));
		}
		else {
			System.out.println("Semantic...");
			SemanticAnalyzer.analyze(clazz, cg);
			time = (System.currentTimeMillis() - timestamp) / 1000f;
			System.out.println("Semantic done, time:" + String.format(Locale.US, "%.3f", time) + " s");

			//ReachableAnalyzer.analyze(mc, (ClassNode)parser.getClazz(), cg);

			Path targetPath = basePath.resolve("target").normalize();
			targetPath.toFile().mkdirs();

			if(dumpIR) {
				try {
					Path path = targetPath.resolve(FSUtils.getBaseName(sourcePath));
					File dumpIrFile = new File(path.toString() + ".j8bir");
					dumpIrFile.setWritable(true);
					BufferedWriter dumpIrBW = new BufferedWriter(new FileWriter(dumpIrFile));
					new ASTPrinter(dumpIrBW, clazz);
					dumpIrBW.close();
					dumpIrFile.setReadOnly();
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
			}

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
						timestamp = System.currentTimeMillis();
						System.out.println("Codegen...");

						clazz.firstCodeGen(cg, launchNode);
						ReachableAnalyzer.analyze(mc, (ClassNode)parser.getClazz(), cg);
						cg.build(VarType.fromClassName(clazz.getName()), 0);
						//System.out.println("\n" + cg.getAsm());

						Map<Path, SourceType> sourcePaths	= new HashMap<>();
						sourcePaths.put(targetPath, SourceType.BASE);
						sourcePaths.put(rtosPath, SourceType.RTOS);
						sourcePaths.put(libPath, SourceType.LIB);
						Path asmPath = targetPath.resolve(FSUtils.getBaseName(sourcePath));

						File asmFile = new File(asmPath.toString() + ".asm");
						asmFile.createNewFile();
						FileWriter fw = new FileWriter(asmFile);
						fw.write(cg.getAsm());
						fw.close();

						File asmMapFile = null;
						if(asmMap) {
							asmMapFile = new File(asmPath.toString() + ".map");
						}

						BufferedWriter listBw = null;
						if(asmList) {
							listBw = new BufferedWriter(new FileWriter(new File(asmPath.toString() + ".lst")));
						}

						time = (System.currentTimeMillis() - timestamp) / 1000f;
						System.out.println("Codegen done, time:" + String.format(Locale.US, "%.3f", time) + " s");

						timestamp = System.currentTimeMillis();
						System.out.println("Assembling...");
						PlatformLoader.launchAssembler(platform, libDir, mc, asmFile.toPath(), sourcePaths, asmPath.toString(), asmMapFile, listBw);
						time = (System.currentTimeMillis() - timestamp) / 1000f;
						System.out.println("Assembling done, time:" + String.format(Locale.US, "%.3f", time) + " s");

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
		time = (System.currentTimeMillis() - startTimestamp) / 1000f;
		System.out.println("Total time:" + String.format(Locale.US, "%.3f", time) + " s");

    }
	
	private static void showDisclaimer() {
		System.out.println("Version: " + VERSION + " | License: Apache-2.0");
		System.out.println("================================================================");
		System.out.println("WARNING: This project is under active development.");
		System.out.println("The primary focus is on functionality; testing is currently limited.");
		System.out.println("It is strongly recommended to carefully review the generated");
		System.out.println("assembler code before flashing the device.");
		System.out.println("Please report any bugs found to: konstantin@5277.ru");
		System.out.println("================================================================");
	}
	
	private static void showHelp() {
		System.out.println("j8b compiler: Java-like source code compiler for vm5277 Embedded Toolkit");
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
		System.out.println("  -o,  --output <file>  Output HEX file (default: <input>.hex)");
		System.out.println("  -F,  --freq <MHz>     MCU clock frequency in MHz (default: platform specific)");
		System.out.println("  -P,  --path <dir>     Custom toolkit directory path");
		System.out.println("  -I,  --include <dir>  Additional include path(s)");
		System.out.println("  -t,  --tabsize <num>  Tab size in spaces (default: 4)");
		System.out.println("       --dump-ir        Output intermediate representation after frontend processing");
		System.out.println("       --cg-verbose 0-2 Generate detailed codegen info");
		System.out.println("  -s,  --strict         <strong|light|none> Ambiguity handling level");
		System.out.println("                        strong  - Treat as error");
		System.out.println("                        light   - Show warning (default)");
		System.out.println("                        none    - Silent mode");
		System.out.println("  -p,  --opt            <none|front|size|speed> Optimization high language(j8b) level");
		System.out.println("                        none    - No optimization");
		System.out.println("                        front   - Frontend optimization only");
		System.out.println("                        size    - Size(Flash) optimization (default)");
		System.out.println("                        speed   - Execution speed optimization");
		System.out.println("  -ao, --asm-output     <file> Output HEX file (default: <input>.hex)"); //TODO
		System.out.println("  -af, --asm-format     <fmt>  Output format (hex, bin)"); //TODO
		System.out.println("                        hex     - Intel HEX (default)");
		System.out.println("                        bin     - Raw binary");
		System.out.println("  -am, --asm-map        Generate asm memory map file");
		System.out.println("  -al, --asm-list       Generate asm listing file");
		System.out.println("  -v,  --version        Display version");
		System.out.println("  -h,  --help           Show this help");
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
		System.err.println("2. Add toolkit directory(vm5277) to system environment variables");
		System.err.println("3. Check project source or documentation at https://github.com/w5277c/vm5277");
	}
	
	private static void showInvalidStrictLevel(String invalidParam) {
		System.err.println("[ERROR] TODO Invalid strict(-s) level:" + invalidParam + ", expected error|warning|ignore");
	}

	private static void showInvalidOptLevel(String invalidParam) {
		System.err.println("[ERROR] TODO Invalid optimization level:" + invalidParam + ", expected none|size|speed");
	}

	public static int getStrictLevel() {
		return stirctLevel;
	}
	
	public static int getOptLevel() {
		return optLevel;
	}
	
	public static void debugAST(Object obj, SemanticAnalyzePhase phase, boolean isIn, String info) {
		debugAST(obj, phase, isIn, false, info);
	}
	public static void debugAST(Object obj, SemanticAnalyzePhase phase, boolean isIn, boolean result, String info) {
		if(DEBUG_AST) {
			Integer cntr = null;
			if(isIn && SemanticAnalyzePhase.POST==phase) {
				cntr = DEBUG_AST_CNTR_MAP.get(obj);
				if(null==cntr) cntr = 0;
				cntr++;
				DEBUG_AST_CNTR_MAP.put(obj, cntr);
			}
			
			System.out.println(	"ASTDEBUG " + (null==obj ? "" : String.format("%8X", obj.hashCode())) + "\t" + phase + "\t" +
								(isIn ? "I " + (null!=cntr ? "x" + cntr : "") : "O " + result) + "\t" + info);
		}
	}
}

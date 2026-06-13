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

import ru.vm5277.common.lexer.Lexer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import ru.vm5277.common.Device;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.enums.SemanticAnalyzePhase;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.enums.RTOSParam;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.RTOSHaltMode;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.compiler.codegen.PlatformLoader;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassBlockNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.MethodNode;
import ru.vm5277.common.lexer.LexerType;

public class Main {
	public	final	static	String					VERSION				= StrUtils.readVersion(Main.class);
	public			static	boolean					isWindows;
	public			static	Path					toolkitPath;
	public			static	String					launchMethodName	= "main";
	private			static	StrictLevel				strictLevel			= StrictLevel.LIGHT;
	private			static	OptimizationType		optimizationType	= OptimizationType.SIZE;
	public	final	static	boolean					DEBUG_AST			= false;
	public	final	static	boolean					DEBUG_CGSCOPE		= false;
	private	final	static	Map<Object, Integer>	DEBUG_AST_CNTR_MAP	= new HashMap<>();
	private			static	boolean					quiet				= false;
	private			static	Device					device;
	private			static	CodeGenerator			cg					= null;
	private			static	CGScope					parentCGScope		= new CGScope();
	
	public static void main(String[] args) {
		System.exit(exec(args));
	}
	
	public static int exec(String[] args) {
		long startTimestamp = System.currentTimeMillis();
		
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		toolkitPath = FSUtils.getToolkitPath();
		
		if(0x00==args.length) {
			showHelp();
			return 0;
		}
		if(0x01 == args.length) {
			if(args[0x00].equals("-v") || args[0x00].equals("--version")) {
				System.out.println("j8b compiler version: " + VERSION);
			}
			else if(args[0].equals("h") || args[0x00].equals("--help")){
				showHelp();
			}
			else if(args[0x00].equals("--help-params")){
				showPlatformParams();
			}
			else {
				showHelp();
			}
			return 0;
		}
		
		PlatformType platformType = null;
		String mcu = null;
		Integer freq = null;
		String source = null;
		Path outputPath = null;
		int cgVerbose = 0;
		int maxErrors = 30;
		boolean dumpIR = false;
		int tabSize = 0x04;
		Map<RTOSParam, Object> params = new HashMap<>();
		
		if(0x02 <= args.length) {
			String[] parts = args[0x00].split(":");
			if(0x02 == parts.length) {
				platformType = PlatformType.fromName(parts[0]);
				mcu = parts[1];
			}
			source = args[1];
			
			for(int i=2; i<args.length; i++) {
				String arg = args[i];
				if(arg.equals("--dump-ir")) {
					dumpIR = true;
					continue;
				}
				if(arg.equals("-q") || arg.equals("--quiet")) {
					quiet = true;
					continue;
				}

				if(args.length > i+1) {
					if(arg.equals("-P") || arg.equals("--path")) {
						toolkitPath = FSUtils.resolveWithEnv(args[++i]);
						continue;
					}
					else if(arg.equals("-o") || arg.equals("--output")) {
						outputPath = FSUtils.resolveWithEnv(args[++i]);
						continue;
					}
					else if(arg.equals("-F") || arg.equals("--freq")) {
						String value = args[++i];
						try {
							freq = Math.round(Float.parseFloat(value)*1000);
							if(0>=freq || 1024000<freq) {
								System.err.println("[ERROR] Invalid freq parameter: " + value);
								return 1;
							}
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid freq parameter:"  + value);
							return 1;
						}
						continue;
					}
					else if(arg.equals("-D") || arg.equals("--define")) {
						parts = args[++i].split("=", 2);
						String name = parts[0x00].trim().toUpperCase();
						RTOSParam param = RTOSParam.valueOf(name);
						if(null==param) {
							System.err.println("[ERROR] Unlnown RTOSParam.name: " + name);
							return 1;
						}
						
						if(parts[0x01].startsWith("0x")) {
							try {
								int value = Integer.parseInt(parts[0x01].substring(0x02), 0x10);
								params.put(param, value);
							}
							catch(Exception ex) {
								params.put(param, parts[0x01]);
							}
						}
						else {
							try {
								int value = Integer.parseInt(parts[0x01]);
								params.put(param, value);
							}
							catch(Exception ex) {
								params.put(param, parts[0x01]);
							}
						}
						continue;
					}
					else if(arg.equals("-s") || arg.equals("--strict")) {
						String strictStr = args[++i];
						strictLevel = StrictLevel.fromName(strictStr);
						if(null==strictLevel) {
							showInvalidStrictLevel(strictStr);
							return 1;
						}
						continue;
					}
					else if(arg.equals("-p") || arg.equals("--opt")) {
						String optStr = args[++i];
						optimizationType = OptimizationType.fromName(optStr);
						if(null==optimizationType) {
							showInvalidOptLevel(optStr);
							return 1;
						}
						continue;
					}
					else if(arg.equals("--cg-verbose")) {
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
					else if(arg.equals("--max-errors")) {
						String value = args[++i];
						try {
							maxErrors = Integer.parseInt(value);
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							break;
						}
						continue;
					}
					else if(arg.equals("-t") || arg.equals("--tabsize")) {
						String value = args[++i];
						try {
							tabSize = Integer.parseInt(value);
						}
						catch(Exception e) {
							System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
							break;
						}
						continue;
					}
				}
				
				System.err.println("[ERROR] Invalid parameter: " + arg);
				return 1;
			}
		}
		else {
			showHelp();
			return 0;
		}
 
		boolean isAsmView = !source.toLowerCase().endsWith(".j8b");
		
		if(!quiet & !isAsmView) showDisclaimer();
		
		if(null==toolkitPath) {
			showInvalidToolkitDir(null);
			return 1;
		}
		if(!quiet & !isAsmView) System.out.println("Toolkit path: " + toolkitPath.toString());
		
		if(null == platformType || null == mcu || mcu.isEmpty()) {
			showInvalidDeviceFormat(args[0]);
			return 1;
		}
		
		MessageContainer mc = new MessageContainer(maxErrors, true, false);
		
		Path runtimePath = toolkitPath.resolve("runtime").normalize();
		File libDir = toolkitPath.resolve("bin").resolve("libs").normalize().toFile();

		params.put(RTOSParam.MCU, mcu);
		if(null!=freq) {
			params.put(RTOSParam.CORE_FREQ, freq);
		}
		
		Path rtosPath = toolkitPath.resolve("rtos").resolve(platformType.name().toLowerCase()).normalize();
		try {
			device = new Device(toolkitPath, rtosPath, platformType, mcu, params, optimizationType, strictLevel);
			cg = PlatformLoader.loadGenerator(device, libDir);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return 1;
		}
		CodeGenerator.verbose = cgVerbose;
		
		Path sourcePath = (isAsmView ? null : FSUtils.resolve(toolkitPath, source));
		Path projectPath = (null==sourcePath ? null : sourcePath.getParent());

		long timestamp = System.currentTimeMillis();
		if(!quiet && !isAsmView) System.out.println("Parsing " + (null==sourcePath ? "" : sourcePath.toString())  + " ...");
		Lexer lexer = null;
		try {
			if(isAsmView) {
				lexer = new Lexer(LexerType.J8B, source, false, tabSize);
			}
			else {
				lexer = new Lexer(LexerType.J8B, sourcePath.toFile(), null, tabSize, false);
			}
		}
		catch(FileNotFoundException ex) {
			mc.add(new ErrorMessage(ex.getMessage(), null));
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return 1;
		}

		boolean result = false;
		if(null!=lexer) {
			Instance inst = new Instance(toolkitPath, runtimePath, mc, tabSize);
			ASTParser parser = null;
			try {
				parser = new ASTParser(inst, projectPath, lexer.getTokens(), true, isAsmView);
			}
			catch(Exception ex) {
				ex.printStackTrace();
				return 1;
			}

			ClassNode clazz = (ClassNode)parser.getClazz();
			float time = (System.currentTimeMillis() - timestamp) / 1000f;
			if(!quiet && !isAsmView) System.out.println("Parsing done, time:" + String.format(Locale.US, "%.3f", time) + " s");
			timestamp = System.currentTimeMillis();
			if(null==clazz) {
				mc.add(new ErrorMessage("Main class or interface not found", null));
			}
			else {
				if(!quiet && !isAsmView) System.out.println("Semantic...");
				SemanticAnalyzer.analyze(clazz, cg, parentCGScope, parser.getAutoImported());
				time = (System.currentTimeMillis() - timestamp) / 1000f;
				if(!quiet && !isAsmView) System.out.println("Semantic done, time:" + String.format(Locale.US, "%.3f", time) + " s");

				//ReachableAnalyzer.analyze(mc, (ClassNode)parser.getClazz(), cg);

				String sourceName = null;

				if(!isAsmView) {
					sourceName = (isAsmView ? null : sourcePath.getFileName().toString());
					sourceName = sourceName.substring(0, sourceName.lastIndexOf("."));

					if(null==outputPath) {
						outputPath = projectPath.resolve("target").normalize();
					}
					if(!outputPath.toFile().exists()) {
						outputPath.toFile().mkdirs();
					}

					if(dumpIR) {
						try {
							File dumpIrFile = outputPath.resolve(sourceName + ".ir.j8b").normalize().toFile();
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
							if(!quiet && !isAsmView) System.out.println("Codegen...");

							CGExcs excs = new CGExcs();
							clazz.firstCodeGen(cg, launchNode, excs);
							ReachableAnalyzer.analyze(mc, (ClassNode)parser.getClazz(), cg, excs);
							cg.build(VarType.fromClassName(clazz.getName()), 0);
							//System.out.println("\n" + cg.getAsm());

							if(!isAsmView) {
								File asmFile = outputPath.resolve(sourceName + ".asm").normalize().toFile();
								asmFile.createNewFile();
								FileWriter fw = new FileWriter(asmFile);
								fw.write(cg.getAsm(parentCGScope, device.getDefsPath(), mc));
								fw.close();

								if(DEBUG_CGSCOPE) {
									cg.showScopeTree(parentCGScope);
								}

								//TODO обозвать fwreport.txt - будет хранить все тех. описание сгенерированной прошивки (ресурсы, метки, типы, исключения,
								//выделенные пулы памяти, вероятный размер испольуемых ресурсов, включенные фичи и т.п.
								File dbgFile = outputPath.resolve(sourceName + ".dbg").normalize().toFile();
								dbgFile.createNewFile();
								fw = new FileWriter(dbgFile);
								cg.getTargetInfoBuilder().write(sourcePath, fw);
							}
							else {
								System.out.println(cg.getAsm(parentCGScope, device.getDefsPath(), mc));
							}

							time = (System.currentTimeMillis() - timestamp) / 1000f;
							if(!quiet && !isAsmView) System.out.println("Codegen done, time:" + String.format(Locale.US, "%.3f", time) + " s");
							result = true;
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
		}

		if(result) {
			if(!isAsmView) System.out.println("Compile SUCCESS, warnings:" + mc.getWarningCntr());
		
			float time = (System.currentTimeMillis() - startTimestamp) / 1000f;
			if(!quiet && !isAsmView) System.out.println("Total time:" + String.format(Locale.US, "%.3f", time) + " s\n");
		}
		else {
			System.out.println("Compile FAIL, warnings:" + mc.getWarningCntr() + ", errors:" + mc.getErrorCntr() + "/" + mc.getMaxErrorQnt());
		}

		return result ? 0x00 : 0x01;
    }
	
	private static void showDisclaimer() {
		System.out.println("j8b compiler v." + VERSION + ": Java-like source code compiler for vm5277 Embedded Toolkit");
	}
	
	private static void showHelp() {
		System.out.println("j8b compiler: Java-like source code compiler for vm5277 Embedded Toolkit");
		System.out.println("Version: " + VERSION + " | License: Apache-2.0");
		System.out.println("-------------------------------------------");
		System.out.println("This tool is part of the open-source project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277 | Official site: https://vm5277.ru");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: j8bc" + (isWindows ? ".exe" : "") + " <platform>:<mcu> <input.j8b> [options]");
		System.out.println("       j8bc" + (isWindows ? ".exe" : "") + " <platform>:<mcu> \"<code>\" [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -o,  --output <dir>	   Output directory");
		System.out.println("  -F,  --freq <MHz>        MCU clock frequency in MHz (default: platform specific)");
		System.out.println("  -P,  --path <dir>        Custom toolkit directory path");
		System.out.println("  -I,  --include <dir>     Additional include path(s)");
		System.out.println("  -D,  --define name=value Define RTOSParam.name=value");
		System.out.println("  -t,  --tabsize <num>     Tab size in spaces (default: 4)");
		System.out.println("       --dump-ir           Output intermediate representation after frontend processing");
		System.out.println("       --cg-verbose 0-2    Generate detailed codegen info");
		System.out.println("  -s,  --strict            <strong|light|none> Ambiguity handling level");
		System.out.println("                           strong  - Treat as error");
		System.out.println("                           light   - Show warning (default)");
		System.out.println("                           none    - Silent mode");
		System.out.println("  -p,  --opt               <none|front|size|speed> Optimization high language (j8b) level");
		System.out.println("                           none    - No optimization");
		System.out.println("                           front   - Frontend optimization only");
		System.out.println("                           size    - Size(Flash) optimization (default)");
		System.out.println("                           speed   - Execution speed optimization");
		System.out.println("       --max-errors <num>  Maximum errors before abort (default: 30)");
		System.out.println();
		System.out.println("  -q,  --quiet             Quiet mode (minimal output)");
		System.out.println("  -v,  --version           Display version");
		System.out.println("  -h,  --help              Show this help");
		System.out.println("       --help-params       Show RTOS parameters for target platform");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  j8bc avr:atmega328p main.j8b -F 8 -I ./libs");
		System.out.println("  j8bc avr:atmega328p \"System.out(\\\"Hi there\\\");\" -F 16 -D STDIO_PORT=pd0/pd1");
		System.out.println("  j8bc avr:atmega328p \"class Main{ public static void main() {for(byte i=0; i<5; i++) { System.out(i); }}}\"");
	}
	
	private static void showPlatformParams() {
		for(RTOSParam param : RTOSParam.values()) {
			if(!param.isHidden()) {
				if(RTOSParam.HALT_OK_MODE==param || RTOSParam.HALT_ERR_MODE==param) {
					System.out.println(param.toString() + ": " + StrUtils.toString(RTOSHaltMode.values()));
				}
				else {
					System.out.println(param.toString());
				}
			}
		}
	}
	
	private static void showInvalidDeviceFormat(String invalidParam) {
		System.err.println("[ERROR] Invalid device format. Expected: <platform>:<mcu>");
		System.err.println("Examples:");
		System.err.println("  avr:atmega328p");
		System.err.println("  stm8:stm8s003f3");
		System.err.println("  pic:16f877a");
		System.err.println("Detected: '" + invalidParam + "'");
		System.err.println("Supported MCUs are listed in the rtos/<platform>/devices/ directory of the project");
	}
	
	private static void showInvalidToolkitDir(Path toolkitPath) {
		System.err.println("[ERROR] Toolkit directory not found or empty:" + toolkitPath.toString());
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
		System.err.println("[ERROR] TODO Invalid optimization level:" + invalidParam + ", expected none|front|size|speed");
	}

	public static StrictLevel getStrictLevel() {
		return strictLevel;
	}
	
	public static OptimizationType getOptimizationType() {
		return optimizationType;
	}
	public static void setOptimizationType(OptimizationType _optimizationType) {
		optimizationType = _optimizationType;
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
	
	public static void showCGScopeTree() {
		if(null!=cg) {
			cg.showScopeTree(parentCGScope);
		}
	}
}

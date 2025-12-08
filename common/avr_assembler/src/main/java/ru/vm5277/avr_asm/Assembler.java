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

/*
 * TODO:
 * Добавить директивы .REPT и .ENDR, повторяет блок N раз
 * Проверить ADIW/SBIW rXX,const, rXX может быть только >=24
 */

package ru.vm5277.avr_asm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.vm5277.avr_asm.nodes.DataNode;
import ru.vm5277.avr_asm.nodes.MacroNode;
import ru.vm5277.avr_asm.nodes.MnemNode;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.common.SourceType;
import ru.vm5277.avr_asm.output.IntelHexBuilder;
import ru.vm5277.avr_asm.scope.MacroCallSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.scope.VariableSymbol;
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class Assembler implements AssemblerInterface {
	public	final	static	String	VERSION		= StrUtils.readVersion(Assembler.class);
	public			static	int		tabSize		= 4;
	public			static	boolean	isWindows;
	public			static	Path	toolkitPath;
	
	public static void main(String[] args) throws IOException, Exception {
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));

		toolkitPath = FSUtils.getToolkitPath();
		
		String source = null;
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01 == args.length) {
			if(args[0x00].equals("--version")) {
				System.out.println("AVR assembler version: " + VERSION);
				System.exit(0);
			}
		}

		String mcu = null;
		Map<Path, SourceType> sourcePaths	= new HashMap<>();
		int stirctLevel = STRICT_LIGHT;
		int maxErrors = 30;
		String format = "hex";
		String mapFileName = null;
		String listFileName = null;
		String outputFileName = null;
		Map<String, Long> defines = new HashMap<>();
		List<String> includes = new ArrayList<>();
		
		source = args[0x00];
		for(int i=1; i<args.length; i++) {
			String arg = args[i++];
			if(args.length > i) {
				if(arg.equals("-P") || arg.equals("--path")) {
					toolkitPath = FSUtils.resolveWithEnv(args[i]);
				}
				else if(arg.equals("-I") || arg.equals("--include")) {
					if(null==toolkitPath) {
						showInvalidToolkitDir(toolkitPath, null);
						System.exit(1);
					}
					sourcePaths.put(FSUtils.resolve(toolkitPath, args[i]), SourceType.LIB);
				}
				else if(arg.equals("-t") || arg.equals("--tabsize")) {
					tabSize = Integer.parseInt(args[i]);
				}
				else if(arg.equals("--max-errors")) {
					String value = args[++i];
					try {
						maxErrors = Integer.parseInt(value);
					}
					catch(Exception e) {
						System.err.println("[ERROR] Invalid parameter " + arg + " value: " + value);
						System.exit(1);
					}
				}
				else if(arg.equals("-d") || arg.equals("--device")) {
					mcu = args[i];
					includes.add(mcu + ".inc");
				}
				else if(arg.equals("-D") || arg.equals("--define")) {
					String[] parts = args[i].split("=", 2);
					String name = parts[0x00].trim().toLowerCase();
					if(defines.containsKey(name.toLowerCase())) {
						System.err.println("[ERROR] define '" + name + "' already defined");
						System.exit(1);
					}
					long value = 1l;
					if(0x02==parts.length) {
						String valueStr = parts[0x01].trim().toLowerCase();
						if(valueStr.isEmpty()) {
							System.err.println("[ERROR] define '" + name + "' value is empty");
							System.exit(1);
						}
						try {
							value = Long.parseLong(valueStr);
						}
						catch(Exception ex) {
							System.err.println("[ERROR] define '" + name + "' value is not integral");
							System.exit(1);
						}
					}
					defines.put(name, value);
				}
				else if(arg.equals("-i") || arg.equals("--include")) {
					includes.add(args[i]);
				}
				else if(arg.equals("-m") || arg.equals("--map")) {
					mapFileName = args[i];
				}
				else if(arg.equals("-l") || arg.equals("--list")) {
					listFileName = args[i];
				}
				else if(arg.equals("-o") || arg.equals("--output")) {
					outputFileName = args[i];
				}
				else if(arg.equals("-s") || arg.equals("--strict")) {
					String strictStr = args[i];
					if(strictStr.equals("strong")) stirctLevel = STRICT_STRONG;
					else if(strictStr.equals("none")) stirctLevel = STRICT_NONE;
					else if(!strictStr.equals("light")) {
						showInvalidStrictLevel(strictStr);
						System.exit(1);
					}
				}
				else if(arg.equals("-f") || arg.equals("--format")) {
					format = args[i].toLowerCase();
					if(!format.equals("hex") && !format.equals("bin")) {
						showInvalidStrictLevel("-f " + format);
						System.exit(1);
					}
				}
				else {
					System.err.println("[ERROR] Invalid parameter:" + arg + "\n");
					showHelp();
					System.exit(1);
				}
			}
		}
 
		if(null==toolkitPath) {
			showInvalidToolkitDir(toolkitPath, null);
			System.exit(1);
		}
		System.out.println("Toolkit path: " + toolkitPath.toString());
		
		sourcePaths.put(FSUtils.resolve(toolkitPath, "defs/avr/"), SourceType.LIB);

		MessageContainer mc = new MessageContainer(maxErrors, true, false);
		
		Path sourcePath = FSUtils.resolve(toolkitPath, source);
		Path baseDir = sourcePath.getParent();
		sourcePaths.put(baseDir, SourceType.BASE);

		File mapFile = null;
		if(null != mapFileName) {
			Path path = FSUtils.resolve(baseDir, mapFileName);
			mapFile = (1<path.getNameCount() ? path.toFile() : baseDir.resolve(path).normalize().toFile());
		}
		
		BufferedWriter listWriter = null;
		if(null != listFileName) {
			Path path = FSUtils.resolve(baseDir, listFileName);
			File listFile = (1<path.getNameCount() ? path.toFile() : baseDir.resolve(path).normalize().toFile());
			try {listWriter = new BufferedWriter(new FileWriter(listFile));}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(null == outputFileName) outputFileName = FSUtils.getBaseName(sourcePath);
		if(1==Paths.get(outputFileName).getNameCount()) outputFileName = baseDir.resolve(outputFileName).normalize().toString();
		
		long timestamp = System.currentTimeMillis();
		System.out.println("Parsing " + sourcePath.toString()  + " ...");
		boolean success = new Assembler().exec(mc, mcu, sourcePath, sourcePaths, stirctLevel, outputFileName, mapFile, listWriter, defines, includes);
		float time = (System.currentTimeMillis() - timestamp) / 1000f;
		System.out.println("Total time: " + String.format(Locale.US, "%.3f", time) + " s");
		System.exit(success ? 0 : 1);
	}

	@Override
	public boolean exec(MessageContainer mc, String mcu, Path sourcePath, Map<Path, SourceType> sourcePaths, int stirctLevel,
						String outputFileName, File mapFile, BufferedWriter listWriter, Map<String, Long> defines, List<String> includes) throws Exception {
		
		if(null == toolkitPath) {
			toolkitPath = FSUtils.getToolkitPath();
		}
		
		Path instrPath = toolkitPath.resolve("defs").resolve("avr");
		InstrReader instrReader = new InstrReader(instrPath, mc);
		Scope scope = new Scope(sourcePath.toFile(), instrReader, listWriter);
		Scope.setStrictLevel(stirctLevel);
		
		if(null!=defines) {
			for(String defName : defines.keySet()) {
				scope.setVariable(new VariableSymbol(defName, defines.get(defName), true), null);
			}
		}
		
		Lexer lexer = new Lexer(sourcePath.toFile(), scope, mc);
		//for(Token token : lexer.getTokens()) {
			//System.out.print(token.toString());
		//}
		try {
			Parser parser = new Parser(lexer.getTokens(), scope, mc, sourcePaths, tabSize, includes);
			secondPass(scope, mc, parser.getSecondPassNodes());
		}
		catch(Exception e) {
			mc.add(new ErrorMessage(e.getMessage(), null));
		}
		
		try {scope.leaveImport(null);} //TODO не проверяю на MessageContainerIsFullException
		catch(CompileException e) {mc.add(e.getErrorMessage());}
		catch(CriticalParseException e) {mc.add(e.getErrorMessage()); throw e;}

		if(null != mapFile) try {scope.makeMap(mapFile);} catch(Exception e) {e.printStackTrace();}
		if(null != listWriter) try {listWriter.close();} catch(Exception e) {e.printStackTrace();}
		
		if(0 != mc.getErrorCntr()) {
			System.out.println("\nBuild FAIL, warnings:" + mc.getWarningCntr() + ", errors:" + mc.getErrorCntr() + "/" + mc.getMaxErrorQnt());
			return false;
		}
		else {
			System.out.println();
			if(!scope.getCSeg().isEmpty()) {
				IntelHexBuilder hexBuilder = new IntelHexBuilder(new File(outputFileName + "_cseg.hex"));
				scope.getCSeg().build(hexBuilder);
				hexBuilder.close();
				scope.getCSeg().printStat();
			}

			System.out.println("\nparsed: " + mc.getLineQnt() + " lines");
			System.out.println("Build SUCCESS, warnings:" + mc.getWarningCntr());
			return true;
		}
	}
	
	
	private static void secondPass(Scope scope, MessageContainer mc, List<Node> nodes) {
		for(Node node : nodes) {
			if(node instanceof MnemNode) {
				((MnemNode)node).secondPass();
			}
			else if(node instanceof MacroNode) {
				MacroCallSymbol callSymbol = ((MacroNode)node).getCallSymbol();
				scope.beginMacroSecondPass(callSymbol);
				secondPass(scope, mc, callSymbol.getSecondPartNodes());
				scope.endMacroSecondPass();
			}
			else if(node instanceof DataNode) {
				((DataNode)node).secondPass();
			}
			else {
				mc.add(new ErrorMessage("Unexpected node type, expected MnemNode or MacroNode, got: " + node, null));
			}
		}
	}
	
	private static void showHelp() {
		System.out.println("AVR assembler for vm5277 Embedded Toolkit");
		System.out.println("Version: " + VERSION + " | License: Apache-2.0");
		System.out.println("-------------------------------------------");
		System.out.println();
		System.out.println("This tool is part of the open-source project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: avrasm" + (isWindows ? ".exe" : "") + " <input.asm> [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -o, --output <file>   Output HEX file (default: <input>.hex)");
		System.out.println("  -i, --include <file>  Include specific file before processing");
		System.out.println("  -f, --format <fmt>    Output format (hex, bin)");
		System.out.println("                          hex     - Intel HEX (default)");
		System.out.println("                          bin     - Raw binary");
		System.out.println("  -d, --device <mcu>    Target MCU (e.g. atmega328p)");
		System.out.println("  -P,  --path <dir>     Custom toolkit directory path");
		System.out.println("  -I, --include <dir>   Additional include path(s)");
		System.out.println("  -t, --tabsize <num>   Tab size in spaces (default: 4)");
		System.out.println("  -s, --strict <strong|light|none> Ambiguity handling level");
		System.out.println("                          strong  - Treat as error");
		System.out.println("                          light   - Show warning (default)");
		System.out.println("                          none    - Silent mode");
		System.out.println("  -m, --map <file>      Generate memory map file");
		System.out.println("  -l, --list <file>     Generate assembly listing file");
		System.out.println("  -D, --define <symbol>[=<value>]] Define symbol (.equ direcive");
		System.out.println();
		System.out.println("  -v, --version         Display version");
		System.out.println("  -h, --help            Show this help");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  avrasm main.asm -d atmega328p -o firmware.hex -I ./libs");
	}
	
	private static void showInvalidStrictLevel(String invalidParam) {
		System.err.println("[ERROR] TODO Invalid strict(-s) level:" + invalidParam + ", expected error|warning|ignore");
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
	
	private static void showInvalidToolkitDir(Path toolkitPath, Path subPath) {
		System.err.println("[ERROR] Toolkit directory not found or empty");
		System.err.println();
		if(null!=subPath) {
			System.err.println("Tried to access: " + subPath.toString());
			System.err.println();
		}
		System.err.println("Possible solutions:");
		System.err.println("1. Specify custom path with --path (-P) option");
		System.err.println("2. Add toolkit directory(vm5277) to system environment variables");
		System.err.println("3. Check project source or documentation at https://github.com/w5277c/vm5277");
	}
}

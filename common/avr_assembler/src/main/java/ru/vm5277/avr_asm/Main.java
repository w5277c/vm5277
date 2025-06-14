/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
29.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.vm5277.avr_asm.nodes.MacroNode;
import ru.vm5277.avr_asm.nodes.MnemNode;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.nodes.SourceType;
import ru.vm5277.avr_asm.output.IntelHexBuilder;
import ru.vm5277.avr_asm.scope.MacroCallSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class Main {
	public	final	static	String	VERSION		= "0.1.0";
	public			static	int		tabSize		= 4;
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
		
		String source = null;
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01 == args.length) {
			if(args[0x00].equalsIgnoreCase("--version")) {
				System.out.println("AVR assembler version: " + VERSION);
				System.exit(0);
			}
		}

		String mcu = null;
		Map<String, SourceType> sourcePaths	= new HashMap<>();
		int stirctLevel = Scope.STRICT_WARNING;
		String format = "hex";
		String mapFileName = null;
		String listFileName = null;
		String outputFileName = null;
		
		source = args[0x00];
		for(int i=1; i<args.length; i++) {
			String arg = args[i++];
			if(args.length > i) {
				if(arg.equals("-I") || arg.equals("--include")) {
					sourcePaths.put(args[i], SourceType.LIB);
				}
				else if(arg.equals("-t") || arg.equals("--tabsize")) {
					tabSize = Integer.parseInt(args[i]);
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
					if(strictStr.equalsIgnoreCase("error")) stirctLevel = Scope.STRICT_ERROR;
					else if(strictStr.equalsIgnoreCase("ignore")) stirctLevel = Scope.STRICT_IGNORE;
					else if(!strictStr.equalsIgnoreCase("warning")) {
						showInvalidStrictLevel(strictStr);
						System.exit(0);
					}
				}
				else if(arg.equals("-f") || arg.equals("--format")) {
					format = args[i].toLowerCase();
					if(!format.equals("hex") && !format.equals("bin")) {
						showInvalidStrictLevel("-f " + format);
						System.exit(0);
					}
				}
				else {
					System.err.println("[ERROR] TODO invalid parameter:" + arg + "\n");
					showHelp();
					System.exit(0);
				}
			}
		}
 
		MessageContainer mc = new MessageContainer(16, true, false);
		
		String rtosPath = toolkitPath + File.separator + "platform" + File.separator + "avr" + File.separator + "rtos";
		sourcePaths.put(rtosPath, SourceType.RTOS);
		File sourceFile = new File(source);
		String baseDir = sourceFile.getParentFile().getAbsolutePath();
		sourcePaths.put(baseDir, SourceType.BASE);

		File mapFile = null;
		if(null != mapFileName) {
			mapFile = (1<Paths.get(mapFileName).getNameCount()) ? new File(mapFileName) : new File(baseDir, mapFileName);
		}
		
		BufferedWriter listWriter = null;
		if(null != listFileName) {
			File listFile = (1<Paths.get(listFileName).getNameCount()) ? new File(listFileName) : new File(baseDir, listFileName);
			try {listWriter = new BufferedWriter(new FileWriter(listFile));}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(null == outputFileName) outputFileName = sourceFile.getName();
		if(1==Paths.get(outputFileName).getNameCount()) outputFileName = baseDir + File.separator + outputFileName;
		
		long timestamp = System.currentTimeMillis();
		InstrReader instrReader = new InstrReader("./", mc);
		Scope scope = new Scope(sourceFile, instrReader, listWriter);
		Scope.setStrictLevel(stirctLevel);
		if(null != mcu) scope.setDevice(mcu);
		
		Lexer lexer = new Lexer(sourceFile, scope, mc);
		//for(Token token : lexer.getTokens()) {
			//System.out.print(token.toString());
		//}
		try {
			Parser parser = new Parser(lexer.getTokens(), scope, mc, sourcePaths, tabSize);
			secondPass(scope, mc, parser.getSecondPassNodes());
		}
		catch(Exception e) {
			mc.add(new ErrorMessage(e.getMessage(), null));
		}
		
		try {scope.leaveImport();} //TODO не проверяю на MessageContainerIsFullException
		catch(ParseException e) {mc.add(e.getErrorMessage());}
		catch(CriticalParseException e) {mc.add(e.getErrorMessage()); throw e;}

		if(null != mapFile) try {scope.makeMap(mapFile);} catch(Exception e) {e.printStackTrace();}
		if(null != listWriter) try {listWriter.close();} catch(Exception e) {e.printStackTrace();}
		
		
		if(0 != mc.getErrorCntr()) {
			System.out.println("\nBuild FAIL, warnings:" + mc.getWarningCntr() + ", errors:" + mc.getErrorCntr() + "/" + mc.getMaxErrorQnt());
			System.exit(1);
		}
		else {
			System.out.println();
			if(!scope.getCSeg().isEmpty()) {
				IntelHexBuilder hexBuilder = new IntelHexBuilder(new File(outputFileName + "_cseg.hex"));
				scope.getCSeg().build(hexBuilder);
				hexBuilder.close();
				scope.getCSeg().printStat();
			}

			float time = (System.currentTimeMillis() - timestamp) / 1000f;
			System.out.println("\nparsed: " + mc.getLineQnt() + " lines, total time: " + String.format(Locale.US, "%.2f", time) + " s");
			System.out.println("Build SUCCESS, warnings:" + mc.getWarningCntr());
			System.exit(0);
		}

		System.out.println(System.currentTimeMillis()-timestamp);
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
			else {
				mc.add(new ErrorMessage("TODO ожиаем MnemNode или MacroNode, получен:" + node, null));
			}
		}
	}
	
	private static void showHelp() {
		System.out.println("AVR assembler for vm5277 Embedded Toolkit");
		System.out.println("Version: " + VERSION + " | License: GPL-3.0-or-later");
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
		System.out.println("  -o, --output <file> Output HEX file (default: <input>.hex)");
		System.out.println("  -f, --format <fmt>  Output format (hex, bin)");
		System.out.println("                     hex     - Intel HEX (default)");
		System.out.println("                     bin     - Raw binary");
		System.out.println("  -d, --device <mcu>  Target MCU (e.g. atmega328p)");
		System.out.println("  -H, --headers <dir> Path to MCU header files");
		System.out.println("  -I, --include <dir> Additional include path(s)");
		System.out.println("  -t, --tabsize <num> TODO размер табуляции");
		System.out.println("  -s, --strict <error|warning|silent>\tTODO реакция на неоднозначности");
		System.out.println("                     error   - считать ошибкой");
		System.out.println("                     warning - предупреждать (по умолчанию)");
		System.out.println("                     silent  - не сообщать");
		System.out.println("  -m, --map <file>   TODO имя файла map вывода");
		System.out.println("  -l, --list <file>  TODO имя файла list вывода");
		System.out.println();
		System.out.println("  -v, --version      Display version");
		System.out.println("  -h, --help         Show this help");
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

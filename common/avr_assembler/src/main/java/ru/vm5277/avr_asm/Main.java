/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
29.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.nodes.MnemNode;
import ru.vm5277.avr_asm.nodes.SourceType;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Lexer;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.InfoMessage;
import ru.vm5277.common.messages.MessageContainer;

public class Main {
    public	final	static	String	VERSION		= "0.0.1";
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
		
		for(int i=0; i<args.length; i++) {
			String arg = args[i++];
			if(args.length > i+1) {
				if(arg.equals("-I") || arg.equals("--include")) {
					sourcePaths.put(args[i], SourceType.LIB);
				}
				else if(arg.equals("-t") || arg.equals("--tabsize")) {
					tabSize = Integer.parseInt(args[i]);
				}
			}
			else {
				source = arg;
//				System.err.println("[ERROR] Invalid parameter:" + arg);
//				System.exit(0);
			}
		}
 
		MessageContainer mc = new MessageContainer(16, true, false);
		
		String rtosPath = toolkitPath + File.separator + "platform" + File.separator + "avr" + File.separator + "rtos";
		sourcePaths.put(rtosPath, SourceType.RTOS);
		File sourceFile = new File(source);
		String baseDir = sourceFile.getParentFile().getAbsolutePath();
		sourcePaths.put(baseDir, SourceType.BASE);
		
		InstrReader instrReader = new InstrReader("./", mc);
		Scope scope = new Scope(sourceFile, instrReader);
		if(null != mcu) scope.setDevice(mcu);
		
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(sourceFile))) {
			mc.setFile(source, null);
			Lexer lexer = new AsmLexer(isr, scope, mc);
			//for(Token token : lexer.getTokens()) {
				//System.out.print(token.toString());
			//}
			Parser parser = new Parser(lexer.getTokens(), scope, mc, sourcePaths, tabSize);
			mc.add(new InfoMessage("---Second pass---", null));
			for(MnemNode mnemNode : scope.getMnemNodes()) {
				mnemNode.secondPass();
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		mc.releaseFile(null); // TODO объединить с IncludeSymbol?
		try {scope.leaveImport();} //TODO не проверяю на MessageContainerIsFullException
		catch(ParseException e) {mc.add(e.getErrorMessage());}
		catch(CriticalParseException e) {mc.add(e.getErrorMessage()); throw e;}

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
		System.out.println("  -o, --output <file>\tOutput HEX file (default: <input>.hex)");
		System.out.println("  -d, --device <mcu>\tTarget MCU (e.g. atmega328p)");
		System.out.println("  -H, --headers <dir>\tPath to MCU header files");
		System.out.println("  -I, --include <dir>\tAdditional include path(s)");
		System.out.println("  -t, --tabsize <num>\tTODO размер табуляции");
		System.out.println("  -v, --version\t\tDisplay version");
		System.out.println("  -h, --help\t\tShow this help");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  avrasm main.asm -d atmega328p -o firmware.hex -I ./libs");
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

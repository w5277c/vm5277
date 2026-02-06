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

package ru.vm5277.flasher;

import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import ru.vm5277.common.DefReader;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.PlatformType;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.firmware.IntelHex.IntelHexParser;
import ru.vm5277.common.firmware.Segment;

public class Main {
	public	final	static	String					VERSION				= StrUtils.readVersion(Main.class);
	private	final	static	int						WAIT_TIME			= 15*1000;
	public			static	boolean					isWindows;
	public			static	Path					toolkitPath;
	private			static	boolean					quiet;
	
	public static void main(String[] args) {
		System.exit(exec(args));
	}
	
	public static int exec(String[] args) {
		long startTimestamp = System.currentTimeMillis();
	
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));
		
		toolkitPath = FSUtils.getToolkitPath();
		
		if(0x00 == args.length) {
			showHelp();
			return 0;
		}
		if(0x01 == args.length) {
			if(args[0x00].equals("--version")) {
				System.out.println("j8b flasher version: " + VERSION);
			}
			else {
				showHelp();
			}
			return 0;
		}
		
		PlatformType platform = null;
		String mcu = null;
		Float cpuFreq = null;
		String source = null;
		int fwVersion = 0; 
		int retries = 3;
		String serialDeviceName = null;
		boolean isEncryptedFw = false;
		boolean smartMode = false;
		int	waitTime = 0;
		boolean	interactiveMode = false;
		
		boolean doClearCache = false;
		boolean doFlash = false;
		boolean doVerify = false;
		boolean doErase = false;
		boolean doReboot = false;
		
		if(0x02<=args.length) {
			String[] parts = args[0x00].split(":");
			if(0x02 == parts.length) {
				platform = PlatformType.valueOf(parts[0].toUpperCase());
				mcu = parts[1];
			}
			try {
				cpuFreq = Float.parseFloat(args[0x01]);
				if(0>=cpuFreq || 255<cpuFreq) {
					System.err.println("[ERROR] Invalid freq parameter: " + args[0x01]);
					return 1;
				}
			}
			catch(Exception e) {
				System.err.println("[ERROR] Invalid freq parameter:"  + args[0x01]);
				return 1;
			}
			
			if(0x03<=args.length) {
				source = args[0x02];

				for(int i=3; i<args.length; i++) {
					String arg = args[i];
					if(args.length>i+1 && (arg.equals("-P") || arg.equals("--path"))) {
						toolkitPath = FSUtils.resolveWithEnv(args[++i]);
					}
					else if(args.length>i+1 && (arg.equals("-S") || arg.equals("--serial"))) {
						serialDeviceName = args[++i];
					}
					else if(args.length>i+1 && (arg.equals("-f") || arg.equals("--fwversion"))) {
						try {
							fwVersion = Integer.parseInt(args[++i]);
						}
						catch(Exception ex) {}
						if(0>fwVersion||255<fwVersion) {
							System.err.println("[ERROR] Invalid fwversion parameter value");
							return 1;
						}
					}
					else if(args.length>i+1 && (arg.equals("-r") || arg.equals("--retries"))) {
						retries = -1;
						try {
							retries = Integer.parseInt(args[++i]);
						}
						catch(Exception ex) {}
						if(0>retries) {
							System.err.println("[ERROR] Invalid retries parameter value");
							return 1;
						}
					}
					else if(arg.equals("-s") || arg.equals("--smart")) {
						smartMode = true;
					}
					else if(arg.equals("-w") || arg.equals("--wait")) {
						waitTime = WAIT_TIME;
					}
					else if(arg.equals("-C") || arg.equals("--clear-cache")) {
						doClearCache = true;
					}
					else if(arg.equals("-E") || arg.equals("--erase")) {
						doErase = true;
					}
					else if(arg.equals("-F") || arg.equals("--flash")) {
						doFlash = true;
					}
					else if(arg.equals("-V") || arg.equals("--verify")) {
						doVerify = true;
					}
					else if(arg.equals("-R") || arg.equals("--reboot")) {
						doReboot = true;
					}
					else if(arg.equals("-q") || arg.equals("--quiet")) {
						quiet = true;
					}
					else if(arg.equals("-e") || arg.equals("--encrypted")) {
						isEncryptedFw = true;
					}
					else if(arg.equals("-I") || arg.equals("--interactive")) {
						interactiveMode = true;
					}
					else {
						System.err.println("[ERROR] Invalid parameter: " + arg);
						return 0;
					}
				}
			}
		}
		else {
			showHelp();
			return 0;
		}
		
		if(!quiet) showDisclaimer();
		
		if(null==toolkitPath) {
			showInvalidToolkitDir(null);
			return 1;
		}
		if(!quiet) System.out.println("Toolkit path: " + toolkitPath.toString());
		
		if(null==platform || null==mcu || mcu.isEmpty()) {
			showInvalidDeviceFormat(args[0]);
			return 1;
		}

		Path incPath = toolkitPath.resolve("defs").resolve(platform.toString().toLowerCase()).resolve(mcu + ".inc");
		if(!incPath.toFile().exists()) {
			showInvalidToolkitDir(incPath);
			return 1;
		}

		Path rtosPath = toolkitPath.resolve("rtos").resolve(platform.toString().toLowerCase()).normalize();
		if(!rtosPath.toFile().exists()) {
			showInvalidToolkitDir(rtosPath);
			return 1;
		}

		Path defPath = rtosPath.resolve("devices").resolve(mcu + ".def");
		if(!defPath.toFile().exists()) {
			showInvalidToolkitDir(defPath);
			return 1;
		}

		if(smartMode && isEncryptedFw) {
			System.err.println("[ERROR] Smartmode can't be realized with encrypted firmware.");
			return 1;
		}
		
		Path cachePath = null;		
		if(smartMode) {
			cachePath = toolkitPath.resolve("var").resolve("mcu_caches");
			if(!cachePath.toFile().exists()) {
				if(!cachePath.toFile().mkdirs()) {
					showInvalidToolkitDir(cachePath);
					return 1;
				}
			}
		}
		
		DefReader dReader = new DefReader();
		try {
			dReader.parse(incPath.normalize());
		}
		catch(Exception ex) {
			System.err.println("[ERROR] " + ex.getMessage());
			return 1;
		}

		Long flashSize = dReader.getNum("FLASH_SIZE");
		if(null==flashSize) {
			showNotDefined("FLASH_SIZE");
			return 1;
		}

		Long flashPageSize = dReader.getNum("FLASH_PAGESIZE");
		if(null==flashPageSize) {
			showNotDefined("FLASH_PAGESIZE");
			return 1;
		}

		Long bootWAddr = dReader.getNum("BOOT_512W_ADDR");
		if(null==bootWAddr) {
			showNotDefined("BOOT_512W_ADDR");
			return 1;
		}

		if(0!=bootWAddr%flashPageSize) {
			System.err.println("[ERROR] MCU definition error: available flash size not aligned to hardware page boundaries");
			return 1;
		}
		
		Long signature = dReader.getNum("DEVICE_SIGNATURE");
		if(null==signature) {
			showNotDefined("DEVICE_SIGNATURE");
			return 1;
		}
		
		byte[] sourceData = null;
		List<Segment> sourceSegments = null;
		if(doFlash || doVerify) {
			Path sourcePath = FSUtils.resolve(toolkitPath, source);
			if(!sourcePath.toFile().exists()) {
				showInvalidToolkitDir(sourcePath);
				return 1;
			}

			try (Scanner scan = new Scanner(sourcePath)) {
				int total = 0;
				IntelHexParser ihp = new IntelHexParser(scan);
				sourceData = new byte[bootWAddr.intValue()*2];
				if(isEncryptedFw) { //Заполняем случайными данными для пустых блоков
					SecureRandom srnd = new SecureRandom(); //TODOCOM реализовать более безопасный механизм
					srnd.nextBytes(sourceData);
				}
				else {
					Arrays.fill(sourceData, (byte)0xff);
				}

				sourceData[sourceData.length-0x01] = (byte)fwVersion;	// Записываем в последний байт прошивки версию прошивки, если прошивка занимает всю память
																		// то версия будет заменена кодом прошивки.
				sourceSegments = ihp.parse(sourceData);	
				ihp.appendSegment(sourceData.length-0x01, 0x01, sourceSegments);

				if(!quiet) {
					System.out.println("Source: " + sourcePath.toString());
					System.out.println(" ----- Source segments ----- ");
					for(Segment segment : sourceSegments) {
						System.out.println(	" Start\t= " + String.format("0x%04X", segment.getAddr()) + ", Length = " + String.format("0x%04X", segment.getSize()) +
											" bytes");
						total += segment.getSize();
					}
					System.out.println(" -----");
					System.out.println(" Total\t:  " + total + " bytes " + String.format("%.1f", 100d*total/sourceData.length) + "%");
				}
				else {
					for(Segment segment : sourceSegments) {
						total += segment.getSize();
					}
					System.out.println("Firmware size: " + total + " bytes " + String.format("%.1f", 100d*total/sourceData.length) + "%");
				}
			}
			catch(Exception ex) {
				System.err.println("[ERROR] " + ex.getMessage());
				return 1;
			}
		}
		
		if(!quiet) {
			System.out.println();
			System.out.print("Connecting to device: " + (null==serialDeviceName ? "auto detect" : serialDeviceName));
		}
		
		ServiceMessageHandler serviceMessageHandler = null;
		if(interactiveMode) { // В инерактивном режиме может потребоваться обработка сервисных сообщений
			// В том числе трассировка исключений
			
			final TargetInfoParser parser = new TargetInfoParser();
			if(null!=source) {
				int pos = source.lastIndexOf(".");
				if(-1 != pos) {
					String targetInfo = source.substring(0, pos);
					if(targetInfo.endsWith("_cseg")) {
						targetInfo = targetInfo.substring(0, targetInfo.length()-0x05);
					}
					File file = new File(targetInfo + ".dbg");
					if(file.exists()) {
						parser.parse(file);
					}
				}
			}
			
			serviceMessageHandler = new ServiceMessageHandler() {
				@Override
				public String handle(String message) {
					if(parser.isReady()) {
						if(message.startsWith("ETRC:")) {
							StringBuilder sb = new StringBuilder();
							int pos = 0x05;
							int endNumPos = getHexNumEndPos(message, pos);
							int exId = Integer.parseInt(message.substring(pos, endNumPos), 0x10);
							String exName = parser.getException(exId);
							pos=endNumPos+1;
							endNumPos = getHexNumEndPos(message, pos);
							int code = Integer.parseInt(message.substring(pos, endNumPos), 0x10);
							String codeName = parser.getExceptionCode(exId, code);
							pos=endNumPos+1;
							//sb.append(">").append(message).append("<");
							sb.append("Stacktrace for ").append(exName).append(", code:").append(null==codeName ? code : codeName).append("\n");
							
							
							while(pos<message.length()) {
								if(message.substring(pos).startsWith("..")) {
									sb.append("...\n");
									pos+=0x03;
								}
								else {
									endNumPos = getHexNumEndPos(message, pos);
									sb.append(parser.getThrowPoint(Integer.parseInt(message.substring(pos, endNumPos), 0x10))).append("\n");
									pos=endNumPos+1;
								}
							}
							sb.append("End stacktrace\n");
							return sb.toString();
						}	
					}
					return null;
				}
				
				public int getHexNumEndPos(String str, int startPos) {
					for(int i=startPos; i<str.length(); i++) {
						char ch = str.charAt(i);
						if(!(Character.isDigit(ch) || ((ch|0x20)>='a' && (ch|0x20)<='f'))) {
							return i;
						}
					}
					return str.length();
				}
			};
			
		}
		
		
		BootloaderIface bIface = new BootloaderIface(cpuFreq, serialDeviceName, waitTime, retries);
		if(bIface.open()) {
			boolean result = true;

			System.out.println("Serial port: " + (null==bIface.getSerialPort() ? "null" : bIface.getSerialPort().getPortName()));
			System.out.println("Connection mode:" + (bIface.getDeviceInfo().isSingleWireMode() ? "1" : "2") + " wire mode");
			System.out.println("Device info:" + bIface.getDeviceInfo());

			if(bIface.checkSignature(signature.intValue())) {

				if(doClearCache) {
					if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
						System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
											cachePath.toString() + " and try again.");
						return 1;
					}
				}

				if(result && doErase) {
					long timestamp = System.currentTimeMillis();
					if(smartMode) {
						if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
							System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
												cachePath.toString() + " and try again.");
							return 1;
						}
					}

					if(!quiet) {
						System.out.println("Erasing...");
					}
					if(bIface.erase(bootWAddr.intValue()*2, flashPageSize.intValue())) {
						if(!quiet) {
							System.out.println("Success, time: " + String.format(Locale.US, "%.3f", (System.currentTimeMillis()-timestamp)/1000f) + " s\n");
						}
					}
					else {
						result = false;
					}
				}

				if(result && doFlash) {
					long timestamp = System.currentTimeMillis();
					if(smartMode) {
						FwCacher.updateByCache(	cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID(), sourceSegments, sourceData,
												bootWAddr.intValue());
						if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
							System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
												cachePath.toString() + " and try again.");
							return 1;
						}
					}
					if(!quiet) {
						System.out.println("Flashing"  + (smartMode ? "(smart mode)" : "") + "...");
					}
					if(bIface.flash(sourceSegments, sourceData, flashPageSize.intValue(), isEncryptedFw)) {
						if(smartMode) {
							if(!FwCacher.putCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID(), sourceData)) {
								if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
									System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
														cachePath.toString() + " and try again.");
									return 1;
								}
							}
						}
						if(!quiet) {
							System.out.println("Success, time: " + String.format(Locale.US, "%.3f", (System.currentTimeMillis()-timestamp)/1000f) + " s\n");
						}
					}
					else {
						if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
							System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
												cachePath.toString() + " and try again.");
							return 1;
						}
						result = false;
					}
				}

				if(result && doVerify) {
					long timestamp = System.currentTimeMillis();
					if(!quiet) {
						System.out.println("Verifying...");
					}
					if(bIface.verify(sourceSegments, sourceData, flashPageSize.intValue(), isEncryptedFw)) {
						if(!quiet) {
							System.out.println("Success, time: " + String.format(Locale.US, "%.3f", (System.currentTimeMillis()-timestamp)/1000f) + " s\n");
						}
					}
					else {
						if(!FwCacher.clearCache(cachePath, platform, signature.intValue(), bIface.getDeviceInfo().getUID())) {
							System.err.println(	"[ERROR] Failed to delete cache file.\nPlease try manually deleting the cache directory " +
												cachePath.toString() + " and try again.");
							return 1;
						}
						result = false;
					}
				}

				if(result && doReboot) {
					long timestamp = System.currentTimeMillis();
					if(!quiet) {
						System.out.println("Rebooting...");
					}
					if(bIface.reboot()) {
						if(!quiet) {
							System.out.println("Success, time: " + String.format(Locale.US, "%.3f", (System.currentTimeMillis()-timestamp)/1000f) + " s\n");
						}
					}
					else {
						result = false;
					}
				}
				
				if(result && interactiveMode) {
					try {
						System.out.println("\nEntering interactive mode. Press Ctrl+C to exit.");
						SerialTerminal terminal = new SerialTerminal(bIface.getSerialPort(), serviceMessageHandler);

						Runtime.getRuntime().addShutdownHook(new Thread() {
							@Override
							public void run() {
								terminal.disable();
								System.out.println("\nExiting terminal mode...");
							}
						});
						terminal.start();
						terminal.join();
					}
					catch (Exception e) {
						System.err.println("[ERROR] Failed to start interactive mode: " + e.getMessage());
						result = false;
					}
				}
			}
			else {
				result = false;
				System.err.println("[ERROR] Different device signature");
			}

			bIface.close();

			if(result) {
				if(!quiet) {
					double time = (System.currentTimeMillis() - startTimestamp) / 1000f;
					System.out.println("All operations successfully executed");
					System.out.println("Total time: " + String.format(Locale.US, "%.3f", time) + " s\n");
				}
				else {
					System.out.println("Firmware flash SUCCESS");
				}
				return 0;
			}
			else {
				System.err.println("Firmware flash FAIL");
				return 1;
			}
		}
		else {
			return 1;
		}
    }
	
	public static void showMsg(String str, boolean always) {
		if(!quiet || always) {
			System.out.print(str);
		}
	}
	public static void showErr(String str) {
		System.err.println(str);
	}
	
	private static void showDisclaimer() {
		System.out.println(";j8b flasher: firmware flasher for vm5277 Embedded Toolkit");
		System.out.println(";Version: " + VERSION + " | License: Apache-2.0");
		System.out.println(";================================================================");
		System.out.println(";WARNING: This project is under active development.");
		System.out.println(";The primary focus is on functionality; testing is currently limited.");
		System.out.println(";Please report any bugs found to: konstantin@5277.ru");
		System.out.println(";================================================================");
	}

	private static void showHelp() {
		System.out.println("j8b flasher: firmware flasher for vm5277 Embedded Toolkit");
		System.out.println("-------------------------------------------");
		System.out.println();
		System.out.println("This tool is part of the open-source project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: j8bf" + (isWindows ? ".exe" : "") + " <platform>:<mcu> <freq> <input.hex> [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -S,  --serial <device>  Serial device name (default: auto-scan)");
		System.out.println("                          Examples: /dev/ttyUSB0, COM3, /dev/ttyACM0");
		System.out.println("  -P,  --path <dir>       Custom toolkit directory path");
		System.out.println("  -s,  --smart            Enable smart flash (only write changed pages)");
		System.out.println("  -e   --encrypted        Firmware is encrypted");
		System.out.println("  -C   --clear-cache      Clear firmware cache for this device (smart mode)");
		System.out.println("  -E,  --erase            Erase data on device");
		System.out.println("  -F,  --flash            Flash HEX file to device");
		System.out.println("  -V,  --verify           Verify device against HEX file");
		System.out.println("  -R,  --reboot           Reboot device");
		System.out.println("  -I,  --interactive      Enter interactive mode after operations");
		System.out.println("                          (stdin/stdout redirected to serial port, Ctrl+C for exit)");
		System.out.println("  -f,  --fwversion <num>  Set firmware version 0-255 (default: 0, used with keys F,V)");
		System.out.println("  -w,  --wait             Wait for device (10 seconds)");
		System.out.println("  -r,  --retries <num>    Number of retries on failure (default: 3)");
		System.out.println();
		System.out.println("  -q,  --quiet            Quiet mode (no output)");
		System.out.println("  -v,  --version          Display version");
		System.out.println("  -h,  --help             Show this help");
		System.out.println();
		System.out.println("Progress indicators:");
		System.out.println("    e/w  Page erased/written successfully on first attempt");
		System.out.println("    E/W  Page erased/written after retry (error occurred)");
		System.out.println("    i    Page identical to existing content (not rewritten)");
		System.out.println("    I    Page identical after retry (verification error occurred)");
		System.out.println("    s    Page skipped by smart mode (identical to cache)");
		System.out.println("    -    Page skipped (no data in hex file for this address)");
		System.out.println();
		System.out.println("Example:");
		System.out.println("  j8bf avr:atmega328p 16 main.hex -s -F -V -R -S /dev/ttyUSB0");
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
	
	private static void showNotDefined(String defName) {
		System.err.println("[ERROR] " + defName + " definition not found in MCU headers.");
	}

	private static void showInvalidToolkitDir(Path subPath) {
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

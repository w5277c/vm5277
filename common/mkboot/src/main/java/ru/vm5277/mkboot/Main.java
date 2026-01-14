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

package ru.vm5277.mkboot;

import ru.vm5277.common.DefReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import ru.vm5277.common.DatatypeConverter;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.exceptions.CompileException;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.AssemblerLoader;
import ru.vm5277.common.SourceType;
import ru.vm5277.common.messages.MessageContainer;


public class Main {
	public	final	static	String					VERSION				= StrUtils.readVersion(Main.class);
	public			static	boolean					isWindows;
	public			static	Path					toolkitPath;

	public static void main(String[] args) throws CompileException {
		showDisclaimer();
		
		isWindows = (null != System.getProperty("os.name") && System.getProperty("os.name").toLowerCase().contains("windows"));
		
		toolkitPath = FSUtils.getToolkitPath();
		
		if(0x00 == args.length) {
			showHelp();
			System.exit(0);
		}
		if(0x01==args.length && args[0x00].equals("--version")) {
			System.out.println("j8b mkboot version: " + VERSION);
			System.exit(0);
		}
		
		String platform = null;
		String mcu = null;
		String secureSource = null;
		byte[] key = null;
		String uidsFile = "var" + File.separator+ "uids.db";
		byte[] uid = null;
		byte[] authId = null;
		String authPassword = null;
		UIDMode uidMode = UIDMode.LOCAL;
		int pin = -1;
		boolean allowOverrideUID = false;
		
		
		if(0x01<=args.length) {
			String[] parts = args[0x00].split(":");
			if(0x02 == parts.length) {
				platform = parts[0];
				mcu = parts[1];
			}
			
			for(int i=1; i<args.length; i++) {
				String arg = args[i];
				if(arg.equals("-o") || arg.equals("--overwrite")) {
					allowOverrideUID=true;
				}
				else if(args.length>i+1 && (arg.equals("-P") || arg.equals("--path"))) {
					toolkitPath = FSUtils.resolveWithEnv(args[++i]);
				}
				else if(args.length>i+1 && (arg.equals("-k") || arg.equals("--key"))) {
					key = null;
					try {
						key = DatatypeConverter.parseHexBinary(args[++i]);
					}
					catch(Exception ex) {}
					if(null==key || 0x20!=key.length) {
						System.err.println("[ERROR] Invalid key hexdata, expected 64 hex digits");
						System.exit(1);
					}
				}
				else if(args.length>i+1 && (arg.equals("-u") || arg.equals("--uid"))) {
					uid = null;
					try {
						uid = DatatypeConverter.parseHexBinary(args[++i]);
					}
					catch(Exception ex) {}
					if(null==uid || 0x08!=uid.length) {
						System.err.println("[ERROR] Invalid uid hexdata, expected 16 hex digits");
						System.exit(1);
					}
				}
				else if(arg.equals("-s") || arg.equals("--secure")) {
					secureSource = args[++i];
				}
				else if(args.length>i+1 && (arg.equals("-i") || arg.equals("--uids"))) {
					uidsFile = args[++i];
				}
				else if(args.length>i+1 && (arg.equals("-m") || arg.equals("--uidmode"))) {
					uidMode = null;
					try {
						uidMode = UIDMode.valueOf(args[++i].toUpperCase());
					}
					catch(Exception ex) {
					}
					if(null==uidMode) {
						System.err.println("[ERROR] Invalid uid mode value");
						System.exit(1);
					}
				}
				else if(args.length>i+1 && (arg.equals("-p") || arg.equals("--pin"))) {
					String tmp = args[++i].toUpperCase();
					if(tmp.equalsIgnoreCase("rst") || tmp.equalsIgnoreCase("reset")) {
						pin = -1;
					}
					else if(	0x03==tmp.length() && tmp.startsWith("P") &&
						'A'<=tmp.charAt(0x01) && 'Z'>=tmp.charAt(0x01) && '0'<=tmp.charAt(0x02) && '7'>=tmp.charAt(0x02)) {
						
						pin = (tmp.charAt(0x01)-'A')*0x10 + (tmp.charAt(0x02)-'0');
					}
					else {
						System.err.println("[ERROR] Invalid pin value");
						System.exit(1);
					}
				}
				else if(args.length>i+2 && (arg.equals("-a") || arg.equals("--auth"))) {
					authId = null;
					try {
						authId = DatatypeConverter.parseHexBinary(args[++i]);
					}
					catch(Exception ex) {}
					if(null==authId || 0x02!=authId.length) {
						System.err.println("[ERROR] Invalid auth hexdata, expected 6 hex digits");
						System.exit(1);
					}
					authPassword = args[++i];
				}
				else {
					System.err.println("[ERROR] Invalid parameter: " + arg);
					System.exit(1);
				}
			}
		}
		else {
			showHelp();
			System.exit(1);
		}
		
		if(null==toolkitPath) {
			showInvalidToolkitDir(toolkitPath, null);
			System.exit(1);
		}
		System.out.println("Toolkit path: " + toolkitPath.toString());
		
		if(null==platform || platform.isEmpty() || null==mcu || mcu.isEmpty()) {
			showInvalidDeviceFormat(args[0]);
			System.exit(1);
		}

		if(UIDMode.GLOBAL==uidMode && (null==authId || null==authPassword)) {
			showInvalidGlobalMode();
			System.exit(1);
		}
		if(UIDMode.PARAM==uidMode && null==uid) {
			showInvalidParamMode();
			System.exit(1);
		}
		
		Path rtosPath = toolkitPath.resolve("rtos").resolve(platform).normalize();
		if(!rtosPath.toFile().exists()) {
			showInvalidToolkitDir(toolkitPath, rtosPath);
			System.exit(1);
		}

		Path incPath = toolkitPath.resolve("defs").resolve(platform).resolve(mcu + ".inc");
		if(!incPath.toFile().exists()) {
			showInvalidToolkitDir(toolkitPath, incPath);
			System.exit(1);
		}

		Path defPath = rtosPath.resolve("devices").resolve(mcu + ".def");
		if(!defPath.toFile().exists()) {
			showInvalidToolkitDir(toolkitPath, defPath);
			System.exit(1);
		}

		try {
			DefReader dReader = new DefReader();
			dReader.parse(incPath.normalize());
			dReader.parse(defPath.normalize());
			
			Long flashSize = dReader.getNum("FLASH_SIZE");
			if(null==flashSize) {
				showNotDefined("FLASH_SIZE");
				System.exit(1);
			}
			
			Long bldrSecSupport = dReader.getNum("BLDRSEC_SUPPORT");
			if(null!=secureSource) {
				if(null==bldrSecSupport || 0x00==bldrSecSupport) {
					showDecryptionUnsupported();
					System.exit(1);
				}
				
				if(null==key) {
					SecureRandom rnd = new SecureRandom();
					key = new byte[0x20];
					rnd.nextBytes(key);
				}
			}
			
			String source = (1024>=flashSize ? "bldrtiny.asm" : "bldr.asm");
			Path bldrPath = rtosPath.resolve("boot").resolve(source);
			if(!bldrPath.toFile().exists()) {
				showInvalidToolkitDir(toolkitPath, bldrPath);
				System.exit(1);
			}

			Path securePath = null;
			if(null!=secureSource) {
				securePath = rtosPath.resolve("boot").resolve(secureSource);
				if(!securePath.toFile().exists()) {
					showInvalidToolkitDir(toolkitPath, securePath);
					System.exit(1);
				}
			}
			
			Path uidsPath = toolkitPath.resolve(uidsFile);
			if(!uidsPath.toFile().exists()) {
				try {
					Files.createFile(uidsPath);
				}
				catch(Exception ex) {
					System.err.println("Can't create local file '" + uidsFile + "' (uids storage)");
					System.exit(1);
				}
			}

			Properties props = new Properties();
			try (FileInputStream in = new FileInputStream(uidsPath.normalize().toFile())) {
				props.load(in);
			}
			catch(Exception ex) {
				System.err.println("Can't read local file '" + uidsFile + "' (uids storage)");
				System.exit(1);
			}
			
			Long signature = dReader.getNum("DEVICE_SIGNATURE");
			
			if(UIDMode.GLOBAL==uidMode) {
				uid = null;
				try {
					//TODO проверять сертификат
					URL url = new URL(	"https://vm5277.ru/api/uid?vid=" + DatatypeConverter.printHexBinary(authId) +
										"&pass=" + URLEncoder.encode(authPassword, "UTF-8") +
										"&platform=" + URLEncoder.encode(platform, "UTF-8") +
										"&mcu=" + URLEncoder.encode(mcu, "UTF-8"));
					String result = downloadContent(url);
					if(null!=result && !result.isEmpty()) {
						uid = DatatypeConverter.parseHexBinary(result);
					}
					
					//TODO также добавлять в локальное хранилище для исключения подмены сервера и генерации не уникальных ID
				}
				catch(Exception ex) {
				}
				if(null==uid) {
					showServerCommunicationFailed();
					System.exit(1);
				}
				
				String uidStr = DatatypeConverter.printHexBinary(uid);
				String property = props.getProperty(uidStr);
				if(null!=property) {
					if(allowOverrideUID) {
						System.out.println("[WARNING] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
//						props.remove(uidStr);
					}
					else {
						System.err.println("[ERROR] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
						System.exit(1);
					}
				}
				props.put(uidStr, makeUIDInfoStr("global", uid, platform, mcu, signature, key));
			}
			else if(UIDMode.LOCAL==uidMode) {
				SecureRandom rnd = new SecureRandom();
				String uidStr = String.format("%016x", rnd.nextLong());
				while(null!=props.getProperty(uidStr)) {
					uidStr = String.format("%016x", rnd.nextLong());
				}
				uid = DatatypeConverter.parseHexBinary(uidStr);

				String property = props.getProperty(uidStr);
				if(null!=property) {
					if(allowOverrideUID) {
						System.out.println("[WARNING] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
//						props.remove(uidStr);
					}
					else {
						System.err.println("[ERROR] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
						System.exit(1);
					}
				}
				props.put(uidStr, makeUIDInfoStr("local", uid, platform, mcu, signature, key));
			}
			else {
				String uidStr = DatatypeConverter.printHexBinary(uid);
				String property = props.getProperty(uidStr);
				if(null!=property) {
					if(allowOverrideUID) {
						System.out.println("[WARNING] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
//						props.remove(uidStr);
					}
					else {
						System.err.println("[ERROR] UID '" + uidStr + "' already exists in '" + uidsFile + "'");
						System.exit(1);
					}
				}
				props.put(uidStr, makeUIDInfoStr("param", uid, platform, mcu, signature, key));
			}

			try (OutputStream out = new FileOutputStream(uidsPath.normalize().toFile())) {
				props.store(out, "UIDs storage");
			}
			catch(Exception ex) {
				System.err.println("Can't write local file '" + uidsFile + "' (uids storage)");
				System.exit(1);
			}

			
			try {
				AssemblerInterface ai = AssemblerLoader.load(toolkitPath, platform);
				
				Map<Path, SourceType> sourcePaths = new HashMap<>();
				sourcePaths.put(FSUtils.resolve(toolkitPath, "defs" + File.separator + platform + File.separator), SourceType.LIB);
				sourcePaths.put(FSUtils.resolve(toolkitPath, "rtos" + File.separator + platform + File.separator), SourceType.LIB);
				
				List<String> includes = new ArrayList<>();
				includes.add("devices/" + mcu + ".def");
				if(null!=key) {
					includes.add(securePath.normalize().toString());
				}
				
				Map<String, Long> defines = new HashMap<>();
				defines.put("os_ft_bootloader_only", 0x01L);
				defines.put("v_uid", (long)(((uid[0x00]&0xff)<<0x08) + (uid[0x01]&0xff)));
				defines.put("d_uid", java.nio.ByteBuffer.wrap(uid).getLong()&0xffffffffffffL);
				if(null!=key) {
					for(int i=0; i<8; i++) {
						defines.put("key"+(i+1), java.nio.ByteBuffer.wrap(key, i*4, 4).getInt()&0xffffffffL);
					}
				}
				
				if(-1!=pin) {
					Long num = dReader.getNum("PORT" + ('A'+(pin>>0x04)));
					if(null==num) {
						showNotDefined("PORT" + ('A'+(pin>>0x04)));
						System.exit(1);
					}
					defines.put("BLDR_UART_PORT".toLowerCase(), num);
					
					num = dReader.getNum("DDR" + ('A'+(pin>>0x04)));
					if(null==num) {
						showNotDefined("DDR" + ('A'+(pin>>0x04)));
						System.exit(1);
					}
					defines.put("BLDR_UART_DDR".toLowerCase(), num);

					num = dReader.getNum("PIN" + ('A'+(pin>>0x04)));
					if(null==num) {
						showNotDefined("PIN" + ('A'+(pin>>0x04)));
						System.exit(1);
					}
					defines.put("BLDR_UART_PIN".toLowerCase(), num);

					defines.put("BLDR_UART_PINNUM".toLowerCase(), (long)(pin&0x0f));
				}

				
				dReader.parse(bldrPath);
				Long bldrVersion = dReader.getNum("BLDR_VERSION");
				
				MessageContainer mc = new MessageContainer(5, true, false);
				boolean result = ai.exec(mc, mcu, bldrPath, sourcePaths, AssemblerInterface.STRICT_NONE, "bldr", null, null, defines, includes);
				if(result) {
					System.out.println();
					System.out.println("#-------------------------------------------------------------------------------------------");
					System.out.println("#    *** CRITICAL INFORMATION ***");
					System.out.println("#-------------------------------------------------------------------------------------------");
					System.out.println("# Platform:                 " + platform);
					System.out.println("# MCU:                      " + mcu);
					System.out.println("# Device signature:         " + (null==signature ? "null" : String.format("%08x", signature)));
					System.out.println("# Bootloader version:       " + (null==bldrVersion ? "null" :  bldrVersion));
					System.out.println("# Device unique identifier: " + DatatypeConverter.printHexBinary(uid));
					if(null!=key) {
						System.out.println("# Decryption key:           " + DatatypeConverter.printHexBinary(key));
					}
					System.out.println();
					System.out.println("#-------------------------------------------------------------------------------------------");
					System.out.println("#    *** CRITICAL SECURITY WARNINGS ***");
					System.out.println("#-------------------------------------------------------------------------------------------");
					System.out.println("# 1. CRITICAL INFORMATION IS REQUIRED FOR:");
					System.out.println("#    Identifying this specific device");
					System.out.println("#    Generating encrypted firmware (if using the key)");
					System.out.println("# 2. FIRMWARE GENERATED WITH THIS DATA:");
					System.out.println("#    Is for ONE MCU ONLY — do not reuse");
					System.out.println("#    MUST NEVER be distributed/shared");
					System.out.println("#    May contain the decryption key in plain binary form");
					System.out.println("# 3. INFORMATION LOCALLY STORED:");
					System.out.println("#    Critical information has been saved to: " + uidsPath.toString());
					if(null!=key) {
						System.out.println("#    Note: The decryption key is contained in this data");
					}
					System.out.println("#    Keep this file secure - contains sensitive device identification data.");

					if(null!=key) {
						System.out.println("# 4. SECURITY RISK IF COMPROMISED:");
						System.out.println("#    If the decryption key is exposed, ALL firmware encrypted with it becomes decryptable");
						System.out.println("#    Previously released firmware updates will lose all protection");
						System.out.println("#    Consider the key permanently compromised if shared");
						System.out.println("#-------------------------------------------------------------------------------------------");
					}
				}
				System.exit(result ? 0 : 1);
			}
			catch(FileNotFoundException ex) {
				System.err.println("[ERROR] Assembler JAR library not found: "  + ex.getMessage());
			}
			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		System.exit(1);
    }
	
	
	private static String makeUIDInfoStr(String srcType, byte[] uid, String platform, String mcu, Long signature, byte[] secureKey)
																														throws UnsupportedEncodingException {
		return	"timestamp:" + Long.toString(System.currentTimeMillis()) +
				",srcType:" + URLEncoder.encode(srcType, "UTF-8") +
				",uid:" + DatatypeConverter.printHexBinary(uid) +
				",platform:" + URLEncoder.encode(platform, "UTF-8") +
				",mcu:" + URLEncoder.encode(mcu, "UTF-8") +
				",signature:" + (null==signature ? "null" : String.format("%08x", signature)) +
				",secureKey:" + (null==secureKey ? "null" : DatatypeConverter.printHexBinary(secureKey));
	}
	


	public static String downloadContent(URL url) throws Exception {
		//TODO использовать Let's Encrypt
		// Создаем доверяющий всем менеджер
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() { return null; }
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			}
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			return reader.readLine();
		}
	}
	
	private static void showDisclaimer() {
		System.out.println(";j8b mkboot: bootloader firmware generator for vm5277 Embedded Toolkit");
		System.out.println(";Version: " + VERSION + " | License: Apache-2.0");
		System.out.println(";================================================================");
		System.out.println(";WARNING: This project is under active development.");
		System.out.println(";The primary focus is on functionality; testing is currently limited.");
		System.out.println(";Please report any bugs found to: konstantin@5277.ru");
		System.out.println(";================================================================");
	}

	
	private static void showHelp() {
		System.out.println("j8b mkboot: bootloader firmware generator for vm5277 Embedded Toolkit");
		System.out.println("-------------------------------------------");
		System.out.println();
		System.out.println("This tool is part of the open-source vm5277 project for 8-bit microcontrollers (AVR, PIC, STM8, etc.)");
		System.out.println("Provided AS IS without warranties. Author is not responsible for any consequences of use.");
		System.out.println();
		System.out.println("Project home: https://github.com/w5277c/vm5277");
		System.out.println("Contact: w5277c@gmail.com | konstantin@5277.ru");
		System.out.println();
		System.out.println("Usage: mkboot" + (isWindows ? ".exe" : "") + " <platform>:<mcu> [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -P,  --path <dir>       Custom toolkit directory path");
		System.out.println("  -k,  --key <hexdata>    Firmware decryption key (32 bytes, randomly generated if not specified)");
		System.out.println("  -s,  --secure <asmfile> Assembly file for bootloader with decryption function, can be used with -k");
		System.out.println("                          Decryption requires hardware support for protected bootloader sections");
		System.out.println("  -i,  --uids <file>      UIDs data file (default: uids.db)");
		System.out.println("  -u,  --uid <hexdata>    Chip unique identifier (8 bytes)");
		System.out.println("  -a,  --auth <hexdata> <password>");
		System.out.println("                          Vendor unique identifier (2 bytes) and password");
		System.out.println("  -m,  --uidmode <mode>   UID generation method:");
		System.out.println("                          global  - request to server (requires -a)");
		System.out.println("                          local   - use local file uids.db (default, can be overridden with -i)");
		System.out.println("                          param   - set manually (requires -u)");
		System.out.println("  -o,  --overwrite        Overwrite existing UID in local database");
		System.out.println("  -p   --pin <name>       MCU pin for bootloader ('format: 'PA0', default: RESET pin if supports GPIO)");
		System.out.println("  -v,  --version          Display version");
		System.out.println("  -h,  --help             Show this help");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  mkboot avr:atmega328p");
		System.out.println("  mkboot avr:atmega328p -k fe73...89 -s bldrsecure.asm -m local");
		System.out.println("  mkboot avr:attiny85 -u 2d849f626c42ff33f29a054bcae564e79f753a7e89a4761b1ed5f2680ac7 -p PB3");
	}
	
	private static void showServerCommunicationFailed() {
		System.err.println("[ERROR] Server communication failed.");
		System.err.println("Try:");
		System.err.println("  Check your internet connection");
		System.err.println("  Verify auth credentials (-a <hexdata> <password>)");
		System.err.println("  Use local UID mode: -m local");
		System.err.println("  Try again later");
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
	
	private static void showInvalidGlobalMode() {
		System.err.println("[ERROR] Global mode requires authentication. Use: -m global -a <hexdata> <password>");
		System.err.println("Register developer ID at: https://vm5277.ru/reg");
	}

	private static void showInvalidParamMode() {
		System.err.println("[ERROR] param mode requires UID. Use: -m param -u <hexdata>");
	}
	
	private static void showDecryptionUnsupported() {
		System.err.println("[ERROR] This MCU model does not support hardware protection of the bootloader section.");
		System.err.println("Remove the '-k/-s' options or select a different MCU.");
	}
	
	private static void showNotDefined(String defName) {
		System.err.println("[ERROR] " + defName + " definition not found in MCU headers.");
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

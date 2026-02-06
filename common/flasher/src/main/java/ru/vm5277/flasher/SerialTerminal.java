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

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import jssc.SerialPort;
import java.nio.charset.Charset;
import com.googlecode.lanterna.terminal.Terminal;
import java.util.Arrays;

public class SerialTerminal extends Thread {
	private	final			SerialPort				serialPort;
	private					ServiceMessageHandler	handler;
	private					boolean					enabled		= true;
	private	static	final	Charset					MC_CHARSET	= Charset.forName("KOI8-R");
	
	public SerialTerminal(SerialPort serialPort, ServiceMessageHandler hanlder) {
		this.serialPort = serialPort;
		this.handler = hanlder;
	}

	@Override
	public void run() {
		try {
			Terminal terminal = new DefaultTerminalFactory().createTerminal();
			while(enabled && !isTerminalClosed(terminal)) {
				try{Thread.sleep(100);} catch(Exception ex) {}
				KeyStroke ks = terminal.pollInput();
				if(null!=ks) {
					if(KeyType.EOF == ks.getKeyType()) break;
					Character ch = ks.getCharacter();
					System.out.println("ks:" + ks + ", ch:" + ch);
					if(null==ch) {
						switch(ks.getKeyType()) {
							case ArrowDown:
								ch = '2';
								break;
							case ArrowLeft:
								ch = '4';
								break;
							case ArrowRight:
								ch = '6';
								break;
							case ArrowUp:
								ch = '8';
								break;
						}
					}
					if(null!=ch) {
						String charStr = String.valueOf(ch);
						byte[] koi8Bytes = charStr.getBytes(MC_CHARSET);

						serialPort.writeByte(koi8Bytes[0x00]);
						serialPort.readBytes(0x01);
					}
				}

				StringBuilder sb = null;
				while(enabled && 0<serialPort.getInputBufferBytesCount()) {
					byte[] buffer = serialPort.readBytes(serialPort.getInputBufferBytesCount());
					if(null!=buffer && 0<buffer.length) {
						for(int i=0; i<buffer.length; i++) {
							if(0x00==buffer[i]) {
								sb = new StringBuilder();
							}
							else {
								if(null!=sb) {
									if('\n'==buffer[i]) {
										String result = handler.handle(sb.toString());
										terminal.putString(new String((null==result ? sb.toString() : result).getBytes(), MC_CHARSET));
										sb = null;
									}
									else {
										sb.append((char)buffer[i]);
									}
								}
								else {
									terminal.putCharacter(new String(buffer, i, 0x01, MC_CHARSET).charAt(0));
								}
							}
						}
/*						if(0<=pos) {
							terminal.putString(new String(buffer, 0x00, pos, MC_CHARSET));
							terminal.putString("EXCEPTION STACK TRACE:");
//							terminal.putString(new String(buffer, MC_CHARSET));
						}
						else {
							terminal.putString(new String(buffer, MC_CHARSET));
						}*/
						terminal.flush();
					}
				}
			}
			System.out.println("Terminal closed");
		}
		catch (Exception e) {
			if(enabled) {
				System.err.println("\n[ERROR] Write error: " + e.getMessage());
			}
		}
	}
	
	public boolean isTerminalClosed(Terminal terminal) {
		try {
			// Попытка чтения размера терминала
			terminal.getTerminalSize();
			return false;
		} catch (Exception e) {
			// Если возникает исключение, терминал вероятно закрыт
			return true;
		}
	}

	public void disable() {
		enabled = false;
	}
}
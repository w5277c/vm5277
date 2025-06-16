/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
03.06.2025	konstantin@5277.ru			Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class InstrReader {
	private					MessageContainer						mc;
	private					Path									basePath;
	private					Map<String, Instruction>				instrById	= new HashMap<>();
	private					Map<String, Map<String, Instruction>>	instrByMn	= new HashMap<>();
	private					Set<String>								supported;	// Поддерживаемые инструкции
	
	public InstrReader(Path instrPath, MessageContainer mc) {
		this.mc = mc;
		this.basePath = instrPath;
		
		try(BufferedReader br = new BufferedReader(new FileReader(basePath.resolve("full.instr").normalize().toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null == line) break;
				int commentPos = line.indexOf("#");
				if(-1 != commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim().toLowerCase();
				if(!line.isEmpty()) {
					try {
						Instruction instr = new Instruction(line);
						instrById.put(instr.getId(), instr);
						
						Map<String, Instruction> ids = instrByMn.get(instr.getMnemonic());
						if(null == ids) {
							ids = new HashMap<>();
							instrByMn.put(instr.getMnemonic(), ids);
						}
						ids.put(instr.getId(), instr);
						instrByMn.put(instr.getMnemonic(), ids);
					}
					catch(Exception e) {
						mc.add(new ErrorMessage(e.getMessage(), null));
					}
				}
			}
		}
		catch(Exception ex) {
			mc.add(new ErrorMessage("Cannot read AVR instruction definitions file:" + instrPath, null));
		}
	}

	public void setMCU(String mcu) {
		if(null == supported) {
			supported = new HashSet<>();
			try(BufferedReader br = new BufferedReader(new FileReader(basePath.resolve(mcu + ".instr").normalize().toFile()))) {
				while(true) {
					String line = br.readLine();
					if(null == line) break;
					int commentPos = line.indexOf("#");
					if(-1 != commentPos) {
						line = line.substring(0, commentPos);
					}
					line = line.trim().toLowerCase();
					if(!line.isEmpty()) {
						try {
							String[] parts = line.split(",");
							for(String part : parts) {
								part = part.trim().toLowerCase();
								supported.add(part);
							}
						}
						catch(Exception e) {
							mc.add(new ErrorMessage(e.getMessage(), null));
						}
					}
				}
			}
			catch(Exception ex) {
				mc.add(new ErrorMessage("Failed to load instruction set for " + mcu + " (file: " + basePath + ")", null));
			}
		}
		else {
			mc.add(new ErrorMessage("MCU already specified, .DEVICE cannot be redefined", null));
		}
	}
	
	public Map<String, Map<String, Instruction>> getInstrByMn() {
		return instrByMn;
	}
	public Map<String, Instruction> getInstrById() {
		return instrById;
	}
	
	public Set<String> getSupported() {
		return supported;
	}
}

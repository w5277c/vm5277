/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
03.06.2025	konstantin@5277.ru			Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class InstrReader {
	private	final	static	String									FILENAME		= "full.instr";
	private					Map<String, Instruction>				instrById	= new HashMap<>();
	private					Map<String, Map<String, Instruction>>	instrByMn	= new HashMap<>();

	
	public InstrReader(String path, MessageContainer mc) {
		try(BufferedReader br = new BufferedReader(new FileReader(path + File.separator + FILENAME))) {
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
			mc.add(new ErrorMessage("TODO can't read instructions, path:" + path, null));
		}
	}
	
	public Map<String, Map<String, Instruction>> getInstrByMn() {
		return instrByMn;
	}
	public Map<String, Instruction> getInstrById() {
		return instrById;
	}
}

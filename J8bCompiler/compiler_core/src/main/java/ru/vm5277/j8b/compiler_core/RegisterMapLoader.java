/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
26.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.RegisterMap;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.messages.ErrorMessage;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;

public class RegisterMapLoader {
	private	final	Map<String, RegisterMap>	map	= new HashMap<>();

	public RegisterMapLoader(String runtimePath, MessageContainer mc) throws ParseException {
		File file = new File(runtimePath + File.separator + "register.map");
		if (!file.exists()) {
			mc.add(new ErrorMessage("Register map file not found", null));
		}
		else {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				while(true) {
					String line = reader.readLine();
					if(null != line) {
						int commentPos = line.indexOf("#");
						if(-1 != commentPos) {
							line = line.substring(0, commentPos);
						}
						line = line.trim();
						if(!line.isEmpty()) {
							RegisterMap rm = new RegisterMap(line);
							map.put(rm.getMethodQName(), rm);
						}
					}
					else break;
				}
			}
			catch (FileNotFoundException ex) {
			}
			catch (IOException ex) {
				mc.add(new ErrorMessage("Register map read exception:" + ex.getMessage(), null));
			}
		}
	}
	
	public Map<String, RegisterMap> getMap() {
		return map;
	}
}
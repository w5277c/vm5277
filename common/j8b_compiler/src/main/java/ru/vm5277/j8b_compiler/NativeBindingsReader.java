/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.06.2025	konstantin@5277.ru			Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.NativeBinding;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class NativeBindingsReader {
	private					MessageContainer						mc;
	private					Path									runtimePath;
	private					Map<String, NativeBinding>				map		= new HashMap<>();
	
	public NativeBindingsReader(Path runtimePath, MessageContainer mc) {
		this.mc = mc;
		this.runtimePath = runtimePath;
		
		try(BufferedReader br = new BufferedReader(new FileReader(runtimePath.resolve("native_bindings.cfg").normalize().toFile()))) {
			while(true) {
				String line = br.readLine();
				if(null == line) break;
				int commentPos = line.indexOf("#");
				if(-1 != commentPos) {
					line = line.substring(0, commentPos);
				}
				line = line.trim();
				if(!line.isEmpty()) {
					try {
						NativeBinding nb = new NativeBinding(line);
						map.put(nb.getMethod(), nb);
					}
					catch(Exception e) {
						mc.add(new ErrorMessage(e.getMessage(), null));
					}
				}
			}
		}
		catch(Exception ex) {
			mc.add(new ErrorMessage("Cannot read native bindings map file:" + runtimePath, null));
		}
	}
	
	public NativeBinding get(String methodPath) {
		return map.get(methodPath);
	}
	
	public Map<String, NativeBinding> getMap() {
		return map;
	}
}

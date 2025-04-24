/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.List;
import ru.vm5277.j8b.compiler.enums.VarType;

public class MethodInfo {
	private	final	VarType			returnType;
	private	final	List<VarType>	parameters;

	public MethodInfo(VarType returnType, List<VarType> parameters) {
		this.returnType = returnType;
		this.parameters = parameters;
	}

	public VarType getReturnType() {
		return returnType;
	}
	
	public List<VarType> getParameters() {
		return parameters;
	}
}
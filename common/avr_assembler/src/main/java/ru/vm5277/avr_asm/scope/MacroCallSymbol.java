/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
08.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class MacroCallSymbol extends Symbol {
	private	final	List<Expression>			params;
	private	final	Map<String, VariableSymbol>	variables		= new HashMap<>();
	private	final	Map<String, Integer>		labels			= new HashMap<>();
	private			List<Node>					secondPassNodes;
	
	public MacroCallSymbol(String name, List<Expression> params) {
		super(name);
		
		this.params = params;
	}
	
	public List<Expression> getParams() {
		return params;
	}

	public void addLabel(String name, SourcePosition sp, int address) throws ParseException {
		if(labels.keySet().contains(name)) throw new ParseException("Label '" + name + "' already defined", sp);
		if(variables.keySet().contains(name)) throw new ParseException("Symbol '" + name + "' already defined as variable", sp);
		labels.put(name, address);
	}

	void addVariable(VariableSymbol variableSymbol, SourcePosition sp, int address) throws ParseException {
		String name = variableSymbol.getName();
		VariableSymbol vs = variables.get(name);
		if(null != vs && vs.isConstant()) throw new ParseException("TODO Нельзя переписать значение константы:" + name, sp);
		variables.put(name, vs);

	}

	public VariableSymbol resolveVariable(String name) {
		return variables.get(name);
	}

	public Integer resolveLabel(String name) {
		return labels.get(name);
	}

	public void setSecondPartNodes(List<Node> secondPassNodes) {
		this.secondPassNodes = secondPassNodes;
	}
	public List<Node> getSecondPartNodes() {
		return secondPassNodes;
	}
	
	@Override
	public String toString() {
		return name + ":" + params;
	}
}

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
package ru.vm5277.avr_asm.scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;

public class MacroCallSymbol extends Symbol {
	private	final	List<Expression>			params;
	private	final	Map<String, VariableSymbol>	variables		= new HashMap<>();
	private	final	Map<String, Integer>		labels			= new HashMap<>();
	private			List<Node>					secondPassNodes;
	
	public MacroCallSymbol(String name, List<Expression> params) {
		super(name.toLowerCase());
		
		this.params = params;
	}
	
	public List<Expression> getParams() {
		return params;
	}

	public void addLabel(String name, SourcePosition sp, int address) throws CompileException {
		String lcName = name.toLowerCase();
		if(labels.keySet().contains(lcName)) throw new CompileException("Label '" + name + "' already defined", sp);
		if(variables.keySet().contains(lcName)) throw new CompileException("Symbol '" + name + "' already defined as variable", sp);
		labels.put(lcName, address);
	}

	void addVariable(VariableSymbol variableSymbol, SourcePosition sp) throws CompileException {
		String lcName = variableSymbol.getName();
		VariableSymbol vs = variables.get(lcName);
		if(null != vs && vs.isConstant()) throw new CompileException("TODO Нельзя переписать значение константы:" + lcName, sp);
		variables.put(lcName, vs);

	}

	public VariableSymbol resolveVariable(String name) {
		return variables.get(name.toLowerCase());
	}

	public Integer resolveLabel(String name) {
		return labels.get(name.toLowerCase());
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

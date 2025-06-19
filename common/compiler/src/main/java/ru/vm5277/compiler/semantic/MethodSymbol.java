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
package ru.vm5277.compiler.semantic;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.compiler.VarType;

public class MethodSymbol extends Symbol {
	private	final	List<Symbol>	parameters;
	private			boolean			canThrow;
	private			String			signature;
	private	final	MethodScope		scope;
	
	public MethodSymbol(String name, VarType returnType, List<Symbol> parameters, boolean isFinal, boolean isStatic, boolean isNative, boolean canThrow,
						MethodScope scope) {
		super(name, returnType, isFinal, isStatic, isNative);
		
		this.parameters = parameters;
		this.canThrow = canThrow;
		this.scope = scope;
	}

	public MethodScope getScope() {
		return scope;
	}
	
	public List<Symbol> getParameters() {
		return parameters;
	}
	
	public List<VarType> getParameterTypes() {
		List<VarType> result = new ArrayList<>();
		for (Symbol param : parameters) {
			result.add(param.getType());
		}
		return result;
	}
	
	public boolean canThrow() {
		return canThrow;
	}
	
	public synchronized String getSignature() {
		if(null == signature) {
			StringBuilder sb = new StringBuilder();
			sb.append(name).append("(");
			for (Symbol param : parameters) {
				sb.append(param.getType().getName()).append(",");
			}
			if (!parameters.isEmpty()) {
				sb.setLength(sb.length() - 1); // Удаляем последнюю запятую
			}
			sb.append(")");
			signature = sb.toString();
		}
		return signature;
	}
}
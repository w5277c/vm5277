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

import java.util.List;
import ru.vm5277.common.exceptions.CompileException;

public class EnumScope extends CIScope {
	private	List<String>	values;
	
	public EnumScope(String name, Scope parent, List<String> values) throws CompileException {
		super(parent);
		
		if(parent instanceof EnumScope) {
			throw new CompileException("Enum " + name + " can only be declared within a class.");
		}
		
		if(values.isEmpty() || 255<values.size()) {
			throw new CompileException("Enum " + name + " values quantity out of range (1-256): " + values.size());
		}

		this.name = name;
		this.values = values;
	}
	
	public int getValueIndex(String value) {
		return values.indexOf(value);
	}
	
	public String getValue(int index) {
		if(values.size()>=index) return null;
		return values.get(index);
	}
	
	public int getSize() {
		return values.size();
	}
}

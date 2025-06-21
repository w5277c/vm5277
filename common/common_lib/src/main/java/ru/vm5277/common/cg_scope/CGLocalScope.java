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
package ru.vm5277.common.cg_scope;

import ru.vm5277.common.compiler.Operand;

public class CGLocalScope extends CGScope {
	private	final	int						typeId;
	private	final	int						size;
	private			boolean					isConstant;
	private			Operand					value;
			
	public CGLocalScope(CGScope parent, int id, int typeId, int size, boolean isConstant, String name) {
		super(parent, id, name);
		
		this.typeId = typeId;
		this.size = size;
		this.isConstant = isConstant;
	}
	
	public void setValue(Operand value) {
		this.value = value;
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	public Operand getValue() {
		return value;
	}
}

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

import ru.vm5277.common.compiler.Operand;
import ru.vm5277.common.compiler.VarType;

public class Symbol {
	protected			String	name;
	protected			VarType	type;
	protected			boolean	isStatic;
	protected			boolean	isFinal;
	protected			boolean	isNative;
	protected			int		runtimeId	= -1;
	protected			Operand	constOp;
	
	protected Symbol(String name) {
		this.name = name;
	}
	
	public Symbol(String name, VarType type) {
		this.name = name;
		this.type = type;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic, boolean isNative) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
		this.isNative = isNative;
	}

	public String getName() {
		return name;
	}
	
	public VarType getType() {
		return type;
	}
	public void setType(VarType type) {
		this.type = type;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public boolean isNative() {
		return isNative;
	}
	
	public void setRuntimeId(int runtimeId) {
		this.runtimeId = runtimeId;
	}
	public int getRuntimeId() {
		return runtimeId;
	}

	public void setConstantOperand(Operand op) {
		this.constOp = op;
	}
	public Operand getConstantOperand() {
		return constOp;
	}
}
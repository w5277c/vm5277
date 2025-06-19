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
package ru.vm5277.common.compiler;

public class Operand {
	private	int			Id;
	private	OperandType	opType;
	private	Object		value;
	
	public Operand(int typeId, OperandType opType, Object value) {
		this.Id = typeId;
		this.opType = opType;
		this.value = value;
	}
	
	public Operand(int Id, OperandType opType) {
		this.Id = Id;
		this.opType = opType;
		this.value = null;
	}

	public int getId() {
		return Id;
	}
	
	public OperandType getOperandType() {
		return opType;
	}	
	
	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return opType + "[id:" + Id + "]" + (null == value ? "" : "=" + value);
	}
	
}

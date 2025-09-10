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
package ru.vm5277.common.cg;

import ru.vm5277.common.StrUtils;

public class CGCells {
	public static enum Type {
		REG,			//регистры
		ACC,			//как и регистры, но ids и size не заполнены
		STACK_FRAME,	//блок памяти выделенный в стеке
		ARGS,			//аргументы метода, лежащие в начале стека(размер STACK_FRAME известен только на финальном этапе построения кода метода)
		STAT,			//глобальный блок памяти выделенный под статические переменные
		HEAP,			//блок памяти выделенный в инстансе класса
		STACK;			//значение лежащее на вершине стека

//		REF;	//адрес на объект(класс, массив)
	}
	
	private	Type	type;
	private	int[]	ids;
	private	int		size;
	private	Object	obj;
	private	boolean	isRef;
	
	public CGCells(Type type) {
		this.type = type;
	}

	public CGCells(Type type, int size) {
		this.type = type;
		this.size = size;
	}
	
	public CGCells(Type type, int size, int offset) {
		this.type = type;
		ids = new int[size];
		this.size = size;
		for(int i=0; i<size; i++) {
			ids[i] = offset+i;
		}
	}

	public CGCells(Type type, byte[] ids) {
		this.type = type;
		this.ids = new int[ids.length];
		this.size = ids.length;
		for(int i=0; i<ids.length; i++) {
			this.ids[i] = ids[i];
		}
	}
	
	public CGCells(RegPair[] regPairs) {
		this.type = Type.REG;
		this.ids = new int[regPairs.length];
		this.size = regPairs.length;
		for(int i=0; i<regPairs.length; i++) {
			ids[i] = regPairs[i].getReg();
		}
	}
	
	public Type getType() {
		return type;
	}
	
	public int getId(int i) {
		return ids[i];
	}
	
	public int getSize() {
		return size;
	}
	
	public void setObject(Object obj) {
		this.obj = obj;
	}
	public Object getObject() {
		return obj;
	}
	
	@Override
	public String toString() {
		return type + "[" + (Type.STACK == type ? "x" + size : StrUtils.toString(ids)) + "]";
	}
}

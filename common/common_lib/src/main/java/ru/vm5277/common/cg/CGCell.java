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

public class CGCell {
	public static enum Type {
		REG,	//регистры
		STACK,	//блок памяти выделенный в стеке
		STAT,	//глобальный блок памяти выделенный под статические переменные
		HEAP,	//блок памяти выделенный в инстансе класса
		RET;	//результат возвращенный методом, лежит за верхушкой стека
	}
	
	private	Type	type;
	private	int		num;
	
	public CGCell(Type type, int num) {
		this.type = type;
		this.num = num;
	}
	
	public Type getType() {
		return type;
	}
	
	public int getNum() {
		return num;
	}
	
	@Override
	public String toString() {
		return type + ":" + num;
	}
}

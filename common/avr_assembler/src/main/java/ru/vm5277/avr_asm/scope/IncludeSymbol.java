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

import java.util.Stack;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CompileException;

public class IncludeSymbol extends Symbol {
	private					boolean									hasTrue; //Блок для выполнения уже был
	private					Stack<Boolean>							blockStates		= new Stack<>(); // Состояния вложенных блоков
	private					Stack<Boolean>							hasTrueStack	= new Stack<>();
	private					IncludeSymbol							parent;
	
	public IncludeSymbol(String name, IncludeSymbol parent) {
		super(name.toLowerCase());
		
		this.parent = parent;
	}

	public void blockStart(boolean isTrue, SourcePosition sp) {
		if(!blockStates.isEmpty()) {
			hasTrueStack.add(hasTrue); // Запоминаем признак наличия предыдущего выполняемого блока (если блоки есть)
		}
		blockStates.add(isTrue); // Помещаем состояние нового блока
		hasTrue = false;
	}

	public void blockElse(SourcePosition sp) throws CompileException {
		blockElseIf(true, sp); //Используем метод blockElseIf с true параметром
	}

	public void blockElseIf(boolean isTrue, SourcePosition sp) throws CompileException {
		if(blockStates.isEmpty()) throw new CompileException("ELSE/ELSEIF directive without matching block", sp);
		
		hasTrue |= blockStates.peek(); // true - был выполняемый блок
		
		if(!isTrue) return; // Ничего не делаем, если условие ложно
		
		if(!hasTrue) { // Ничего не делаем если выполняемый блок уже есть
			blockStates.pop(); // Иначе заменяем последний на выполняемый
			blockStates.add(true);
		}
	}

	public int getBlockCntr() {
		return  blockStates.size();
	}

	public void blockEnd(SourcePosition sp) throws CompileException {
		if(blockStates.isEmpty()) throw new CompileException("END directive without matching block", sp);

		blockStates.pop();
		if(!hasTrueStack.isEmpty()) {
			hasTrue = hasTrueStack.pop(); // Если блоки есть - восстанавливаем признак наличия выполняемого блока
		}
	}
	
	public boolean isTrue() throws CompileException {
		if(blockStates.isEmpty()) return true;
		if(hasTrue) return false;
		
		boolean result = true;
		for(Boolean state : blockStates) {
			result &= state;
		}
		return result;
	}
	
	public boolean hasTrue() {
		return hasTrue;
	}
	
	public IncludeSymbol getParent() {
		return parent;
	}
	
	public String debugInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("hasTrue:").append(hasTrue);
		sb.append(", blockStates:[");
		for(int i=0; i<blockStates.size(); i++) {
			if(i>0) sb.append(",");
			sb.append(blockStates.get(i));
		}
		sb.append("]");
		return sb.toString();
	}
}

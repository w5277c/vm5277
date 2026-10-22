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
	private					Stack<Boolean>							states		= new Stack<>(); // Состояния вложенных блоков
	private					Stack<Boolean>							executed	= new Stack<>();
	private					IncludeSymbol							parent;
	
	public IncludeSymbol(String name, IncludeSymbol parent) {
		super(name.toLowerCase());
		
		this.parent = parent;
	}

	public void blockStart(boolean isTrue, SourcePosition sp) {
		executed.add(false);
		states.add(isTrue); // Помещаем состояние нового блока
	}

	public void blockElse(SourcePosition sp) throws CompileException {
		blockElseIf(true, sp); //Используем метод blockElseIf с true параметром
	}

	public void blockElseIf(boolean isTrue, SourcePosition sp) throws CompileException {
		if(states.isEmpty()) throw new CompileException("ELSE/ELSEIF directive without matching block", sp);
		
		if(executed.peek() || states.peek()) {
			executed.pop();
			executed.add(true);
		}
		
		if(!isTrue) return; // Ничего не делаем, если условие ложно
		
		if(!executed.peek()) { // Ничего не делаем если выполняемый блок уже есть
			states.pop(); // Иначе заменяем последний на выполняемый
			states.add(true);
		}
	}

	public int getBlockCounter() {
		return  states.size();
	}

	public void blockEnd(SourcePosition sp) throws CompileException {
		if(states.isEmpty()) throw new CompileException("END directive without matching block", sp);

		states.pop();
		executed.pop();
	}
	
	public boolean isTrue() throws CompileException {
		if(states.isEmpty()) return true;

		for(Boolean e : executed) {
			if(e) return false;
		}

		boolean result = true;
		for(Boolean state : states) {
			result &= state;
		}
		return result;
	}
	
	public IncludeSymbol getParent() {
		return parent;
	}
	
	public String debugInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("executed:[");
		for(int i=0; i<executed.size(); i++) {
			if(i>0) sb.append(",");
			sb.append(executed.get(i));
		}
		sb.append("], blockStates:[");
		for(int i=0; i<states.size(); i++) {
			if(i>0) sb.append(",");
			sb.append(states.get(i));
		}
		sb.append("]");
		return sb.toString();
	}
}

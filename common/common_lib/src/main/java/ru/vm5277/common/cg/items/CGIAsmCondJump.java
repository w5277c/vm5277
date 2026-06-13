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
package ru.vm5277.common.cg.items;

import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.exceptions.CompileException;

// Этот элемент особенный - он может состоять из нескольких элементов в итоговом листинге, хотя для всех этапов кроме последнего он выглядит как один элемент
// Уникальность в необходимости расширения диапазона перехода, если метка условного перехода недосягаема.
// В этом случае необходимо инвертировать условный переход с переходом на PC+2, где следующей(пропускаемой) инструкцией будет прыжок(JMP) на недосягаемую ранее метку
// null!=jumpInstr говорит о том, что метка не досягаема и при формировании итоговых инструкций нужно учитывать расширенный вариант.

public class CGIAsmCondJump extends CGIAsm {
	private	CGIAsmJump jumpInstr;

	public CGIAsmCondJump(String instr, CGLabelScope lbScope) throws CompileException {
		super(instr, lbScope.getName());
	}

	public CGIAsmCondJump(String instr, String labelName) throws CompileException {
		super(instr, labelName);
	}

	public String getLabelName() {
		return postfix;
	}
	public void setLabelName(String labelName) {
		this.postfix = labelName;
	}
	
	public void requireExpansion(String invertedInstr, String postfix, CGIAsmJump jumpInstr, int jumpInstrSize) throws CompileException {
		if(null!=this.jumpInstr) throw new CompileException(toString() + " expansion already required");
		
		this.instr = invertedInstr;
		this.postfix = postfix;
		this.jumpInstr = jumpInstr;
		
		setSizeInBytes(getSizeInBytes() + jumpInstrSize);
	}
	public boolean isExpansionRequired() {
		return null!=jumpInstr;
	}
	
	public CGIAsmJump getJumpInstr() {
		return jumpInstr;
	}
	
	@Override
	public String getSource() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.getSource());
		if(null!=jumpInstr) {
			sb.append(jumpInstr.getSource());
		}
		return sb.toString();
	}
}

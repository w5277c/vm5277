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

import ru.vm5277.common.exceptions.CompileException;

public class CGIAsm extends CGItem {
	protected	String	instr;
	protected	String	postfix;
	protected	Integer	sizeInBytes	= null;
	
	public CGIAsm(String instr) throws CompileException {
		if(null==instr) {
			throw new CompileException("Asm instruction is null");
		}
		this.instr = instr.toLowerCase();
	}

	public CGIAsm(String instr, String postfix) throws CompileException {
		if(null==instr) {
			throw new CompileException("Asm instruction is null");
		}
		this.instr = instr.toLowerCase();
		this.postfix = (null==postfix ? null : postfix.toLowerCase());
	}
	
	public String getInstr() {
		return instr;
	}
	public void setInstr(String instr) throws CompileException {
		if(null==instr) {
			throw new CompileException("Asm instruction is null");
		}
		this.instr = instr.toLowerCase();
	}
	
	public String getPostfix() {
		return postfix;
	}
	public void setPostfix(String postfix) {
		this.postfix = (null==postfix ? null : postfix.toLowerCase());
	}

	public String getText() {
		return instr + (null==postfix||postfix.isEmpty() ? "" : " " + postfix);
	}
	
	public void setSizeInBytes(int sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}
	public Integer getSizeInBytes() {
		return sizeInBytes;
	}
	
	@Override
	public String getSource() {
		return "\t" + getText() + "\n";
	}
	
	@Override
	public String toString() {
		return getText();
	}
}

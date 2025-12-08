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

import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.exceptions.CompileException;

public class CGIAsmIReg extends CGIAsm {
	private CGIContainer	prefCont	= new CGIContainer();
	private CGIContainer	postCont	= new CGIContainer();
	private	char			ireg;
	private	String			prefix;
	private	String			postfix;
	private	int				offset;
	private	CGBlockScope	curBScope;
	private	CGBlockScope	varBScope;
	private	boolean			isArg;
	
	public CGIAsmIReg(char ireg, String instr, String prefix, int offset, String postfix) throws CompileException {
		super(instr, null);
		
		this.ireg = ireg;
		this.prefix = (null==prefix ? null : prefix.toLowerCase());
		this.offset = offset;
		this.postfix = (null==postfix ? null : postfix.toLowerCase());
	}

	public CGIAsmIReg(char ireg, String instr, String prefix, int offset, String postfix, CGBlockScope curBScope, CGBlockScope varBScope, boolean isArg)
																																	throws CompileException {
		super(instr, null);
		this.ireg = ireg;
		this.prefix = (null==prefix ? null : prefix.toLowerCase());
		this.offset = offset;
		this.postfix = (null==postfix ? null : postfix.toLowerCase());
		this.curBScope = curBScope;
		this.varBScope = varBScope;
		this.isArg = isArg;
	}

	public char getIreg() {
		return ireg;
	}
	
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	
	public boolean isArg() {
		return isArg;
	}
	
	public CGBlockScope getCurBlockScope() {
		return curBScope;
	}
	
	public CGBlockScope getVarBlockScope() {
		return varBScope;
	}

	public CGIContainer getPrefCont() {
		return prefCont;
	}

	public CGIContainer getPostCont() {
		return postCont;
	}
	
	@Override
	public String getText() {
		return instr + " " + prefix + offset + postfix;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if(!prefCont.isEMpty()) {
			result.append(prefCont.getSource()).append("\n");
		}
		result.append(prefix).append(offset).append(postfix).append("\n");
		result.append("[").append(ireg).append("]");
		if(isArg) {
			result.append("[ARG]");
		}
		if(!postCont.isEMpty()) {
			result.append(postCont.getSource()).append("\n");
		}
		return result.toString();
	}
}

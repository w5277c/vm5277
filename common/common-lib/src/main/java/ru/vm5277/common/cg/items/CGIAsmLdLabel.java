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

public class CGIAsmLdLabel extends CGIAsm {
	private	String	reg;
	
	public CGIAsmLdLabel(String instr, String reg, String postfix) throws CompileException {
		super(instr, null);
		this.reg = reg;
		this.postfix = postfix;
	}

	public String getLabelName() {
		return postfix;
	}
	public void setLabelName(String labelName) {
		this.postfix = labelName;
	}
	
	@Override
	public String getText() {
		return instr + " " + reg + "," + postfix;
	}
}

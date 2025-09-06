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

public class CGIAsmJump extends CGIAsm {
	private	String	labelName;
	
	public CGIAsmJump(String instr, CGLabelScope lbScope) {
		super(instr);
		this.labelName = lbScope.getName();
	}

	public CGIAsmJump(String instr, String labelName) {
		super(instr);
		this.labelName = labelName;
	}

	public String getLabelName() {
		return labelName;
	}
	public void setLabelName(String labelName) {
		this.labelName = labelName;
	}
	
	@Override
	public String getSource() {
		return "\t" + text + " " + labelName + "\n";
	}
	
	@Override
	public String toString() {
		return text + " " + labelName;
	}
}

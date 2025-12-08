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

public class CGIAsmMv extends CGIAsm {
	private		String dstReg;
	private		String srcReg;
	
	public CGIAsmMv(String instr, String dstReg, String srcReg) throws CompileException {
		super(instr, null);
		
		this.dstReg = dstReg;
		this.srcReg = srcReg;
	}

	public String getDstReg() {
		return dstReg;
	}
	public void setDstReg(String dstReg) {
		this.dstReg = (null==dstReg ? null : dstReg.toLowerCase());
	}

	public String getSrcReg() {
		return srcReg;
	}
	public void setSrcReg(String srcReg) {
		this.srcReg = (null==srcReg ? null : srcReg);
	}
	
	@Override
	public String getText() {
		return instr + " " + dstReg + "," + srcReg;
	}
}

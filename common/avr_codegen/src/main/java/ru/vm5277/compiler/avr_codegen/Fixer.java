/*
 * Copyright 2025 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0eunary
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.compiler.avr_codegen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.InstrReader;
import ru.vm5277.avr_asm.Instruction;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeFixer;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCondJump;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;

public class Fixer extends CodeFixer {
	private	static final int JMP_INSTR_SIZE	= 0x04;
	
	@Override
	public List<CGItem> resolveInstructionsSize(CGScope scope, Path instrPath, MessageContainer mc, String mcu) throws CompileException {
		InstrReader instrReader = new InstrReader(instrPath, mc);
		instrReader.setMCU(mcu);
		
		List<CGItem> list = new ArrayList();
		CodeGenerator.treeToList(scope, list);
		
		for(CGItem item : list) {
			if(item instanceof CGIAsm) {
				CGIAsm asmItem = (CGIAsm)item;
				Map<String, Instruction> instructions = instrReader.getInstrByMn().get(asmItem.getInstr());
				if(null==instructions || instructions.isEmpty()) {
					throw new CompileException("COMPILER ERROR: unknown AVR assembler instruction:" + asmItem.getText());
				}
				asmItem.setSizeInBytes(instructions.values().iterator().next().getWSize()*0x02);
			}
		}
		
		return list;
	}

	@Override
	public void branchRangeFixer(List<CGItem> list) throws CompileException {
		Map<String, Integer> labelsMap = new HashMap<>();
		
		boolean modified = true;
		while(modified) {
			modified = false;
			labelsMap.clear();
			
			List<CGIAsm> asmList = new ArrayList<>();
			int offset = 0;
			for(CGItem item : list) {
				if(!item.isDisabled()) {
					if(item instanceof CGLabelScope) {
						labelsMap.put(((CGLabelScope)item).getName(), offset);
					}
					else if(item instanceof CGIAsm) {
						offset+=((CGIAsm)item).getSizeInBytes();
						asmList.add((CGIAsm)item);
					}
				}
			}
			
			offset = 0;
			for(CGItem item : new ArrayList<>(asmList)) {
				if(!item.isDisabled() && item instanceof CGIAsm) {
					if(item instanceof CGIAsmCondJump) {
						CGIAsmCondJump acj = (CGIAsmCondJump)item;
						Integer labelOffset = labelsMap.get(acj.getLabelName());
						if(null!=labelOffset) {
							int wDelta = (labelOffset-offset);
							if(wDelta<-64 || wDelta>63) {
								CGLabelScope skipLbScope = new CGLabelScope(null, CodeGenerator.genId(), LabelNames.SKIP, true);
								acj.setInstr(Utils.brInstrInvert(acj.getInstr()));
								acj.setLabelName(skipLbScope.getName());
								int index = list.indexOf(acj);
								CGIAsm aij = new CGIAsmJump("jmp", acj.getLabelName(), false);
								aij.setSizeInBytes(JMP_INSTR_SIZE);
								list.add(index, aij);
								list.add(index+1, skipLbScope);

								modified = true;
							}
						}
					}

					offset+=((CGIAsm)item).getSizeInBytes();
				}
			}
		}
	}
}

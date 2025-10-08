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

package ru.vm5277.compiler.avr_codegen;

import java.util.ArrayList;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;

//TODO удалять двойные rjmp, но перед ними проверять прыжки вида pc+/- и инструкции порпуска типа sbrc. Прыжки лучше добавлять в специальный контейнер, внутри
//котрого такие оптимизации не будут применяться


public class Optimizer extends CodeOptimizer {
	@Override
	public void optimizeBranchChains(CGScope scope) {
		ArrayList<CGItem> list = new ArrayList();

		boolean changed = true;
		while(changed) {
			list.clear();
			
			optimizeEmptyJumps(scope);
			CodeGenerator.treeToList(scope, list);		

			changed = false;
			optimizeBranchChainsLoopLabel:
			for(int i=0; i<list.size()-2; i++) {
				CGItem item1 = list.get(i);
				CGItem item2 = list.get(i+1);
				CGItem item3 = list.get(i+2);
				if(item1 instanceof CGIAsmJump && item2 instanceof CGIAsmJump && item3 instanceof CGLabelScope) {
					CGIAsmJump j1 = (CGIAsmJump)item1;
					CGIAsmJump j2 = (CGIAsmJump)item2;
					String name = ((CGLabelScope)item3).getName();

					if(j1.getText().toLowerCase().startsWith("br") && j2.getText().equalsIgnoreCase("rjmp") && j1.getLabelName().equalsIgnoreCase(name)) {
						j1.setText(Utils.brInstrInvert(j1.getText()));
						j1.setLabelName(j2.getLabelName());
						j2.disable();
						changed = true;
					}
				}
			}
		}
	}

	@Override
	public void optimizeBaseInstr(CGScope scope) {
		ArrayList<CGItem> list = new ArrayList();
		CodeGenerator.treeToList(scope, list);
		optimizeBaseInstrLoopLabel:
		for(int i=0; i<list.size()-1; i++) {
			CGItem item1 = list.get(i);
			if(item1.isDisabled() || !(item1 instanceof CGIAsm)) {
				continue;
			}
			CGItem item2 = list.get(i+1);
			while(item2.isDisabled()) {
				i++;
				if(i==list.size()-1) break optimizeBaseInstrLoopLabel;
				item2 = list.get(i+1);
			}
			if(!(item2 instanceof CGIAsm)) continue;
			
			String instr1 = replaceAliasToReg(((CGIAsm)item1).getText().trim().toLowerCase());
			String instr2 = replaceAliasToReg(((CGIAsm)item2).getText().trim().toLowerCase());
			if(instr1.startsWith("ldi r")) {
				if(instr2.startsWith("ldi r")) {
					int pos1 = instr1.indexOf(',');
					int dst1 = Integer.parseInt(instr1.substring(5, pos1));
					int pos2 = instr2.indexOf(',');
					int dst2 = Integer.parseInt(instr2.substring(5, pos2));
					if(dst1==dst2) {
						item1.disable();
					}
					continue;
				}

				Integer k = null;
				int pos = instr1.indexOf(',');
				byte reg = Byte.parseByte(instr1.substring(5, pos));
				try{k = Integer.parseInt(instr1.substring(pos+1));}catch(Exception ex) {}

				if(null == k) {
					pos = instr1.indexOf(",low(");
					if(-1!=pos) {
						try{k = Integer.parseInt(instr1.substring(pos+5, instr1.length()-1))&0xff;}catch(Exception ex) {}
					}
					else {
						pos = instr1.indexOf(",high(");
						if(-1!=pos) {
							try{k = (Integer.parseInt(instr1.substring(pos+6, instr1.length()-1))>>0x08)&0xff;}catch(Exception ex) {}
						}
					}
				}

				if(null != k && (k==0x00 || k==0x01 || k==0xff)) {
					CGIAsm asmInstr = (CGIAsm)item2;
					String kStr = "c0x" + (0==k ? "00" : (1==k ? "01" : "ff"));
					if((instr2.startsWith("st ") || instr2.startsWith("std ") || instr2.startsWith("sts ")) && instr2.endsWith(",r"+reg)) {
						item1.disable();
						pos = instr2.indexOf(',');
						asmInstr.setText(instr2.substring(0, pos) + "," + kStr);
					}
					else if(instr2.startsWith("push r")) {
						int reg2 = Integer.parseInt(instr2.substring(6));
						//TODO Опасно, может удалить корректный код! Нужно код, сохраняющий значения в стек обернуть конетйнером, а здесь научить метод
						//отслеживать такие контейнеры
						if(reg==reg2) {
							item1.disable();
							asmInstr.setText("push " + kStr);
						}
					}
				}
			}
			else if(instr1.startsWith("mov r") && instr2.startsWith("mov r") && instr1.contains(",r") && instr2.contains(",r")) {
				int pos = instr1.indexOf(',');
				int dst1 = Integer.parseInt(instr1.substring(5, pos));
				int src1 = Integer.parseInt(instr1.substring(pos+2));
				pos = instr1.indexOf(',');
				int dst2 = Integer.parseInt(instr2.substring(5, pos));
				int src2 = Integer.parseInt(instr2.substring(pos+2));
				if(0==dst1%2 && dst2==dst1+1 && 0==src1%2 && src2==src1+1) {
					item1.disable();
					((CGIAsm)item2).setText("movw r" + dst1 + ",r" + src1);
				}
			}
			else if(instr1.startsWith("subi r") && instr2.startsWith("sbci r")) {
				int pos1 = instr1.indexOf(',');
				int dst1 = Integer.parseInt(instr1.substring(6, pos1));
				int pos2 = instr2.indexOf(',');
				int dst2 = Integer.parseInt(instr2.substring(6, pos2));
				//TODO добавить проверку на записи вида rXX
				if((26==dst1 && 27==dst2) || (28==dst1 && 29==dst2) || (30==dst1 && 31==dst2)) {
					Integer cnst=null;
					Integer hConst=null;
					Integer lConst=null;
					int pos = instr1.indexOf("low");
					if(-1!=pos) {
						try {cnst = Integer.parseInt(instr1.substring(pos+4, instr1.length()-1));}catch(Exception ex){}
					}
					else {
						try {lConst = Integer.parseInt(instr1.substring(pos1+1));}catch(Exception ex){}
					}
					if(null==cnst && null!=lConst) {
						pos = instr2.indexOf("high");
						if(-1==pos) {
							try {hConst = Integer.parseInt(instr2.substring(pos2+1));}catch(Exception ex){}
						}
					}
					if(null == cnst && null != lConst && null != hConst) {
						cnst = hConst * 256 + lConst; //TODO проверить
					}
					if(null != cnst && 0>=cnst && -64<cnst) {
						item1.disable();
						((CGIAsm)item2).setText("adiw r" + dst1 + ","+ cnst*(-1));
					}
				}
			}
			else if(instr1.startsWith("adiw r") && instr2.startsWith("adiw r")) {
				int pos = instr1.indexOf(',');
				int dst1 = Integer.parseInt(instr1.substring(6, pos));
				Integer k1 = null;
				try{k1 = Integer.parseInt(instr1.substring(pos+1));}catch(Exception ex){};
				pos = instr2.indexOf(',');
				int dst2 = Integer.parseInt(instr2.substring(6, pos));
				Integer k2 = null;
				try{k2 = Integer.parseInt(instr2.substring(pos+1));}catch(Exception ex){};
				if(null!=k1 && null!=k2 && dst1==dst2 && (k1+k2)<64) {
					item1.disable();
					((CGIAsm)item2).setText("adiw r" + dst1 + ","+ (k1+k2));
				}
			}
		}		
	}

	private String replaceAliasToReg(String instr) {
		return	instr.replaceAll("[xX][lL]", "r26").replaceAll("[xX][hH]", "r27").replaceAll("[yY][lL]", "r28").replaceAll("[yY][hH]", "r29")
				.replaceAll("[zZ][lL]", "r30").replaceAll("[zZ][hH]", "r31").replaceAll("result", "r20").replaceAll("flags", "r21")
				.replaceAll("accum_l", "r16").replaceAll("accum_h", "r17").replaceAll("accum_el", "r18").replaceAll("accum_eh", "r19")
				.replaceAll("temp_l", "r24").replaceAll("temp_h", "r25").replaceAll("temp_el", "r22").replaceAll("temp_eh", "r23");
	}
}

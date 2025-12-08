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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.items.CGIAsm;
import ru.vm5277.common.cg.items.CGIAsmCall;
import ru.vm5277.common.cg.items.CGIAsmCondJump;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLd;
import ru.vm5277.common.cg.items.CGIAsmMv;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.exceptions.CompileException;

//TODO удалять двойные rjmp, но перед ними проверять прыжки вида pc+/- и инструкции порпуска типа sbrc. Прыжки лучше добавлять в специальный контейнер, внутри
//котрого такие оптимизации не будут применяться


public class Optimizer extends CodeOptimizer {
	private	static final int RJMP_INSTR_SIZE	= 0x02;
	
	@Override
	public boolean optimizeJumpInstr(List<CGItem> list) throws CompileException {
		boolean result = false;
		Map<String, Integer> labelsMap = new HashMap<>();
		
		boolean changed = true;
		while(changed) {
			changed = false;
			labelsMap.clear();
			
			int offset = 0;
			for(CGItem item : list) {
				if(!item.isDisabled()) {
					if(item instanceof CGLabelScope) {
						labelsMap.put(((CGLabelScope)item).getName(), offset);
					}
					else if(item instanceof CGIAsm) {
						offset+=((CGIAsm)item).getSizeInBytes();
					}
				}
			}
			
			offset = 0;
			for(CGItem item : list) {
				if(!item.isDisabled() && item instanceof CGIAsm) {
					if(item instanceof CGIAsmJump) {
						CGIAsmJump aj = (CGIAsmJump)item;
						if(!aj.isExternal() && aj.getInstr().equals("jmp")) {
							Integer labelOffset = labelsMap.get(aj.getLabelName());
							if(null!=labelOffset) {
								int delta = offset-labelOffset;
								if(delta>=-2048 && delta<=2047) {
									aj.setInstr("rjmp");
									aj.setSizeInBytes(RJMP_INSTR_SIZE);
									changed = true;
									result = true;
								}
							}
						}
					}
					else if(item instanceof CGIAsmCall) {
						CGIAsmCall ac = (CGIAsmCall)item;
						if(!ac.isExternal() && ac.getInstr().equals("call")) {
							Integer labelOffset = labelsMap.get(ac.getLabelName());
							if(null!=labelOffset) {
								int delta = offset-labelOffset;
								if(delta>=-2048 && delta<=2047) {
									ac.setInstr("rcall");
									ac.setSizeInBytes(RJMP_INSTR_SIZE);
									changed = true;
									result = true;
								}
							}
						}
					}

					offset+=((CGIAsm)item).getSizeInBytes();
				}
			}
		}
		
		return result;
	}

	@Override
	public boolean optimizeBranchInstr(List<CGItem> list) throws CompileException {
		boolean result = false;
		Map<String, Integer> labelsMap = new HashMap<>();
		
		boolean changed = true;
		while(changed) {
			changed = false;
			labelsMap.clear();
			
			List<CGItem> asmList = new ArrayList<>();
			int offset = 0;
			for(CGItem item : list) {
				if(!item.isDisabled()) {
					if(item instanceof CGLabelScope) {
						labelsMap.put(((CGLabelScope)item).getName(), offset);
						asmList.add(item);
					}
					else if(item instanceof CGIAsm) {
						offset+=((CGIAsm)item).getSizeInBytes();
						asmList.add(item);
					}
				}
			}
			
			offset = 0;
			for(int i=0; i<asmList.size()-2; i++) {
				CGItem item1 = list.get(i);
				CGItem item2 = list.get(i+1);
				CGItem item3 = list.get(i+2);
				if(item1 instanceof CGIAsmCondJump && item2 instanceof CGIAsmJump && item3 instanceof CGLabelScope) {
					CGIAsmCondJump acj = (CGIAsmCondJump)item1;
					CGIAsmJump aj = (CGIAsmJump)item2;
					String name = ((CGLabelScope)item3).getName();
					if(!aj.isExternal() && acj.getLabelName().equals(name)) {
						Integer labelOffset = labelsMap.get(aj.getLabelName());
						if(null!=labelOffset) {
							int wDelta = (labelOffset-offset)/2;
							if(wDelta>=-64 && wDelta<=63) {
								acj.setInstr(Utils.brInstrInvert(acj.getInstr()));
								acj.setLabelName(aj.getLabelName());
								aj.disable();
								//item3 нельзя блокировать, эта метка может быть использована в других местах.
								changed = true;
								result = true;
							}
						}
					}
					i+=2;
					offset+=aj.getSizeInBytes() + ((CGIAsm)item1).getSizeInBytes();
				}
				else if(item1 instanceof CGIAsm) {
					offset+=((CGIAsm)item1).getSizeInBytes();
				}
			}
		}
		
		return result;
	}

/*	@Override
	public void optimizeBranchChains(CGScope scope) throws CompileException {
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
				if(item1 instanceof CGIAsmCondJump && item2 instanceof CGIAsmJump && item3 instanceof CGLabelScope) {
					CGIAsmCondJump j1 = (CGIAsmCondJump)item1;
					CGIAsmJump j2 = (CGIAsmJump)item2;
					String name = ((CGLabelScope)item3).getName();

					if(!j2.isExternal() && j1.getLabelName().equals(name)) {
						j1.setLabelName(Utils.brInstrInvert(j1.getInstr()));
						j1.setLabelName(j2.getLabelName());
						j2.disable();
						changed = true;
					}
				}
			}
		}
	}
*/
	@Override
	public void optimizeBaseInstr(CGScope scope) throws CompileException {
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
			
			CGIAsm ai1 = (CGIAsm)item1;
			CGIAsm ai2 = (CGIAsm)item2;
			
			if(ai1 instanceof CGIAsmMv && ai2 instanceof CGIAsmMv) {
				CGIAsmMv am1 = (CGIAsmMv)item1;
				CGIAsmMv am2 = (CGIAsmMv)item2;
				int dstReg1 = Integer.parseInt(replaceAliasToReg(am1.getDstReg()).substring(1));
				int srcReg1 = Integer.parseInt(replaceAliasToReg(am1.getSrcReg()).substring(1));
				int dstReg2 = Integer.parseInt(replaceAliasToReg(am2.getDstReg()).substring(1));
				int srcReg2 = Integer.parseInt(replaceAliasToReg(am2.getSrcReg()).substring(1));

				if(0==dstReg1%2 && dstReg2==dstReg1+1 && 0==srcReg1%2 && srcReg2==srcReg1+1) {
					((CGIAsmMv)item1).setInstr("movw");
					((CGIAsmMv)item1).setDstReg("r"+dstReg1);
					((CGIAsmMv)item1).setSrcReg("r"+srcReg1);
					item2.disable();
					continue;
				}
			}

			if(ai1 instanceof CGIAsmLd) {
				CGIAsmLd al1 = (CGIAsmLd)item1;

				Integer k = null;
				byte reg = Byte.parseByte(replaceAliasToReg(al1.getReg()).substring(1));
				try{
					k = Integer.parseInt(al1.getConstant());
				}
				catch(Exception ex) {}

				if(null==k) {
					int pos = al1.getConstant().indexOf(",low(");
					if(-1!=pos) {
						try{
							k = Integer.parseInt(al1.getConstant().substring(pos+5, al1.getConstant().length()-1))&0xff;
						}
						catch(Exception ex) {}
					}
					else {
						pos = al1.getConstant().indexOf(",high(");
						if(-1!=pos) {
							try{
								k = (Integer.parseInt(al1.getConstant().substring(pos+6, al1.getConstant().length()-1))>>0x08)&0xff;
							}
							catch(Exception ex) {}
						}
					}
				}

				if(null!=k && (k==0x00||k==0x01||k==0xff)) {
					String kStr = "c0x" + (0==k ? "00" : (1==k ? "01" : "ff"));
					if((ai2.getInstr().equals("st") || ai2.getInstr().equals("std") || ai2.getInstr().equals("sts")) &&
						ai2.getPostfix().endsWith(",r"+reg)) {

						ai1.disable();
						int pos = ai2.getPostfix().indexOf(',');
						ai2.setPostfix(ai2.getPostfix().substring(0, pos) + "," + kStr);
					}
					else if(ai2.getInstr().equals("push") && ai2.getPostfix().startsWith("r")) {
						int reg2 = Integer.parseInt(ai2.getPostfix().substring(1));
						//TODO Опасно, может удалить корректный код! Нужно код, сохраняющий значения в стек обернуть конетйнером, а здесь научить метод
						//отслеживать такие контейнеры
						if(reg==reg2) {
							ai1.disable();
							ai2.setPostfix(kStr);
						}
					}
				}
				continue;
			}
			
			if(ai1.getInstr().equals("subi") && ai2.getInstr().equals("sbci") && ai1.getPostfix().startsWith("r") && ai2.getPostfix().startsWith("r")) {
				int pos1 = ai1.getPostfix().indexOf(',');
				int dstReg1 = Integer.parseInt(ai1.getPostfix().substring(1, pos1));
				int pos2 = ai2.getPostfix().indexOf(',');
				int dstReg2 = Integer.parseInt(ai2.getPostfix().substring(1, pos2));
				//TODO добавить проверку на записи вида rXX
				if((26==dstReg1 && 27==dstReg2) || (28==dstReg1 && 29==dstReg2) || (30==dstReg1 && 31==dstReg2)) {
					Integer cnst=null;
					Integer hConst=null;
					Integer lConst=null;
					int pos = ai1.getPostfix().indexOf("low");
					if(-1!=pos) {
						try {
							cnst = Integer.parseInt(ai1.getPostfix().substring(pos+4, ai1.getPostfix().length()-1));
						}
						catch(Exception ex){}
					}
					else {
						try {
							lConst = Integer.parseInt(ai1.getPostfix().substring(pos1+1));
						}
						catch(Exception ex){}
					}
					if(null==cnst && null!=lConst) {
						pos = ai2.getPostfix().indexOf("high");
						if(-1==pos) {
							try {
								hConst = Integer.parseInt(ai2.getPostfix().substring(pos2+1));
							}
							catch(Exception ex){}
						}
					}
					if(null==cnst && null!=lConst && null!=hConst) {
						cnst = hConst * 256 + lConst; //TODO проверить
					}
					if(null!=cnst && 0>=cnst && -64<cnst) {
						ai1.disable();
						ai2.setInstr("adiw");
						ai2.setPostfix("r" + dstReg1 + ","+ cnst*(-1));
					}
				}
				continue;
			}

			if(ai1.getInstr().equals("adiw") && ai2.getInstr().equals("adiw")  && ai1.getPostfix().startsWith("r") && ai2.getPostfix().startsWith("r")) {
				int pos = ai1.getPostfix().indexOf(',');
				int dst1 = Integer.parseInt(ai1.getPostfix().substring(1, pos));
				Integer k1 = null;
				try{
					k1 = Integer.parseInt(ai1.getPostfix().substring(pos+1));
				}
				catch(Exception ex){};
				
				pos = ai2.getPostfix().indexOf(',');
				int dst2 = Integer.parseInt(ai2.getPostfix().substring(1, pos));
				Integer k2 = null;
				try{
					k2 = Integer.parseInt(ai2.getPostfix().substring(pos+1));
				}
				catch(Exception ex){};
				
				if(null!=k1 && null!=k2 && dst1==dst2 && (k1+k2)<64) {
					item1.disable();
					ai2.setPostfix("r" + dst1 + ","+ (k1+k2));
				}
			}
		}

	}

	private String replaceAliasToReg(String regStr) {
		return	regStr.replaceAll("xl", "r26").replaceAll("xh", "r27").replaceAll("yl", "r28").replaceAll("yh", "r29")
				.replaceAll("zl", "r30").replaceAll("zh", "r31").replaceAll("result", "r20").replaceAll("flags", "r21")
				.replaceAll("accum_l", "r16").replaceAll("accum_h", "r17").replaceAll("accum_el", "r18").replaceAll("accum_eh", "r19")
				.replaceAll("temp_l", "r24").replaceAll("temp_h", "r25").replaceAll("temp_el", "r22").replaceAll("temp_eh", "r23")
				.replaceAll("j8b_atom", "r15");
	}
}

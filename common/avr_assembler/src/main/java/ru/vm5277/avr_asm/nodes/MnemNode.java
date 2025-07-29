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
package ru.vm5277.avr_asm.nodes;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.Instruction;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.nodes.operands.Const;
import ru.vm5277.avr_asm.nodes.operands.HReg;
import ru.vm5277.avr_asm.nodes.operands.AReg;
import ru.vm5277.avr_asm.nodes.operands.Reg;
import ru.vm5277.avr_asm.scope.CodeBlock;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.avr_asm.nodes.operands.EReg;
import ru.vm5277.avr_asm.nodes.operands.FlashAddr;
import ru.vm5277.avr_asm.nodes.operands.IOReg;
import ru.vm5277.avr_asm.semantic.BinaryExpression;
import ru.vm5277.avr_asm.semantic.IRegExpression;
import ru.vm5277.common.Operator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class MnemNode extends Node {
	private	Expression		expr1;
	private	Expression		expr2;
	
	private final	String						mnemonic;
	private final	Map<String, Instruction>	supported	= new HashMap<>();
	private			CodeBlock					block;
	private			int							addr;
	
	public MnemNode(TokenBuffer tb, Scope scope, MessageContainer mc) throws CompileException {
		super(tb, scope, mc);
		
		mnemonic = ((String)tb.consume().getValue()).toLowerCase();
		Map<String, Instruction> instructions = scope.getInstrReader().getInstrByMn().get(mnemonic);
		for(Instruction instr : instructions.values()) {
			if(scope.getInstrReader().getSupported().contains(instr.getId())) {
				supported.put(instr.getId(), instr);
			}
		}
		if(supported.isEmpty()) {
			throw new CompileException("//TODO не поддерживаемая мнемоника: " + mnemonic, sp);
		}

		Instruction instr = (Instruction)supported.values().toArray()[0x00];
		// выделяем память записывая опкоды(в дальнейшем могут быть изменены)
		block = scope.getCSeg().getCurrentBlock();
		addr = scope.getCSeg().getPC();
		block.writeOpcode(instr.getOpcode());
		scope.getCSeg().movePC(1);
		if(0x02 == instr.getWSize()) {
			block.writeOpcode(0x0000);
			scope.getCSeg().movePC(1);
		}

		if(!tb.match(TokenType.NEWLINE)) {
			expr1 = Expression.parse(tb, scope, mc);
			if(tb.match(Delimiter.COMMA)) {
				tb.consume();
				expr2 = Expression.parse(tb, scope, mc);
			}
		}
		
		scope.list(String.format("0x%04X", addr) + " " + mnemonic + (null != expr1 ? " " + expr1 : "") + (null != expr2 ? ", " + expr2 : ""));
	}
	
	public void secondPass() {
		try {
			if(!_secondPass()) {
				mc.add(new ErrorMessage("Unsupported insctruction '" + mnemonic + "'", sp));
			}
		}
		catch(Exception e) {
			mc.add(new ErrorMessage("Failed to parse '" + mnemonic + "' instruction: " + e.getMessage(), null));
		}
	}
	public boolean _secondPass() throws Exception {
		if(!supported.isEmpty()) {
			block.setOffset(addr);
			Instruction instr = (Instruction)supported.values().toArray()[0x00];
			String[] operands = instr.getOperands();

			if(0x01 == operands.length) {
				if(mnemonic.equals("spm")) {
					if(null == expr1) return true;	//нет операнда
					return false;
				}
				
				if(null != operands[0x00]) switch (operands[0x00]) {
					case Instruction.OPERAND_NONE: return true; // Чистый opcode уже записан
					case Instruction.OPERAND_R:
					case Instruction.OPERAND_RH:	parse(instr, new HReg(scope, sp, expr1)); return true;
					case Instruction.OPERAND_RR:	parse(instr, new Reg(scope, sp, expr1), new Reg(scope, sp, expr1)); return true;
					case Instruction.OPERAND_K7S:	parse(instr, new FlashAddr(mc, scope, sp, expr1, -64, 63, 7, addr)); return true;
					case Instruction.OPERAND_K12S:	parse(instr, new FlashAddr(mc, scope, sp, expr1, -2048, 2047, 12, addr)); return true;
					case Instruction.OPERAND_K22:	parse(instr, new Const(mc, scope, sp, expr1, 0, 0x3fffff, 22)); return true; // JMP и CALL
				}
				return false;
			}

			if(0x02 == operands.length) {
				if(Instruction.OPERAND_RE.equals(operands[0x00]) && Instruction.OPERAND_RE.equals(operands[0x01])) {
					parse(instr, new EReg(scope, sp, expr1), new EReg(scope, sp, expr2)); return true;
				}
				if(Instruction.OPERAND_R.equals(operands[0x00]) && Instruction.OPERAND_R.equals(operands[0x01])) {
					parse(instr, new Reg(scope, sp, expr1), new Reg(scope, sp, expr2)); return true;
				}
				if(Instruction.OPERAND_R.equals(operands[0x00]) && Instruction.OPERAND_K3.equals(operands[0x01])) {
					parse(instr, new Reg(scope, sp, expr1), new Const(mc, scope, sp, expr2, 0, 7, 3)); return true;
				}
				if(Instruction.OPERAND_A.equals(operands[0x00]) && Instruction.OPERAND_K3.equals(operands[0x01])) {
					parse(instr, new IOReg(scope, sp, expr1), new Const(mc, scope, sp, expr2, 0, 7, 3)); return true;
				}
				if(Instruction.OPERAND_RH.equals(operands[0x00]) && Instruction.OPERAND_K8.equals(operands[0x01])) {
					parse(instr, new HReg(scope, sp, expr1), new Const(mc, scope, sp, expr2, 0, 255, 8)); return true;
				}
				if(Instruction.OPERAND_RA.equals(operands[0x00]) && Instruction.OPERAND_K6.equals(operands[0x01])) {
					parse(instr, new AReg(scope, sp, expr1), new Const(mc, scope, sp, expr2, 0, 63, 6)); return true;
				}
				if(Instruction.OPERAND_K3.equals(operands[0x00]) && Instruction.OPERAND_K7S.equals(operands[0x01])) {
					parse(instr, new Const(mc, scope, sp, expr1, 0, 7, 3), new Const(mc, scope, sp, expr2, -64, 63, 7)); return true;
				}
				if(Instruction.OPERAND_R.equals(operands[0x00]) && Instruction.OPERAND_K16.equals(operands[0x01])) {
					parse(instr, new Reg(scope, sp, expr1), new Const(mc, scope, sp, expr2, 0, 65535, 16)); return true;
				}
				if(Instruction.OPERAND_K16.equals(operands[0x00]) && Instruction.OPERAND_R.equals(operands[0x01])) {
					parse(instr, new Const(mc, scope, sp, expr1, 0, 65535, 16), new Reg(scope, sp, expr2)); return true;
				}

				if(Instruction.OPERAND_R.equals(operands[0x01]) && mnemonic.equals("st")) {
					if(expr1 instanceof IRegExpression) {
						IRegExpression ire = ((IRegExpression)expr1);
						Instruction i = supported.get("st"+ire.getMnemPart());
						Reg reg = new Reg(scope, sp, expr2);
						if(null != i) {
							if(ire.getId() == (reg.getId()&0x01) && (ire.isDec() || ire.isInc())) {
								if(Scope.STRICT_STRONG == Scope.getStrincLevel()) {
									mc.add(new ErrorMessage("TODO undefined combination " + ire + " with " + expr2, sp));
								}
								else if(Scope.STRICT_LIGHT == Scope.getStrincLevel()) {
									mc.add(new WarningMessage("TODO undefined combination " + ire + " with " + expr2, sp));
								}
							}
							parse(i, reg);
							return true;
						}
						return false;
					}
				}
				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("ld")) {
					if(expr2 instanceof IRegExpression) {
						IRegExpression ire = ((IRegExpression)expr2);
						Instruction i = supported.get("ld"+ire.getMnemPart());
						Reg reg = new Reg(scope, sp, expr1);
						if(null != i) {
							if(ire.getId() == (reg.getId()&0x01) && (ire.isDec() || ire.isInc())) {
								if(Scope.STRICT_STRONG == Scope.getStrincLevel()) {
									mc.add(new ErrorMessage("TODO undefined combination " + ire + " with " + expr1, sp));
								}
								else if(Scope.STRICT_LIGHT == Scope.getStrincLevel()) {
									mc.add(new WarningMessage("TODO undefined combination " + ire + " with " + expr1, sp));
								}
							}
							parse(i, reg);
							return true;
						}
						return false;
					}
				}

				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("ldd")) {
					if(expr2 instanceof BinaryExpression) {
						BinaryExpression be = (BinaryExpression)expr2;
						if(Operator.PLUS == be.getOp()) {
							if(be.getLeftExpr() instanceof IRegExpression) {
								Instruction i = supported.get("ldd"+((IRegExpression)be.getLeftExpr()).getMnemPart());
								if(null != i) {
									// TODO HEX не корректный
									parseQ(i, new Reg(scope, sp, expr1), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6));
									return true;
								}
							}
						}
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x01]) && mnemonic.equals("std")) {
					if(expr1 instanceof BinaryExpression) {
						BinaryExpression be = (BinaryExpression)expr1;
						if(Operator.PLUS == be.getOp()) {
							if(be.getLeftExpr() instanceof IRegExpression) {
								Instruction i = supported.get("std"+((IRegExpression)be.getLeftExpr()).getMnemPart());
								if(null != i) {
									parseQ(i, new Reg(scope, sp, expr2), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6)); return true;
								}
							}
						}
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("lpm")) {
					if(expr2 instanceof IRegExpression) {
						Instruction i = supported.get("lpm"+((IRegExpression)expr2).getMnemPart());
						if(null != i) {
							parse(i, new Reg(scope, sp, expr1));
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	private void parse(Instruction instr, Reg reg) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		block.writeOpcode(instr.getOpcode() | reg.getId()<<4);
	}

	private void parse(Instruction instr, Const k) throws Exception {
		long opcode = instr.getOpcode();
		if(0x02==instr.getWSize()) {
			opcode <<= 16;
		}
		
		switch (k.getBits()) {
			case 7:
				opcode |= (0<=k.getValue() ? (k.getValue()-1)&0x7f : (k.getValue()-1&0x7f))<<3;
				break;
			case 12:
				opcode |= (0<=k.getValue() ? (k.getValue()-1)&0x0fff : (k.getValue()-1&0x0fff));
				break;
			case 22:
				if(0<=k.getValue()) {
					opcode |= k.getValue()&0x1ffff | (k.getValue()&0x03f0000)<<3 ;
				}
				else throw new Exception("");
				break;
			default:
				throw new Exception("");
		}
		
		if(0x02==instr.getWSize()) {
			block.writeDoubleOpcode(opcode);
		}
		else {
			block.writeOpcode((int)opcode);
		}
	}
	
	private void parse(Instruction instr, EReg rd, EReg rr) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		
		block.writeOpcode(instr.getOpcode() | rr.getId()>>1 | (rd.getId()>>1)<<4);
	}

	private void parse(Instruction instr, Reg rd, Reg rr) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		block.writeOpcode((int)(instr.getOpcode() | rd.getId()<<4 | (rr.getId()&0x0f) | (rr.getId()&0x10)<<5));
	}
	private void parse(Instruction instr, HReg rd, Const k) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(8==k.getBits()) {
			block.writeOpcode((int)(instr.getOpcode() | (rd.getId()&0x0f)<<4 | k.getValue()&0x0f | (k.getValue()&0xf0) << 4));
		}
		else throw new Exception("");
	}
	private void parse(Instruction instr, Reg rd, Const k) throws Exception {
		switch (k.getBits()) {
			case 3:
				if(0x01 != instr.getWSize()) throw new Exception();
				block.writeOpcode((int)(instr.getOpcode() | rd.getId()<<4 | k.getValue()&0x07));
				break;
			case 6:
				if(0x01 != instr.getWSize()) throw new Exception();
				byte id = (byte)((rd.getId()-24)/2);
				block.writeOpcode((int)(instr.getOpcode() | id<<4 | k.getValue()&0x0f | (k.getValue()&0x30)<<2));
				break;
			case 16:
				long opcode = (((long)instr.getOpcode())<<16) | k.getValue() | (rd.getId() << 20);
				block.writeDoubleOpcode(opcode);
				break;
			default:
				throw new Exception("");
		}
	}

	private void parse(Instruction instr, IOReg rd, Const k) throws Exception {
		switch (k.getBits()) {
			case 3:
				if(0x01 != instr.getWSize()) throw new Exception();
				block.writeOpcode((int)(instr.getOpcode() | rd.getId()<<3 | k.getValue()&0x07));
				break;
			default:
				throw new Exception("");
		}
	}

	private void parse(Instruction instr, Const k1, Const k2) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(3==k1.getBits() && 7==k2.getBits()) {
			long opcode = instr.getOpcode() | k1.getValue()&0x07;
			block.writeOpcode((int)(opcode | (0<=k2.getValue() ? (k2.getValue()&0x3f)<<3 : (k2.getValue()-1&0x7f)<<3))); // TODO проверить на значения(особенно -63)
		}
		else throw new Exception();
	}

	private void parse(Instruction instr, Const k, Reg rd) throws Exception {
		if(16==k.getBits()) {
			long opcode = (((long)instr.getOpcode())<<16) | k.getValue() | (rd.getId() << 20);
			block.writeDoubleOpcode(opcode);
		}
		else throw new Exception();
	}

	private void parseQ(Instruction instr, Reg rd, Const k) throws Exception {
		block.writeOpcode((int)(instr.getOpcode() | rd.getId()<<4 | k.getValue()&0x07 | (k.getValue()&0x18)<<7 | (k.getValue()&0x20)<<8));
	}
}

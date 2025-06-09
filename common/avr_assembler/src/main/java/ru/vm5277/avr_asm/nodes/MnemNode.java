/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
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
import ru.vm5277.common.Operator;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class MnemNode extends Node {
	private	Expression		expr1;
	private	Expression		expr2;
	
	private	String						mnemonic;
	private	Map<String, Instruction>	supported	= new HashMap<>();
	private	int							addr;
	
	public MnemNode(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		super(tb, scope, mc);
		
		mnemonic = ((String)tb.consume().getValue()).toLowerCase();
		Map<String, Instruction> instructions = scope.getInstrReader().getInstrByMn().get(mnemonic);
		for(Instruction instr : instructions.values()) {
			if(scope.getInstrReader().getSupported().contains(instr.getId())) {
				supported.put(instr.getId(), instr);
			}
		}
		if(supported.isEmpty()) {
			throw new ParseException("//TODO не поддерживаемая мнемоника: " + mnemonic, sp);
		}

		Instruction instr = (Instruction)supported.values().toArray()[0x00];
		// выделяем память записывая опкоды(в дальнейшем могут быть изменены)
		addr = scope.getCSeg().writeOpcode(instr.getOpcode());
		if(0x02 == instr.getWSize()) {
			scope.getCSeg().writeOpcode(0x0000);
		}

		if(!tb.match(TokenType.NEWLINE)) {
			expr1 = Expression.parse(tb, scope, mc);
			if(tb.match(Delimiter.COMMA)) {
				tb.consume();
				expr2 = Expression.parse(tb, scope, mc);
			}
		}
	}
		
	public void secondPass() {
		try {
			if(!_secondPass()) {
				mc.add(new ErrorMessage("//TODO не поддерживаемая мнемоника " + mnemonic, sp));
			}
		}
		catch(Exception e) {
			mc.add(new ErrorMessage("//TODO ошибка парсинга мнемоники " + mnemonic + ": " + e.getMessage(), sp));
		}
	}
	private boolean _secondPass() throws Exception {
		scope.getCSeg().setAddr((long)addr);

		if(!supported.isEmpty()) {
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
					case Instruction.OPERAND_RH:	parse(instr, new Reg(scope, sp, expr1)); return true;
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

				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("ld")) {
					Instruction i = supported.get("ldx");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_X)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldxp");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_XP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldmx");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_MX)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldy");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_Y)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldyp");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_YP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldmy");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_MY)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldz");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_Z)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldzp");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_ZP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("ldmz");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_MZ)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x01]) && mnemonic.equals("st")) {
					Instruction i = supported.get("stx");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_X)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stxp");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_XP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stmx");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_MX)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("sty");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_Y)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("styp");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_YP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stmy");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_MY)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stz");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_Z)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stzp");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_ZP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("stmz");
					if(null != i && operands[0x00].equals(Instruction.OPERAND_MZ)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("ldd")) {
					if(expr2 instanceof BinaryExpression) {
						BinaryExpression be = (BinaryExpression)expr2;
						if(Operator.PLUS == be.getOp()) {
							Instruction i = supported.get("lddy");
							if(null != i && operands[0x01].equals(Instruction.OPERAND_YP + Instruction.OPERAND_K6)) {
								parseQ(i, new Reg(scope, sp, expr1), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6)); return true;
							}
							i = supported.get("lddz");
							if(null != i && operands[0x01].equals(Instruction.OPERAND_ZP + Instruction.OPERAND_K6)) {
								parseQ(i, new Reg(scope, sp, expr1), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6)); return true;

							}
						}
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x01]) && mnemonic.equals("std")) {
					if(expr1 instanceof BinaryExpression) {
						BinaryExpression be = (BinaryExpression)expr1;
						if(Operator.PLUS == be.getOp()) {
							Instruction i = supported.get("stdy");
							if(null != i && operands[0x00].equals(Instruction.OPERAND_YP + Instruction.OPERAND_K6)) {
								parseQ(i, new Reg(scope, sp, expr2), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6)); return true;
							}
							i = supported.get("stdz");
							if(null != i && operands[0x00].equals(Instruction.OPERAND_ZP + Instruction.OPERAND_K6)) {
								parseQ(i, new Reg(scope, sp, expr2), new Const(mc, scope, sp, be.getRightExpr(), 0, 63, 6)); return true;
							}
						}
					}
					return false;
				}

				if(Instruction.OPERAND_R.equals(operands[0x00]) && mnemonic.equals("lpm")) {
					Instruction i = supported.get("lpmz");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_Z)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					i = supported.get("lpmzp");
					if(null != i && operands[0x01].equals(Instruction.OPERAND_ZP)) {
						parse(i, new Reg(scope, sp, expr1)); return true;
					}
					return false;
				}
			}
		}
		return false;
	}

	private void parse(Instruction instr, Reg reg) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		scope.getCSeg().writeOpcode(instr.getOpcode() | reg.getId()<<4);
	}

	private void parse(Instruction instr, Const k) throws Exception {
		long opcode = instr.getOpcode();
		if(0x02==instr.getWSize()) {
			opcode <<= 16;
		}
		
		switch (k.getBits()) {
			case 7:
				opcode |= (0<=k.getValue() ? k.getValue()&0x3f : 0x40 | ((k.getValue()*(-1))-1)&0x3f)<<3;
				break;
			case 12:
				opcode |= 0<=k.getValue() ? k.getValue()&0x0eff : 0x0800 | ((k.getValue()*(-1))-1)&0x0eff;
				break;
			case 22:
				if(0<=k.getValue()) {
					opcode |= k.getValue()&0x1ffff | (k.getValue()&0x03e0000)<<3 ;
				}
				else throw new Exception("");
				break;
			default:
				throw new Exception("");
		}
		
		if(0x02==instr.getWSize()) {
			((CodeBlock)scope.getCSeg().getCurrentBlock()).writeDoubleOpcode(opcode);
		}
		else {
			((CodeBlock)scope.getCSeg().getCurrentBlock()).writeOpcode((int)opcode);
		}
	}
	
	private void parse(Instruction instr, EReg rd, EReg rr) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		
		scope.getCSeg().writeOpcode(instr.getOpcode() | rr.getId()>>1 | (rd.getId()>>1)<<4);
	}

	private void parse(Instruction instr, Reg rd, Reg rr) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		scope.getCSeg().writeOpcode(instr.getOpcode() | rd.getId()<<4 | (rr.getId()&0x0f) | (rr.getId()&0x10<<9));
	}
	private void parse(Instruction instr, HReg rd, Const k) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(8==k.getBits()) {
			scope.getCSeg().writeOpcode((int)(instr.getOpcode() | (rd.getId()&0x0f)<<4 | k.getValue() & 0x0f | (k.getValue() & 0xf0) << 8));
		}
		else throw new Exception("");
	}
	private void parse(Instruction instr, Reg rd, Const k) throws Exception {
		switch (k.getBits()) {
			case 3:
				if(0x01 != instr.getWSize()) throw new Exception();
				scope.getCSeg().writeOpcode((int)(instr.getOpcode() | rd.getId()<<4 | k.getValue()&0x07));
				break;
			case 6:
				if(0x01 != instr.getWSize()) throw new Exception();
				byte id = (byte)((rd.getId()-24)/2);
				scope.getCSeg().writeOpcode((int)(instr.getOpcode() | id<<4 | k.getValue()&0x0f | (k.getValue()&0x30)<<6));
				break;
			case 16:
				long opcode = (((long)instr.getOpcode())<<16) | k.getValue() | (rd.getId() << 20);
				((CodeBlock)scope.getCSeg().getCurrentBlock()).writeDoubleOpcode(opcode);
				break;
			default:
				throw new Exception("");
		}
	}

	private void parse(Instruction instr, IOReg rd, Const k) throws Exception {
		switch (k.getBits()) {
			case 3:
				if(0x01 != instr.getWSize()) throw new Exception();
				scope.getCSeg().writeOpcode((int)(instr.getOpcode() | rd.getId()<<3 | k.getValue()&0x07));
				break;
			default:
				throw new Exception("");
		}
	}

	private void parse(Instruction instr, Const k1, Const k2) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(3==k1.getBits() && 7==k2.getBits()) {
			long opcode = instr.getOpcode() | k1.getValue()&0x07;
			scope.getCSeg().writeOpcode((int)(opcode | (0<=k2.getValue() ? (k2.getValue()&0x3f)<<3 : 0x0200) | (((k2.getValue()*(-1))-1)&0x3f)<<3));
		}
		else throw new Exception();
	}

	private void parse(Instruction instr, Const k, Reg rd) throws Exception {
		if(16==k.getBits()) {
			long opcode = (((long)instr.getOpcode())<<16) | k.getValue() | (rd.getId() << 20);
			((CodeBlock)scope.getCSeg().getCurrentBlock()).writeDoubleOpcode(opcode);
		}
		else throw new Exception();
	}

	private void parseQ(Instruction instr, Reg rd, Const k) throws Exception {
		scope.getCSeg().writeOpcode((int)(instr.getOpcode() | rd.getId()<<4 | k.getValue()&0x07 | (k.getValue()&0x18)<<7 | (k.getValue()&0x20)<<8));
	}
}

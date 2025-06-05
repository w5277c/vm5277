/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
//TODO двухпроходная модель, сначала выделяем память и регистрируем все в scope, в том числе формруем возможные опкоды
//опкоды инструкций связанных с метками помечаем не выплнеными
//На втором заходе формируем опкоды с метками, зная их смещения.

package ru.vm5277.avr_asm.nodes;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.Instruction;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.nodes.operands.Const;
import ru.vm5277.avr_asm.nodes.operands.HReg;
import ru.vm5277.avr_asm.nodes.operands.IReg;
import ru.vm5277.avr_asm.nodes.operands.Reg;
import ru.vm5277.avr_asm.scope.CodeBlock;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
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
		if(mnemonic.equalsIgnoreCase("nop")) {
			int t=3432423;
		}

		Map<String, Instruction> instructions = scope.getInstrReader().getInstrByMn().get(mnemonic);
		for(Instruction instr : instructions.values()) {
			if(scope.isSupported(instr.getId())) {
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
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
		
	public void secondPass() {
		try {
			
			if(mnemonic.equalsIgnoreCase("nop")) {
				int t=3432423;
			}
			
			scope.getCSeg().setAddr((long)addr);
			
			if(0x01==supported.size()) {
				Instruction instr = (Instruction)supported.values().toArray()[0x00];
				String[] operands = instr.getOperands();
				if(0x01 == operands.length) {
					if(null != operands[0x00]) switch (operands[0x00]) {
						case Instruction.OPERAND_NONE:
							// Чистый opcode уже записан
							break;
						case Instruction.OPERAND_R:
						case Instruction.OPERAND_RH:
							parse(instr, new Reg(scope, sp, expr1));
							break;
						case Instruction.OPERAND_K12S:
							parse(instr, new Const(scope, sp, expr1, -2048, 2047, 12));
							break;
						case Instruction.OPERAND_K22:
							parse(instr, new Const(scope, sp, expr1, 0, 0x3fffff, 22));
							break;
						default:
							break;
					}
				}
				else if(0x02 == operands.length) {
					if(Instruction.OPERAND_R.equals(operands[0x00]) && Instruction.OPERAND_R.equals(operands[0x01])) {
						parse(instr, new Reg(scope, sp, expr1), new Reg(scope, sp, expr2));
					}
					else if(Instruction.OPERAND_RH.equals(operands[0x00]) && Instruction.OPERAND_K8.equals(operands[0x01])) {
						parse(instr, new HReg(scope, sp, expr1), new Const(scope, sp, expr2, 0, 255, 8));
					}
					else if(Instruction.OPERAND_R.equals(operands[0x00]) && Instruction.OPERAND_K6.equals(operands[0x01])) {
						parse(instr, new IReg(scope, sp, expr1), new Const(scope, sp, expr2, 0, 63, 6));
					}
					else if(Instruction.OPERAND_K3.equals(operands[0x00]) && Instruction.OPERAND_K7S.equals(operands[0x01])) {
						parse(instr, new Const(scope, sp, expr1, 0, 7, 3), new Const(scope, sp, expr2, -64, 63, 7));
					}
				}
			}
			mc.add(new ErrorMessage("//TODO не поддерживаемая мнемоника " + mnemonic, sp));
		}
		catch(Exception e) {
			mc.add(new ErrorMessage("//TODO ошибка парсинга мнемоники " + mnemonic + ": " + e.getMessage(), sp));
		}
	}

	private void parse(Instruction instr, Reg reg) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		scope.getCSeg().writeOpcode(instr.getOpcode() | (reg.getId()&0x1f)<<4);
	}

	private void parse(Instruction instr, Const k) throws Exception {
		long opcode = instr.getOpcode();
		if(0x02==instr.getWSize()) {
			opcode <<= 16;
		}
		
		switch (k.getBits()) {
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
	
	private void parse(Instruction instr, Reg rd, Reg rr) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		scope.getCSeg().writeOpcode(instr.getOpcode() | (rd.getId()&0x1f)<<4 | (rr.getId()&0x0f) | (rr.getId()&0x10<<9));
	}

	private void parse(Instruction instr, Reg rd, Const k) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(6==k.getBits()) {
			byte id = (byte)((rd.getId()-24)/2);
			scope.getCSeg().writeOpcode((int)(instr.getOpcode() | id<<4 | k.getValue()&0x0f | (k.getValue()&0x30)<<6));
		}
		else throw new Exception("");
	}

	private void parse(Instruction instr, Const k1, Const k2) throws Exception {
		if(0x01 != instr.getWSize()) throw new Exception();
		if(3==k1.getBits() && 7==k2.getBits()) {
			long opcode = instr.getOpcode() | k1.getValue()&0x07;
			scope.getCSeg().writeOpcode((int)(opcode | (0<=k2.getValue() ? (k2.getValue()&0x3f)<<3 : 0x0200) | (((k2.getValue()*(-1))-1)&0x3f)<<3));
		}
		else throw new Exception();
	}
}

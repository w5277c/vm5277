/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
19.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/

package ru.vm5277.j8b.compiler.codegen.avr;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.Case;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.RegisterMap;
import ru.vm5277.j8b.compiler.common.enums.OperandType;
import ru.vm5277.j8b.compiler.common.enums.Operator;
import ru.vm5277.j8b.compiler.common.enums.VarType;

public class Generator extends CodeGenerator {
	private	final	Map<String, RegisterMap> regMap;
	private			Operand acc;	//TODO похоже будет не нужен
	private			StringBuilder	asmSource	= new StringBuilder();
	
	public Generator(Map<String, RegisterMap> regMap, Map<String, String> params) {
		this.regMap = regMap;
	}
	
	@Override
	public int enterClass(int typeId, int[] intrerfaceIds) {
		int id = genId();
		System.out.println("CG:enterClass, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
		return id;
	}

	@Override
	public int enterFiled(int typeId, String name) {
		int id = genId();
		System.out.println("CG:enterField, id:" + id + ", typeId:" + typeId + ", name:" + name);
		return id;
	}

	@Override
	public int enterConstructor(int[] typeIds) {
		int id = genId();
		System.out.println("CG:enterConstructor, id:" + id + ", parameters:" + Arrays.toString(typeIds));
		return id;
	}

	@Override
	public int enterMethod(int typeId, int[] typeIds) {
		int id = genId();
		System.out.println("CG:enterMehod, id:" + id + ", typeId:" + typeId + ", parameters:" + Arrays.toString(typeIds));
		return id;
	}

	@Override
	public int enterLocal(int typeId, String name) {
		int id = genId();
		System.out.println("CG:enterLocal, id:" + id + ", typeId:" + typeId + ", name:" + name);
		return id;
	}

	@Override
	public int enterBlock() {
		int id = genId();
		System.out.println("CG:enterBlock, id:" + id);
		return id;
	}

	@Override
	public void leave() {
		System.out.println("CG:Leave");
	}

	@Override
	public void eNew(int typeId, Operand[] parameters, boolean canThrow) {
		System.out.println("CG:eNew, " + typeId +", params:" + Arrays.toString(parameters));
	}

	@Override
	public void eFree(Operand op) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public void setAcc(Operand source) {
		System.out.println("CG:setAcc, op:" + source);
		acc = source;
	}
	@Override
	public Operand getAcc() {
		System.out.println("CG:getAcc, acc:" + acc);
		return acc;
	}

	@Override
	public void loadAcc(int id) { //Загружаем значение переменной в acc
		System.out.println("CG:loadAcc, srcId:" + id);
	}
	@Override
	public void storeAcc(int id) { //Записываем acc в переменную
		System.out.println("CG:storeAcc, dstId:" + id);
		
	}

	@Override
	public void invokeMethod(int id, int typeId, Operand[] args) {
		System.out.println("CG:invokeMethod, id:" + id + ", typeId:" + typeId + ", args:" + Arrays.toString(args));
	}
	
	@Override
	public void invokeNative(String methodQName, int typeId, Operand[] parameters) throws Exception {
		System.out.println("CG:invokeNative " + methodQName + ", typeId:" + typeId + ", params" + Arrays.toString(parameters));
		
		RegisterMap rm = regMap.get(methodQName);
		if(null == rm) throw new Exception("CG:InvokeNative, register map record not found for method: " + methodQName);
		byte[][] regIds = rm.getRegIds();
		if(parameters.length != regIds.length) {
			throw new Exception("CG:InvokeNative, invalid parameter count for method " + methodQName + ", expected " + regIds.length + ", got " +
								parameters.length);
		}

		StringBuilder sb = new StringBuilder();
		for(int i=0; i<parameters.length; i++) {
			byte[] registers = regIds[i];
			if(0 == registers.length || 4 < registers.length) throw new Exception("CG:Invalid parameters for invoke method " + methodQName);
			
			long value = 0;
			Operand op = parameters[i];
			if(OperandType.VARIABLE == op.getOperandType()) {
				throw new UnsupportedOperationException("Variable parameters not implemented yet");
			}
			else if(OperandType.LITERAL == op.getOperandType()) {
				if(op.getValue() instanceof Number) {
					value = ((Number)op.getValue()).longValue();
				}
				else throw new Exception("CG:InvokeNative: literal must be a number for method " + methodQName);
			}
			for(int j=0; j<registers.length; j++) {
				int reg = registers[j] & 0xFF;
				if (reg<16 || reg>31) throw new Exception("CG:InvokeNative: invalid register R" + reg + " for method " + methodQName);
				sb.append("LDI R").append(reg).append(",").append(value&0xff).append("\n");
				value >>>= 8;
			}
			sb.append("CALL ").append(rm.getRtosFunction()).append("\n");
		}
		asmSource.append(sb);
		
		System.out.println("CG:invokeNative asm:\n" + sb.toString());
	}
	
	@Override
	public Operand emitInstanceof(Operand op, int typeId) {
		System.out.println("CG:emitInstanceOf, op:" + op + ", typeId:" + typeId);
		return new Operand(VarType.BOOL.getId(), OperandType.LITERAL, true);
	}
	
	@Override
	public void emitUnary(Operator op) {
		System.out.println("CG:emitUnary, op:" + op);
	}

	@Override
	public void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId) {
		System.out.println("CG:if, condBlockId:" + conditionBlockId + ", thenBlockId:" + thenBlockId + ", elseBlockId:" + elseBlockId);
	}

	@Override
	public void eTry(int blockId, List<Case> cases, Integer defaultBlockId) {
		System.out.println("CG:try, blockId:" + blockId + ", casesBlocks:" + cases + ", defaultBlockId:" + defaultBlockId);
	}
	
	@Override
	public void eWhile(int conditionBlockId, int bodyBlockId) {
		System.out.println("CG:while, condBlockId:" + conditionBlockId + ", bodyBlockId:" + bodyBlockId);
	}
	
	@Override
	public void eReturn() {
		System.out.println("CG:return");
	}
	
	@Override
	public void eThrow() {
		System.out.println("CG:throw");
	}
}

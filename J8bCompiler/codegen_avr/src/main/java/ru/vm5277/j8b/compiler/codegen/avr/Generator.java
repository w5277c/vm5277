/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
19.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/

package ru.vm5277.j8b.compiler.codegen.avr;

import java.util.Arrays;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.enums.OperandType;
import ru.vm5277.j8b.compiler.common.enums.VarType;

public class Generator extends CodeGenerator {
	private	Operand acc;
	
	public Generator(Map<String, String> params) {
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
	public void invokeMethod(int id, Operand[] args) {
		System.out.println("CG:invokeMethod, id:" + id + ", args:" + Arrays.toString(args));
	}

	@Override
	public Operand emitInstanceof(Operand op, int typeId) {
		System.out.println("CG:emitInstanceOf, op:" + op + ", typeId:" + typeId);
		return new Operand(VarType.BOOL.getId(), OperandType.LITERAL, true);
	}

	@Override
	public void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}

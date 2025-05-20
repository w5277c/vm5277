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
import ru.vm5277.j8b.compiler.common.enums.Operator;

public class Generator implements CodeGenerator {
	private	Operand acc;
	
	public Generator(Map<String, String> params) {
	}
	
	
	@Override
	public void enterClass(int typeId, int[] intrerfaceIds) {
		System.out.println("CG:enterClass, typeId:" + typeId + ", interfaces id:" + Arrays.toString(intrerfaceIds));
	}

	@Override
	public void enterFiled(int typeId, String name) {
		System.out.println("CG:enterField, typeId:" + typeId + ", name:" + name);
	}

	@Override
	public void enterConstructor(int[] typeIds) {
		System.out.println("CG:enterConstructor, parameters:" + Arrays.toString(typeIds));
	}

	@Override
	public void enterMethod(int typeId, int[] typeIds) {
		System.out.println("CG:enterMehod, typeId:" + typeId + ", parameters:" + Arrays.toString(typeIds));
	}

	@Override
	public void enterLocal(int typeId, String name) {
		System.out.println("CG:enterLocal, typeId:" + typeId + ", name:" + name);
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
	public void loadAcc(Operand src) {
		System.out.println("CG:loadAcc, src:" + src);
	}
	@Override
	public void storeAcc(Operand dst) {
		System.out.println("CG:storeAcc, src:" + dst);
	}
}

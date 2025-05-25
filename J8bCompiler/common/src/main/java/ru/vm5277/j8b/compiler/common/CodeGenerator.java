/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common;

import java.util.List;
import ru.vm5277.j8b.compiler.common.enums.Operator;

public abstract class CodeGenerator {
	private	int	idCntr	= 0;
	
	public int genId() {
		return idCntr++;
	}
	
	public abstract int enterClass(int typeId, int[] intrerfaceIds);	//тип 
	public abstract int enterFiled(int typeId, String name);
	public abstract int enterConstructor(int[] typeIds);
	public abstract int enterMethod(int typeId, int[] typeIds);
	public abstract int enterLocal(int typeId, String name); //TODO сделать индексацию вместо имен
	public abstract int enterBlock();
	public abstract void leave();
	
	public abstract void setAcc(Operand src);
	public abstract Operand getAcc();
	public abstract void loadAcc(int srcId); //Загрузить переменную в аккумулятор
	public abstract void storeAcc(int srcId); //Записать аккумулятор в переменную
	
	public abstract void invokeMethod(int id, Operand[] args);
	public abstract Operand emitInstanceof(Operand op, int typeId);	//todo может быть поросто boolean?
	public abstract void emitUnary(Operator op); //PLUS, MINUS, BIT_NOT, NOT, PRE_INC, PRE_DEC, POST_INC, POST_DEC
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public abstract void eNew(int typeId, Operand[] parameters, boolean canThrow);
	public abstract void eFree(Operand op);
	
	public abstract void eIf(int conditionBlockId, int thenBlockId, Integer elseBlockId);
	public abstract void eTry(int blockId, List<Case> cases, Integer defaultBlockId);
	public abstract void eWhile(int conditionBlockId, int bodyBlockId);
	public abstract void eReturn();
	public abstract void eThrow();
	
}
/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common;

public interface CodeGenerator {
	public void enterClass(int typeId, int[] intrerfaceIds);	//тип 
	public void enterFiled(int typeId, String name);
	public void enterConstructor(int[] typeIds);
	public void enterMethod(int typeId, int[] typeIds);
	public void enterLocal(int typeId, String name); //TODO сделать индексацию вместо имен
	public void leave();
	
	
	void setAcc(Operand src);
	Operand getAcc();
	void loadAcc(Operand src);
	void storeAcc(Operand dst);
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
	public void eNew(int typeId, Operand[] parameters, boolean canThrow);
	public void eFree(Operand op);

}
/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
18.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common;

import ru.vm5277.j8b.compiler.common.enums.Operator;

public interface CodeGenerator {
	public void enterClass(byte typeId, byte[] intrerfaceIds);
	public void enterFiled(byte typeId);
	public void enterConstructor(byte[] typeIds);
	public void enterMethod(byte typeId, byte[] typeIds);
	
	public void declareLocal(byte typeId); //TODO нужна позиция в стеке?
	
	public void leave();
	
	// Выражения (линейные)
	void load(Operand source);          // Загрузить значение в "аккумулятор"
	void store(Operand dest);           // Сохранить "аккумулятор" в dest
	void binaryOp(Operator op, Operand left, Operand right); // left op right -> аккумулятор
	void unaryOp(Operator op, Operand operand); // op operand -> аккумулятор
	//...
	
	
	//TODO набор методов для реализации команд if, switch, for, loop и .т.д
}
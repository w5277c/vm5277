/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
19.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common;

import ru.vm5277.j8b.compiler.common.enums.OperandType;

public class Operand {
	private	int			typeId;
	private	OperandType	opType;
	private	Object		value;
	
	public Operand(int typeId, OperandType opType, Object value) {
		this.typeId = typeId;
		this.opType = opType;
		this.value = value;
	}
	
	public int getTypeId() {
		return typeId;
	}
	
	public OperandType getOperandType() {
		return opType;
	}	
	
	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return opType + ":" + typeId + "=" + value;
	}
	
}

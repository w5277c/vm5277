/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.semantic;

import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.enums.VarType;

public class Symbol {
	protected			String	name;
	protected			VarType	type;
	protected			boolean	isStatic;
	protected			boolean	isFinal;
	protected			boolean	isNative;
	protected			int		runtimeId;
	protected			Operand	constOp;
	
	protected Symbol(String name) {
		this.name = name;
	}
	
	public Symbol(String name, VarType type) {
		this.name = name;
		this.type = type;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic, boolean isNative) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
		this.isNative = isNative;
	}

	public String getName() {
		return name;
	}
	
	public VarType getType() {
		return type;
	}
	public void setType(VarType type) {
		this.type = type;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public boolean isNative() {
		return isNative;
	}
	
	public void setRuntimeId(int runtimeId) {
		this.runtimeId = runtimeId;
	}
	public int getRuntimeId() {
		return runtimeId;
	}

	public void setConstantOperand(Operand op) {
		this.constOp = op;
	}
	public Operand getConstantOperand() {
		return constOp;
	}
}
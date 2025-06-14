/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
13.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.expressions;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.common.j8b_compiler.Operand;
import ru.vm5277.common.j8b_compiler.OperandType;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.semantic.InterfaceSymbol;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class TypeReferenceExpression extends ExpressionNode {
	private final	String	className;
	private			VarType	varType;
	
	public TypeReferenceExpression(TokenBuffer tb, MessageContainer mc, String className) {
		super(tb, mc);
		
		this.className = className;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		// Возвращаем тип-класс, если он существует
		VarType type = VarType.fromClassName(className);
		if (null != type && !type.isClassType()) throw new SemanticException("Type '" + className + "' not found");

		if(null == type) {
			// Затем проверяем, является ли это интерфейсом
			InterfaceSymbol interfaceSymbol = scope.getThis().resolveInterface(className);
			if (null != interfaceSymbol) {
				type = VarType.addClassName(className);
			}
		}
		
		return type;
	}

	@Override
	public boolean preAnalyze() {
		return className != null && !className.isEmpty();
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			varType = getType(scope); // Проверяем существование типа
			return true;
		}
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}

	@Override
	public String toString() {
		return "TypeReference: " + className;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		cg.setAcc(new Operand(0, OperandType.TYPE, varType.getId()));
	}
}
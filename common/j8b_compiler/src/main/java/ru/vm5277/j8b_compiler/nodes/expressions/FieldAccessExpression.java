/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
12.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.expressions;

import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.semantic.ClassScope;
import ru.vm5277.j8b_compiler.semantic.Scope;
import ru.vm5277.j8b_compiler.semantic.Symbol;

public class FieldAccessExpression extends ExpressionNode {
	private	final	ExpressionNode	target;
	private	final	String			fieldName;
	
	private			String			className;
	private			Symbol			fieldSymbol;
	
	public FieldAccessExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode target, String fieldName) {
		super(tb, mc);
		this.target = target;
		this.fieldName = fieldName;
	}

	public ExpressionNode getTarget() {
		return target;
	}

	public String getClassName() {
		return className;
	}
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String getNodeType() {
		return "member access";
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		resolve(scope);
		return null == fieldSymbol ? null : fieldSymbol.getType();
	}

	public Object getValue() {
		return null == fieldSymbol ? null : fieldSymbol;
	}
	
	// Реализация остальных методов (getType, analyze и т.д.)
	
	// TODO поидее этот код метода declare
	private void resolve(Scope scope) throws SemanticException {
		if(null == fieldSymbol) {
			if(target instanceof VariableExpression) {
				VariableExpression ve = (VariableExpression)target;
				className = ve.getValue();
				ClassScope classScope = scope.getThis().resolveClass(ve.getValue());
				if(null != classScope) {
					fieldSymbol = classScope.resolve(fieldName);
				}
				else {
					throw new SemanticException("Can't resolve class scope:" + ve.getValue());
				}
			}
			else {
				throw new SemanticException("Not supported expression:" + target.toString() + ", type:"+ target.getNodeType());
			}
		}
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		cg.setAcc(fieldSymbol.getConstantOperand());
	}
}
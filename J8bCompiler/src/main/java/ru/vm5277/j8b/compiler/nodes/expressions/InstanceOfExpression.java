/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
13.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class InstanceOfExpression extends ExpressionNode {
	private	final	ExpressionNode	left;		// Проверяемое выражение
	private	final	ExpressionNode	typeExpr;	// Выражение, возвращающее тип

	public InstanceOfExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode left, ExpressionNode typeExpr) {
		super(tb, mc);
		this.left = left;
		this.typeExpr = typeExpr;
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		// Результат instanceof всегда boolean
		return VarType.BOOL;
	}

	public ExpressionNode getLeft() {
		return left;
	}

	public ExpressionNode getTypeExpr() {
		return typeExpr;
	}

	@Override
	public boolean preAnalyze() {
		if (left == null || typeExpr == null) {
			markError("Both operands of 'is' must be non-null");
			return false;
		}
		return left.preAnalyze() && typeExpr.preAnalyze();
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			if (!left.postAnalyze(scope)) {
				return false;
			}

			// Анализируем правую часть (тип)
			if (!typeExpr.postAnalyze(scope)) {
				return false;
			}

			// Проверяем, что typeExpr возвращает тип (класс)
			VarType type = typeExpr.getType(scope);
			if (!type.isClassType()) {
				markError("Right-hand side of 'is' must be a class type, got: " + type);
				return false;
			}

			// Предупреждение для примитивов (если left — примитив)
			VarType leftType = left.getType(scope);
			if (leftType.isPrimitive()) {
				markWarning("Primitive type check 'is' is usually constant for " + leftType);
			}

			return true;
		}
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}

	@Override
	public String toString() {
		return left + " is " + typeExpr;
	}
}
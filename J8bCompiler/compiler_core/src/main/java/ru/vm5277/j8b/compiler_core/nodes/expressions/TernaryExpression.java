/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class TernaryExpression extends ExpressionNode {
	private final ExpressionNode condition;
	private final ExpressionNode trueExpr;
	private final ExpressionNode falseExpr;

	public TernaryExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode condition, ExpressionNode trueExpr, ExpressionNode falseExpr) {
		super(tb, mc);

		this.condition = condition;
		this.trueExpr = trueExpr;
		this.falseExpr = falseExpr;
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		VarType trueType = trueExpr.getType(scope);
		VarType falseType = falseExpr.getType(scope);

		// Возвращаем более общий тип
		return trueType.getSize() >= falseType.getSize() ? trueType : falseType;
	}
	
	@Override
	public boolean preAnalyze() {
		if (null== condition || null == trueExpr || null == falseExpr) {
			markError("All parts of ternary expression must be non-null");
			return false;
		}

		if (!condition.preAnalyze()) return false;
		if (!trueExpr.preAnalyze()) return false;
		if (!falseExpr.preAnalyze()) return false;

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			// Проверяем условие и ветки
			if (!condition.postAnalyze(scope)) return false;
			if (!trueExpr.postAnalyze(scope)) return false;
			if (!falseExpr.postAnalyze(scope)) return false;

			// Проверяем тип условия
			VarType condType = condition.getType(scope);
			if (VarType.BOOL != condType) {
				markError("Condition must be boolean, got: " + condType);
				return false;
			}

			// Проверяем совместимость типов веток
			VarType trueType = trueExpr.getType(scope);
			VarType falseType = falseExpr.getType(scope);

			if (!isCompatibleWith(scope, trueType, falseType)) {
				markError("Incompatible types in branches: " + trueType + " and " + falseType);
				return false;
			}

			return true;
		}
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
	
	// Геттеры
	public ExpressionNode getCondition() {
		return condition;
	}

	public ExpressionNode getTrueExpr() {
		return trueExpr;
	}

	public ExpressionNode getFalseExpr() {
		return falseExpr;
	}

	@Override
	public String toString() {
		return condition + " ? " + trueExpr + " : " + falseExpr;
	}
}
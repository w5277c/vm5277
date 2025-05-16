/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
16.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class ThrowNode extends CommandNode {
	private ExpressionNode exceptionExpr;

	public ThrowNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "throw"

		// Парсим выражение исключения
		try {this.exceptionExpr = new ExpressionNode(tb, mc).parse();} catch (ParseException e) {markFirstError(e);}

		// Потребляем точку с запятой
		try {consumeToken(tb, Delimiter.SEMICOLON);} catch (ParseException e) {markFirstError(e);}
	}

	public ExpressionNode getExceptionExpr() {
		return exceptionExpr;
	}

	@Override
	public String toString() {
		return "throw " + (null != exceptionExpr ? exceptionExpr.toString() : "");
	}

	@Override
	public String getNodeType() {
		return "throw command";
	}

	@Override
	public boolean preAnalyze() {
		if (null != exceptionExpr) {exceptionExpr.preAnalyze();}
		else markError("Exception expression cannot be null");
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (null != exceptionExpr) exceptionExpr.declare(scope);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		if (null != exceptionExpr) {
			if (exceptionExpr.postAnalyze(scope)) {
				try {
					VarType exprType = exceptionExpr.getType(scope);
					// Проверяем, что выражение имеет тип byte (код ошибки)
					if (VarType.BYTE != exprType) markError("Throw expression must be of type byte, got: " + exprType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}
		return true;
	}
}
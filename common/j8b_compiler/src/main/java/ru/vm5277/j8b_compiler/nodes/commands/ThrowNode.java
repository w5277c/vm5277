/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
16.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.commands;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b_compiler.Delimiter;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class ThrowNode extends CommandNode {
	private	ExpressionNode	exceptionExpr;

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
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		exceptionExpr.codeGen(cg);
		cg.eThrow();
	}
}
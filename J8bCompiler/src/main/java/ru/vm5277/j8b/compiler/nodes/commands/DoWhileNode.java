/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b.compiler.semantic.BlockScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class DoWhileNode extends CommandNode {
	private	ExpressionNode	condition;
	private	BlockScope		blockScope;

	public DoWhileNode(TokenBuffer tb) {
		super(tb);

		consumeToken(tb);
		// Тело цикла
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		try {
			consumeToken(tb, TokenType.COMMAND, Keyword.WHILE);
			consumeToken(tb, Delimiter.LEFT_PAREN);
			this.condition = new ExpressionNode(tb).parse();
			consumeToken(tb, Delimiter.RIGHT_PAREN);
		}
		catch(ParseException e) {
			markFirstError(e);
		}

		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return blocks.isEmpty() ? null : blocks.get(0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("do ");
		sb.append(getBody());
		sb.append(" while (");
		sb.append(condition);
		sb.append(");");
		return sb.toString();
	}

	@Override
	public String getNodeType() {
		return "do-while loop";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка тела цикла (выполняется всегда хотя бы один раз)
		if (null != getBody()) getBody().preAnalyze();
		else markError("Do-while body cannot be null");

		// Проверка условия цикла
		if (null != condition) condition.preAnalyze();
		else markError("Do-while condition cannot be null");

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Создаем новую область видимости для тела цикла
		blockScope = new BlockScope(scope);

		// Объявляем элементы тела цикла
		if (null != getBody()) getBody().declare(blockScope);

		// Объявляем переменные условия (в той же области, что и тело)
		if (null != condition) condition.declare(blockScope);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Анализ тела цикла
		if (null != getBody()) getBody().postAnalyze(blockScope);

		// Проверка типа условия
		if (null != condition) {
			if(condition.postAnalyze(scope)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("While condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
			
			// Проверяем бесконечный цикл с возвратом
			if (	condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) &&
					isControlFlowInterrupted(getBody())) {
				
				markWarning("Code after infinite while loop is unreachable");
			}
		}
		return true;
	}
}
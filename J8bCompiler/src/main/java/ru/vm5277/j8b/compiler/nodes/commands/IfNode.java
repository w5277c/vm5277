/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
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
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.j8b.compiler.semantic.BlockScope;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class IfNode extends CommandNode {
    private	ExpressionNode	condition;
	private	BlockScope		thenScope;
	private	BlockScope		elseScope;
	private	String			varName;
	
	public IfNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e){markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		// Парсинг условия (обычное или pattern matching)
		if (tb.match(Keyword.AS)) {
			tb.consume(); // Потребляем "as"
			// Проверяем, что после типа идет идентификатор
			if (tb.match(TokenType.ID)) {
				this.varName = consumeToken(tb).getStringValue();
			}
			else {
				markError("Expected variable name after type in pattern matching");
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		// Else блок
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, Keyword.IF)) {
				// Обработка else if
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb, mc, new IfNode(tb, mc)));
				tb.getLoopStack().remove(this);
			}
			else {
				tb.getLoopStack().add(this);
				try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return blocks.get(0);
    }

    public BlockNode getElseBlock() {
        return (0x02 == blocks.size() ? blocks.get(1) : null);
    }
	
	public String getVarName() {
		return varName;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("if (").append(condition.toString()).append(") ");
		sb.append(getThenBlock().toString());

		if (getElseBlock() != null) {
			sb.append(" else ");
			sb.append(getElseBlock().toString());
		}

		return sb.toString();
	}

	@Override
	public String getNodeType() {
		return "if condition";
	}

	@Override
	public boolean preAnalyze() {
		if (condition != null) condition.preAnalyze();
		else markError("If condition cannot be null");

		// Проверка что есть хотя бы один блок
		if (null == getThenBlock() && null == getElseBlock()) markError("If statement must have at least one block (then or else)");
		
		// Проверка then-блока
		if (null != getThenBlock()) getThenBlock().preAnalyze();

		// Проверка else-блока (если есть)
		if (null != getElseBlock()) getElseBlock().preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление переменных условия
		if (null != condition) condition.declare(scope);
		
		// Объявление then-блока в новой области видимости
		if (null != getThenBlock()) {
			thenScope = new BlockScope(scope);
		}
		
		// Для pattern matching: объявляем новую переменную в then-блоке
		if (null != varName) {
			if(condition instanceof InstanceOfExpression) {
				InstanceOfExpression instanceOf = (InstanceOfExpression) condition;
				try {
					VarType type = instanceOf.getTypeExpr().getType(scope);
					if (null != type && null != thenScope) {
						thenScope.addLocal(new Symbol(varName, type, false, false));
					}
				}
				catch (SemanticException e) {markError(e);
				}
			}
			else {
				markError("Pattern matching requires 'is' check before 'as'");
			}
		}		

		if(null != thenScope) {
			getThenBlock().declare(thenScope);
		}

		// Объявление else-блока в новой области видимости
		if (null != getElseBlock()) {
			elseScope = new BlockScope(scope);
			getElseBlock().declare(elseScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка типа условия
		if (null != condition) {
			if (condition.postAnalyze(scope)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("If condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Анализ then-блока
		if (null != getThenBlock()) getThenBlock().postAnalyze(thenScope);

		// Анализ else-блока
		if (null != getElseBlock()) getElseBlock().postAnalyze(elseScope);
		
		// Проверяем недостижимый код после if с возвратом во всех ветках
        if (null != getElseBlock() && isControlFlowInterrupted(getThenBlock()) && isControlFlowInterrupted(getElseBlock())) {
            markWarning("Code after if statement may be unreachable");
        }
		
		return true;
	}
}

/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.commands;

import ru.vm5277.j8b.compiler_core.nodes.BlockNode;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.semantic.BlockScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class SwitchNode extends CommandNode {
	private	ExpressionNode	expression;
	private	final	List<Case>				cases			= new ArrayList<>();
	private			BlockNode				defaultBlock	= null;
	private			BlockScope				switchScope;
	private			BlockScope				defaultScope;
	
	public SwitchNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Потребляем "switch"
		// Парсим выражение switch
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
		try {this.expression = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
		
		try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(ParseException e) {markFirstError(e);}
		// Парсим case-блоки
		while (!tb.match(Delimiter.RIGHT_BRACE)) {
			if (tb.match(Keyword.CASE)) {
				Case c = parseCase(tb, mc);
				if(null != c) cases.add(c);
			}
			else if (tb.match(Keyword.DEFAULT)) {
				consumeToken(tb); // Потребляем "default"
				try {consumeToken(tb, Delimiter.COLON);} catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().add(this);
				try {defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
			else {
				markFirstError(parserError("Expected 'case' or 'default' in switch statement"));
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
	}


	public ExpressionNode getExpression() {
		return expression;
	}

	public List<Case> getCases() {
		return cases;
	}

	public BlockNode getDefaultBlock() {
		return defaultBlock;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("switch (");
		sb.append(expression).append(") {\n");

		for (Case c : cases) {
			sb.append("case ").append(c.getFrom());
			if (-1 != c.getTo()) {
				sb.append("..").append(c.getTo());
			}
			sb.append(": ").append(c.getBlock()).append("\n");
		}

		if (null != defaultBlock) {
			sb.append("default: ").append(defaultBlock).append("\n");
		}

		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public String getNodeType() {
		return "switch command";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка выражения switch
		if (null != expression) expression.preAnalyze();
		else markError("Switch expression cannot be null");

		// Проверка всех case-блоков
		for (Case c : cases) {
			if (null != c.getBlock()) c.getBlock().preAnalyze();
		}

		// Проверка default-блока
		if (null != defaultBlock) defaultBlock.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление выражения switch
		if (expression != null) expression.declare(scope);

		// Создаем новую область видимости для switch
		switchScope = new BlockScope(scope);

		// Объявление всех case-блоков
		for (Case c : cases) {
			if (null != c.getBlock()) {
				BlockScope caseScope = new BlockScope(switchScope);
				c.getBlock().declare(caseScope);
				c.setScope(caseScope);
			}
		}

		// Объявление default-блока
		if (null != defaultBlock) {
			defaultScope = new BlockScope(switchScope);
			defaultBlock.declare(defaultScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		boolean allCasesReturn = true;		
		
		// Проверка типа выражения switch
		if (expression != null) {
			if (expression.postAnalyze(scope)) {
				try {
					VarType exprType = expression.getType(scope);
					if (!exprType.isInteger() && VarType.BYTE != exprType && VarType.SHORT != exprType) {
						markError("Switch expression must be integer type, got: " + exprType);
					}
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Проверка case-значений на уникальность
		List<Long> caseValues = new ArrayList<>();
		for (Case c : cases) {
			// Проверка диапазона
			if (-1 != c.getTo() && c.getFrom() > c.getTo()) {
				markError("Invalid case range: " + c.getFrom() + ".." + c.getTo());
			}

			// Проверка на дубликаты
			if (c.getTo() == -1) {
				if (caseValues.contains(c.getFrom())) markError("Duplicate case value: " + c.getFrom());
				else caseValues.add(c.getFrom());
			}
			else {
				for (long i = c.getFrom(); i <= c.getTo(); i++) {
					if (caseValues.contains(i)) markError("Duplicate case value in range: " + i);
					else caseValues.add(i);
				}
			}

			// Анализ блока case
			if (null != c.getBlock()) {
				c.getBlock().postAnalyze(c.getScope());
				if (!isControlFlowInterrupted(c.getBlock())) {
					allCasesReturn = false;
				}
			}
		}

		// Анализ default-блока (если есть)
		if (null != defaultBlock) defaultBlock.postAnalyze(defaultScope);

		if (allCasesReturn && !cases.isEmpty()) {
			markWarning("Code after switch statement may be unreachable");
		}
		
		return true;
	}
}
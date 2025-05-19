/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
15.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.commands;

import ru.vm5277.j8b.compiler_core.nodes.AstNode;
import ru.vm5277.j8b.compiler_core.nodes.BlockNode;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import ru.vm5277.j8b.compiler_core.enums.TokenType;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.semantic.BlockScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.semantic.Symbol;

public class TryNode extends CommandNode {
	private	BlockNode		tryBlock;
	private	String			varName;
	private	List<Case>		catchCases		= new ArrayList<>();
	private	BlockNode		catchDefault;
	private	BlockScope		tryScope;
	private	BlockScope		catchScope;
	private	BlockScope		defaultScope;
	private	boolean			hasDefault		= false;
	
	public TryNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "try"
		
		// Блок try
		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {this.tryBlock = new BlockNode(tb, mc);} catch(ParseException e) {markFirstError(e);}
			tb.getLoopStack().remove(this);
		}
		else markError("Expected '{' after 'try'");

		// Парсим параметр catch (byte errCode)
		if (tb.match(Keyword.CATCH)) {
			consumeToken(tb); // Потребляем "catch"
			try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
			if (tb.match(TokenType.TYPE, Keyword.BYTE)) {tb.consume();} // Потребляем "byte"
			else markError("Expected 'byte' type in catch parameter");
			if (tb.match(TokenType.ID)) {this.varName = consumeToken(tb).getStringValue();}
			else markError("Expected variable name in catch parameter");
			try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
			// Тело catch
			try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(ParseException e) {markFirstError(e);}

			// Если сразу идет код без case/default - считаем его default-блоком
            if (!tb.match(Keyword.CASE) && !tb.match(Keyword.DEFAULT) && !tb.match(Delimiter.RIGHT_BRACE)) {
				tb.getLoopStack().add(this);
                try {catchDefault = new BlockNode(tb, mc, true);} catch (ParseException e) {markFirstError(e);}
                tb.getLoopStack().remove(this);
			}
			else {
				// Парсим case-блоки
				while (!tb.match(Delimiter.RIGHT_BRACE)) {
					if (tb.match(Keyword.CASE)) {
						if (hasDefault) {
							markError("'case' cannot appear after 'default' in catch block");
							tb.skip(Delimiter.RIGHT_BRACE);
							break;
						}
						Case c = parseCase(tb, mc);
						if(null != c) catchCases.add(c);
					}
					else if (tb.match(Keyword.DEFAULT)) {
						consumeToken(tb); // Потребляем "default"
						try {consumeToken(tb, Delimiter.COLON);} catch(ParseException e) {markFirstError(e);}
						tb.getLoopStack().add(this);
						try {catchDefault = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
						catch(ParseException e) {markFirstError(e);}
						tb.getLoopStack().remove(this);
					}
					else {
						markFirstError(parserError("Expected 'case', 'default' or code block in catch"));
					}
				}
			}
			
			try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
		}
		// try может быть без catch
	}

	public String getVarName() {
		return varName;
	}

	public BlockNode getTryBlock() {
		return tryBlock;
	}

	public List<Case> getCatchCases() {
		return catchCases;
	}

	public BlockNode getCatchDefault() {
		return catchDefault;
	}
	
	@Override
	public String getNodeType() {
		return "try-catch block";
	}
	
	public AstNode getEndNode() {
		if (null != catchDefault) return catchDefault;
		if (!catchCases.isEmpty()) return catchCases.get(catchCases.size()-1).getBlock();
		return tryBlock;
	}

	@Override
	public boolean preAnalyze() {
		// Проверка блока try
		if (null != tryBlock) tryBlock.preAnalyze();
		else markError("Try block cannot be null");

		// Проверка всех catch-блоков
		for (Case c : catchCases) {
			if (null != c.getBlock()) c.getBlock().preAnalyze();
		}

		// Проверка default-блока
		if (null != catchDefault) catchDefault.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		// Объявление блока try в новой области видимости
		tryScope = new BlockScope(scope);
		if (null != tryBlock) tryBlock.declare(tryScope);

		// Объявление переменной catch-параметра
		catchScope = new BlockScope(scope);
		if(null != varName)	{
			try{catchScope.addLocal(new Symbol(varName, VarType.BYTE, true, false));}catch(SemanticException e) {markError(e);}
		}

		// Объявление catch-блоков
		for (Case c : catchCases) {
			BlockScope caseScope = new BlockScope(catchScope);
			c.getBlock().declare(caseScope);
			c.setScope(caseScope);
		}

		// Объявление default-блока
		if (null != catchDefault) {
			defaultScope = new BlockScope(catchScope);
			catchDefault.declare(defaultScope);
		}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		// Анализ блока try
		if (null != tryBlock) tryBlock.postAnalyze(tryScope);

		// Проверка catch-значений на уникальность
		List<Long> catchValues = new ArrayList<>();
		for (Case c : catchCases) {
			// Проверка диапазона
			if (-1 != c.getTo() && c.getFrom() > c.getTo()) {
				markError("Invalid catch range: " + c.getFrom() + ".." + c.getTo());
			}

			// Проверка на дубликаты
			if (c.getTo() == -1) {
				if (catchValues.contains(c.getFrom())) markError("Duplicate catch value: " + c.getFrom());
				else catchValues.add(c.getFrom());
			} else {
				for (long i = c.getFrom(); i <= c.getTo(); i++) {
					if (catchValues.contains(i)) markError("Duplicate catch value in range: " + i);
					else catchValues.add(i);
				}
			}

			// Анализ блока catch
			if (null != c.getBlock()) c.getBlock().postAnalyze(c.getScope());
		}

		// Анализ default-блока
		if (null != catchDefault) catchDefault.postAnalyze(defaultScope);

		return true;
	}
}

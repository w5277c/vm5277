/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
15.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.commands;

import ru.vm5277.j8b_compiler.nodes.AstNode;
import ru.vm5277.j8b_compiler.nodes.BlockNode;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.j8b_compiler.Case;
import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.J8bKeyword;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.semantic.BlockScope;
import ru.vm5277.j8b_compiler.semantic.Scope;
import ru.vm5277.j8b_compiler.semantic.Symbol;

public class TryNode extends CommandNode {
	private	BlockNode		tryBlock;
	private	String			varName;
	private	List<AstCase>	catchCases		= new ArrayList<>();
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
		if (tb.match(J8bKeyword.CATCH)) {
			consumeToken(tb); // Потребляем "catch"
			try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
			if (tb.match(TokenType.TYPE, J8bKeyword.BYTE)) {tb.consume();} // Потребляем "byte"
			else markError("Expected 'byte' type in catch parameter");
			if (tb.match(TokenType.ID)) {this.varName = consumeToken(tb).getStringValue();}
			else markError("Expected variable name in catch parameter");
			try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
			// Тело catch
			try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(ParseException e) {markFirstError(e);}

			// Если сразу идет код без case/default - считаем его default-блоком
            if (!tb.match(J8bKeyword.CASE) && !tb.match(J8bKeyword.DEFAULT) && !tb.match(Delimiter.RIGHT_BRACE)) {
				tb.getLoopStack().add(this);
                try {catchDefault = new BlockNode(tb, mc, true);} catch (ParseException e) {markFirstError(e);}
                tb.getLoopStack().remove(this);
			}
			else {
				// Парсим case-блоки
				while (!tb.match(Delimiter.RIGHT_BRACE)) {
					if (tb.match(J8bKeyword.CASE)) {
						if (hasDefault) {
							markError("'case' cannot appear after 'default' in catch block");
							tb.skip(Delimiter.RIGHT_BRACE);
							break;
						}
						AstCase c = parseCase(tb, mc);
						if(null != c) catchCases.add(c);
					}
					else if (tb.match(J8bKeyword.DEFAULT)) {
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
				try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
			}
		}
		// try может быть без catch
	}

	public String getVarName() {
		return varName;
	}

	public BlockNode getTryBlock() {
		return tryBlock;
	}

	public List<AstCase> getCatchCases() {
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
		for (AstCase c : catchCases) {
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
		for (AstCase c : catchCases) {
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
		for (AstCase astCase : catchCases) {
			// Проверка диапазона
			if (null != astCase.getTo() && astCase.getFrom() > astCase.getTo()) {
				markError("Invalid catch range: " + astCase.getFrom() + ".." + astCase.getTo());
			}

			// Проверка на дубликаты
			if (null == astCase.getTo()) {
				if (catchValues.contains(astCase.getFrom())) markError("Duplicate catch value: " + astCase.getFrom());
				else catchValues.add(astCase.getFrom());
			}
			else {
				for (long i = astCase.getFrom(); i <= astCase.getTo(); i++) {
					if (catchValues.contains(i)) markError("Duplicate catch value in range: " + i);
					else catchValues.add(i);
				}
			}

			// Анализ блока catch
			if (null != astCase.getBlock()) astCase.getBlock().postAnalyze(astCase.getScope());
		}

		// Анализ default-блока
		if (null != catchDefault) catchDefault.postAnalyze(defaultScope);

		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		int blockId = cg.enterBlock();
		tryBlock.codeGen(cg);
		cg.leave();
		
		List<Case> cases = new ArrayList<>();
		for(AstCase astCase : catchCases) {
			int caseBlockId = cg.enterBlock();
			astCase.getBlock().codeGen(cg);
			cg.leave();
			cases.add(new Case(astCase.getFrom(), astCase.getTo(), caseBlockId));
		}
			
		Integer defaultBlockId = null;
		if(null != catchDefault) {
			defaultBlockId = cg.enterBlock();
			catchDefault.codeGen(cg);
			cg.leave();
		}
		
		cg.eTry(blockId, cases, defaultBlockId);
	}
}

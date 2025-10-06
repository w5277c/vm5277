/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.compiler.nodes.commands;

import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.compiler.Case;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class TryNode extends CommandNode {
	private	BlockNode		tryBlock;
	private	String			varName;
	private	List<AstCase>	catchCases			= new ArrayList<>();
	private	BlockNode		catchDefaultBlock;
	private	BlockScope		tryScope;
	private	BlockScope		catchScope;
	private	BlockScope		defaultScope;
	private	boolean			hasDefault			= false;
	
	public TryNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "try"
		
		// Блок try
		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {this.tryBlock = new BlockNode(tb, mc);} catch(CompileException e) {markFirstError(e);}
			tb.getLoopStack().remove(this);
		}
		else markError("Expected '{' after 'try'");

		// Парсим параметр catch (byte errCode)
		if (tb.match(Keyword.CATCH)) {
			consumeToken(tb); // Потребляем "catch"
			try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(CompileException e) {markFirstError(e);}
			if (tb.match(TokenType.TYPE, Keyword.BYTE)) {tb.consume();} // Потребляем "byte"
			else markError("Expected 'byte' type in catch parameter");
			if (tb.match(TokenType.ID)) {this.varName = consumeToken(tb).getStringValue();}
			else markError("Expected variable name in catch parameter");
			try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(CompileException e) {markFirstError(e);}
			// Тело catch
			try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(CompileException e) {markFirstError(e);}

			// Если сразу идет код без case/default - считаем его default-блоком
            if (!tb.match(Keyword.CASE) && !tb.match(Keyword.DEFAULT) && !tb.match(Delimiter.RIGHT_BRACE)) {
				tb.getLoopStack().add(this);
                try {catchDefaultBlock = new BlockNode(tb, mc, true);} catch (CompileException e) {markFirstError(e);}
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
						AstCase c = parseCase(tb, mc);
						if(null != c) catchCases.add(c);
					}
					else if (tb.match(Keyword.DEFAULT)) {
						consumeToken(tb); // Потребляем "default"
						try {consumeToken(tb, Delimiter.COLON);} catch(CompileException e) {markFirstError(e);}
						tb.getLoopStack().add(this);
						try {catchDefaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
						catch(CompileException e) {markFirstError(e);}
						tb.getLoopStack().remove(this);
					}
					else {
						markFirstError(parserError("Expected 'case', 'default' or code block in catch"));
					}
				}
				try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(CompileException e) {markFirstError(e);}
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
		return catchDefaultBlock;
	}
	
	@Override
	public String getNodeType() {
		return "try-catch block";
	}
	
	public AstNode getEndNode() {
		if (null != catchDefaultBlock) return catchDefaultBlock;
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
		if (null != catchDefaultBlock) catchDefaultBlock.preAnalyze();

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
			try{catchScope.addVariable(new Symbol(varName, VarType.BYTE, true, false));}catch(CompileException e) {markError(e);}
		}

		// Объявление catch-блоков
		for (AstCase c : catchCases) {
			BlockScope caseScope = new BlockScope(catchScope);
			c.getBlock().declare(caseScope);
			c.setScope(caseScope);
		}

		// Объявление default-блока
		if (null != catchDefaultBlock) {
			defaultScope = new BlockScope(catchScope);
			catchDefaultBlock.declare(defaultScope);
		}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		// Анализ блока try
		if (null != tryBlock) tryBlock.postAnalyze(tryScope, cg);

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
			if (null != astCase.getBlock()) astCase.getBlock().postAnalyze(astCase.getScope(), cg);
		}

		// Анализ default-блока
		if (null != catchDefaultBlock) catchDefaultBlock.postAnalyze(defaultScope, cg);

		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		CGBlockScope blockScope = cg.enterBlock();
		tryBlock.codeGen(cg, null, false);
		cg.leaveBlock();
		
		List<Case> cases = new ArrayList<>();
		for(AstCase astCase : catchCases) {
			CGBlockScope caseBlockScope = cg.enterBlock();
			astCase.getBlock().codeGen(cg, null, false);
			cg.leaveBlock();
			cases.add(new Case(astCase.getFrom(), astCase.getTo(), caseBlockScope));
		}
			
		CGBlockScope defaultBlockScope = null;
		if(null != catchDefaultBlock) {
			defaultBlockScope = cg.enterBlock();
			catchDefaultBlock.codeGen(cg, null, false);
			cg.leaveBlock();
		}
		
		cg.eTry(blockScope, cases, defaultBlockScope);
		
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		List<AstNode> result = new ArrayList<>(catchCases);
		if(null != catchDefaultBlock) result.add(catchDefaultBlock);
		return result;
	}
}

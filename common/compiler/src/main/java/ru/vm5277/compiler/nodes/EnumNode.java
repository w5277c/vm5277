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

package ru.vm5277.compiler.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.EnumScope;
import ru.vm5277.compiler.semantic.Scope;

public class EnumNode extends AstNode {
	protected	final	Set<Keyword>	modifiers;
	protected			String			name;
	protected			List<String>	values;
	private				EnumScope		enumScope;
	
	public EnumNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers) throws CompileException {
		super(tb, mc);
		
		this.modifiers = modifiers;
		
		// Парсинг заголовка enum
        consumeToken(tb);	// Пропуск enum токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();

			values = new ArrayList<>();
			consumeToken(tb, Delimiter.LEFT_BRACE);

			while (!tb.match(Delimiter.RIGHT_BRACE)) {
				String value = (String)consumeToken(tb, TokenType.ID).getValue();
				values.add(value);

				if(!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
			if(tb.match(Delimiter.SEMICOLON)) consumeToken(tb, Delimiter.SEMICOLON);
			consumeToken(tb, Delimiter.RIGHT_BRACE);
			
			VarType.addClassName(this.name, true);
		}
		catch(CompileException e) {markFirstError(e);} // ошибка в имени, оставляем null
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try {
			validateName(name);
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}
		
		if(Character.isLowerCase(name.charAt(0))) {
			markWarning("Enum name should start with uppercase letter: " + name);
		}

		try {
			validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE);
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}

		if(values.isEmpty()) {
			markError("Enum must have at least one value");
			result = false;
		}
		
		Set<String> set = new HashSet<>(values);
		if(set.size() != values.size()) {
			markError("Enum values must be unique");
			result = false;
		}
		for(String value : values) {
			try {
				validateName(value);
			}
			catch(CompileException ex) {
				markError(ex);
				result = false;
			}
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		try {
			enumScope = new EnumScope(name, scope, values);
			scope.addInternal(enumScope);
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override  
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	public String getName() {
		return name;
	}
	
	public EnumScope getScope() {
		return enumScope;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public List<String> getValues() {
		return values;
	}
	
	@Override
	public String toString() {
		return modifiers + " " + name;
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}

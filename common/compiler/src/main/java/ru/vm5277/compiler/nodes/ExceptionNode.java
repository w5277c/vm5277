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
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.GlobalScope;
import ru.vm5277.compiler.semantic.ImportableScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;

public class ExceptionNode extends ObjectTypeNode {
	private			List<String>	values;
	private			List<String>	ext				= new ArrayList<>();
	private			CIScope			extScope;
	
	public ExceptionNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, List<ObjectTypeNode> importedClasses) throws CompileException {
		super(tb, mc, modifiers, null, importedClasses);
		
		if(null!=name) {
			VarType.addException(this.name);
		}
		
		try {
			if(tb.match(TokenType.OOP, J8BKeyword.EXTENDS)) {
				consumeToken(tb);
				while(true) {
					try {
						//TODO QualifiedPath
						ext.add((String)consumeToken(tb, TokenType.IDENTIFIER).getValue());
					}
					catch(CompileException e) {
						markError(e);
					}
					if(!tb.match(Delimiter.COMMA)) {
						break;
					}
					consumeToken(tb);
				}
			}

			values = new ArrayList<>();
			consumeToken(tb, Delimiter.LEFT_BRACE);

			while (!tb.match(Delimiter.RIGHT_BRACE)) {
				String value = (String)consumeToken(tb, TokenType.IDENTIFIER).getValue();
				values.add(value);

				if(!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
			if(tb.match(Delimiter.SEMICOLON)) consumeToken(tb, Delimiter.SEMICOLON);
			consumeToken(tb, Delimiter.RIGHT_BRACE);
		}
		catch(CompileException e) {
			markError(e);
		}
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
			markWarning("Exception name should start with uppercase letter: " + name);
		}

		try {
			validateModifiers(modifiers, J8BKeyword.PUBLIC, J8BKeyword.PRIVATE);
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}

		if(0x01<ext.size()) {
			markError("Multiple exception inheritance not allowed");
			result = false;
		}

		Set<String> set = new HashSet<>(values);
		if(set.size() != values.size()) {
			markError("Exception values must be unique");
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
			ciScope = new ExceptionScope(scope, VarType.getExceptionId(name), name, values);
			
			ImportableScope iScope = (scope instanceof GlobalScope ? (GlobalScope)scope : scope.getThis());
			iScope.addCI(ciScope, true);

			if(null!=importedClasses) {
				for(ObjectTypeNode imported : importedClasses) {
					result&=imported.declare(ciScope);

					if(result) {
						if(imported instanceof ExceptionNode) {
							try {
								ciScope.addCI(((ObjectTypeNode)imported).getScope(), false);
							}
							catch(CompileException ex) {
								markError(ex);
								result = false;
							}
						}
						else {
							markError("Exception cannot import non-exception type: " + imported.getName());
							result = false;
						}
					}
				}
			}

			if(result && !ext.isEmpty()) {
				extScope = scope.resolveCI(ext.get(0), false);
				if(null==extScope) {
					markError("Exception not found: " + ext.get(0));
					result = false;
				}
				else {
					VarType.setExceptionParent(((ExceptionScope)ciScope).getId(), ((ExceptionScope)extScope).getId());
					((ExceptionScope)ciScope).setExtScope((ExceptionScope)extScope);
				}
			}
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
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return null;
	}

	@Override
	public AstNode getBody() {
		return null;
	}
}

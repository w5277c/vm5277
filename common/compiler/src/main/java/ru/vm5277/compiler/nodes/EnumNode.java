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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.EnumScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.tokens.TAnnotation;
import ru.vm5277.common.lexer.tokens.TNumber;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.Instance;

public class EnumNode extends ObjectTypeNode {
	protected			Map<Integer,String>	items;
	private		final	ObjectTypeNode		objTypeNode;
	private				EnumScope			enumScope;
	
	public EnumNode(Instance inst, TokenBuffer tb, Map<String, AnnotationParamNode> annotations, Set<Keyword> modifiers, ObjectTypeNode objectTypeNode)
																																	throws CompileException {
		super(inst, tb, null, modifiers, null, null);
		
		this.annotations = annotations;
		
		modifiers.add(J8BKeyword.STATIC);
		modifiers.add(J8BKeyword.FINAL);
		
		this.objTypeNode = objectTypeNode;
		
		try {
			items = new HashMap<>();
			consumeToken(tb, Delimiter.LEFT_BRACE);

			Integer index=0;
			while (!tb.match(Delimiter.RIGHT_BRACE)) {
				String value = (String)consumeToken(tb, TokenType.IDENTIFIER).getValue();
				if(tb.match(Delimiter.LEFT_PAREN)) {
					tb.consume();
					Object tmp = ((TNumber)consumeToken(tb, TokenType.NUMBER)).getValue();
					if(tmp instanceof Number) {
						index = ((Number)tmp).intValue();
					}
					else {
						markFirstError(new ErrorMessage("Enum index must be ineteger in range 0-255", sp));
					}
					consumeToken(tb, Delimiter.RIGHT_PAREN);
				}

				if(index<0 || index>255) {
					markFirstError(new ErrorMessage("Enum index must be in range 0-255", sp));
				}
				
				if(null!=items.put(index++, value) ) {
					markFirstError(new ErrorMessage("Enum index must be unique", sp));
					break;
				}

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
			validateModifiers(modifiers, J8BKeyword.STATIC, J8BKeyword.FINAL, J8BKeyword.PUBLIC, J8BKeyword.PRIVATE);
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}

		if(items.isEmpty()) {
			markError("Enum must have at least one value");
			result = false;
		}
		
		Set<String> set = new HashSet<>(items.values());
		if(set.size() != items.size()) {
			markError("Enum values must be unique");
			result = false;
		}
		for(String value : items.values()) {
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
			enumScope = new EnumScope(name, scope, items);
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());

		if(null!=annotations) {
			for(String annotation : annotations.keySet()) {
				if(objTypeNode.isRTOSDriver() && annotation.equals(TAnnotation.NATIVE)) {
					AnnotationParamNode apn = annotations.get(TAnnotation.NATIVE);
					for(int index=0; index<items.size(); index++) {
						String valueName = apn.getTextValue(0) + items.get(index).toUpperCase();
						if(!cg.addConstant(valueName, index)) {
							result = false;
							markError("@Native enum value '" + valueName + "' already defined");
							break;
						}
					}
					continue;
				}
				
				if(StrictLevel.LIGHT==inst.getStrictLevel()) {
					addMessage(new WarningMessage("Unsupported enum annotation: @" + annotation, annotations.get(annotation).getSP()));
				}
				else if(StrictLevel.STRONG==inst.getStrictLevel()) {
					addMessage(new ErrorMessage("Unsupported enum annotation: @" + annotation, annotations.get(annotation).getSP()));
				}
			}
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public EnumScope getScope() {
		return enumScope;
	}
	
	@Override
	public Set<Keyword> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
	@Override
	public boolean isFinal() {
		return true;
	}
	@Override
	public boolean isPublic() {
		return modifiers.contains(J8BKeyword.PUBLIC);
	}
	@Override
	public boolean isNative() {
		return false;
	}

	public Map<Integer, String> getItems() {
		return items;
	}
	
	@Override
	public String toString() {
		return modifiers + " " + name;
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		releaseVars(cg);
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

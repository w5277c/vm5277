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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	private			BlockNode			blockNode;
	private			MethodScope			methodScope; // Добавляем поле для хранения области видимости
	private			boolean				canThrow	= false;
	
	public MethodNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name) throws CompileException {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;

        consumeToken(tb); // Потребляем '('
		this.parameters = parseParameters(mc);
        consumeToken(tb); // Потребляем ')'
		
		// Проверяем наличие throws
		if (tb.match(TokenType.OOP, Keyword.THROWS)) {
			consumeToken(tb);
			this.canThrow = true;
		}

		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {
				blockNode = new BlockNode(tb, mc);
			}
			catch(CompileException e) {}
			tb.getLoopStack().remove(this);
		}
		else {
			try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
		}
	}
	
	public boolean canThrow() {
		return canThrow;
	}
	
	private List<ParameterNode> parseParameters(MessageContainer mc) {
        List<ParameterNode> params = new ArrayList<>();
        
		while(!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_PAREN)) {
			try {
				params.add(new ParameterNode(tb, mc));
				if (tb.match(Delimiter.COMMA)) {
					consumeToken(tb); // Потребляем ','
					continue;
				}
				else if(tb.match(TokenType.EOF) || tb.match(Delimiter.RIGHT_PAREN)) {
					break;
				}
				ErrorMessage message = new ErrorMessage("Expected " + Delimiter.RIGHT_PAREN + ", but got " + tb.current().getType(), tb.current().getSP());
				addMessage(message);
				markFirstError(message);
				break;
			}
			catch(CompileException e) {
				markFirstError(e);
				tb.skip(Delimiter.RIGHT_PAREN);
				break;
			}
		}

		return params;
    }
	
	public BlockNode getBody() {
		return blockNode;
	}
	
	public boolean isConstructor() {
		return null == returnType;
	}

	public List<ParameterNode> getParameters() {
		return parameters;
	}
	
	public VarType getReturnType() {
		return returnType;
	}

	public String getName() {
		return name;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "method";
	}

	public boolean isStatic() {
		return modifiers.contains(Keyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(Keyword.PUBLIC);
	}
	public boolean isNative() {
		return modifiers.contains(Keyword.NATIVE);
	}
	
	@Override
	public boolean preAnalyze() {
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC, Keyword.NATIVE);} catch(CompileException e) {addMessage(e);}
		
		if(!isConstructor() && Character.isUpperCase(name.charAt(0))) {
			addMessage(new WarningMessage("Method name should start with uppercase letter:" + name, sp));
		}

		for (ParameterNode parameter : parameters) {
			parameter.preAnalyze();
		}
		
		if(null != getBody()) {
			getBody().preAnalyze();
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		List<Symbol> paramSymbols = new ArrayList<>();
		for (ParameterNode param : parameters) {
			//TODO здесь мы не знаем, кто нас вызывает и что передает
/*			if(!isNative()) {
				Symbol pSymbol = null;//TODO
				if(null == pSymbol) {
					markError(new CompileException("param '" + param.getName() + "' not found"));
					return false;
				}
				paramSymbols.add(new AliasSymbol(param.getName(), param.getType(), pSymbol));
			}
			else {*/
				paramSymbols.add(new Symbol(param.getName(), param.getType(), param.isFinal(), false));
			//}
		}

		// Создаем MethodSymbol
		try {
			methodScope = new MethodScope(null, classScope);
			symbol = new MethodSymbol(	name, returnType, paramSymbols, modifiers.contains(Keyword.FINAL),
										modifiers.contains(Keyword.STATIC),	modifiers.contains(Keyword.NATIVE), canThrow, methodScope, this);
			// Устанавливаем обратную ссылку
			methodScope.setSymbol((MethodSymbol)symbol);

			// Добавляем метод или конструктор в область видимости класса
			if (isConstructor()) {
				classScope.addConstructor((MethodSymbol)symbol);
			}
			else {
				// Для обычных методов
				classScope.addMethod((MethodSymbol)symbol);
			}

			// Объявляем параметры в области видимости метода
			for (ParameterNode param : parameters) {
				param.declare(methodScope);
			}
		}
		catch(CompileException e) {markError(e);}
		
		if(null != getBody()) {
			getBody().declare(methodScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		VarType[] types = null;
		if(!parameters.isEmpty()) {
			types = new VarType[parameters.size()];
			for(int i=0; i<parameters.size(); i++) {
				types[i] = parameters.get(i).getType();
			}
		}

		if(null == returnType) {
			symbol.setCGScope(cg.enterConstructor(types));
		}
		else {
			symbol.setCGScope(cg.enterMethod(returnType, types, name));
		}

		if (modifiers.contains(Keyword.NATIVE)) {
			if (getBody() != null) {
				markError("Native method cannot have a body");
			}
		}
		else {
			// Анализ тела метода (если есть)
			if (null != getBody() && null != methodScope) {
				for (ParameterNode param : parameters) {
					param.postAnalyze(methodScope, cg);
				}

				// Анализируем тело метода
				getBody().postAnalyze(methodScope, cg);

				// Для не-void методов проверяем наличие return
				// TODO переосмыслить после ConstantFolding
				if (null != returnType && !returnType.equals(VarType.VOID)) {
					if (!BlockNode.hasReturnStatement(getBody())) {
						markError("Method '" + name + "' must return a value");
					}
				}

				List<AstNode> nodes = getBody().getChildren();
				for (int i = 0; i < nodes.size(); i++) {
					if (i > 0 && isControlFlowInterrupted(nodes.get(i - 1))) {
						markError("Unreachable code in method " + name);
						break;
					}
				}
			}
		}

		if(null == returnType) {
			cg.leaveConstructor();
		}
		else {
			cg.leaveMethod();
		}
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		return codeGen(cg, false);
	}
	public Object codeGen(CodeGenerator cg, boolean launchPoint) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		((CGMethodScope)symbol.getCGScope()).build(cg, launchPoint);
		
		/* не требуется
		for(ParameterNode node : parameters) {
			node.codeGen(cg);
		}*/
		
		if(null != blockNode) blockNode.codeGen(cg);		
		
		if(VarType.VOID == returnType) {
			cg.eReturn(symbol.getCGScope(), 0);
		}
		
		return null;
	}
/*
	@Override
	public Boolean isUsed() {
		if(null != super.isUsed() && super.isUsed()) return true;
		if(null == methodScope) return null;
		return methodScope.isUsed();
	}
	@Override
	public void setUsed() {
		super.setUsed();
		if(null != methodScope) methodScope.setUsed();
	}
*/
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
	
	@Override
	public String toString() {
		return	StrUtils.toString(modifiers) + " " + returnType + " " + name + "(" + StrUtils.toString(parameters) + ")" +
				(canThrow ? " throws" : "");
	}
}

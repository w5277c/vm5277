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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private	final	ObjectTypeNode		objTypeNode;
	private			List<ParameterNode>	parameters;
	private			BlockNode			blockNode;
	private			MethodScope			methodScope; // Добавляем поле для хранения области видимости
	private			boolean				canThrow	= false;
	
	public MethodNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name, ObjectTypeNode objectTypeNode)
																																	throws CompileException {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		this.objTypeNode = objectTypeNode;

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
	
	public MethodNode(ClassNode classNode) {
		super();
		
		this.modifiers = new HashSet<>();
		this.modifiers.add(Keyword.PUBLIC);
		this.returnType = null;
		this.name = classNode.getName();
		this.objTypeNode = classNode;
		this.parameters = new ArrayList<>();
		
		blockNode = new BlockNode();
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
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		try{
			validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC, Keyword.NATIVE);
		}
		catch(CompileException e) {
			addMessage(e);
			result = false;
		}
		
		if(result) {
			if(null==name) {
				markError("Constructor or method declaration is invalid: name cannot be null");
				result = false;
			}
		}
		
		if(result) {
			if(!isConstructor() && Character.isUpperCase(name.charAt(0))) {
				markWarning("Method name should start with uppercase letter:" + name);
			}

			for(ParameterNode parameter : parameters) {
				result&=parameter.preAnalyze();
			}
		}
		
		if(result) {
			if(null!=blockNode) {
				result&=blockNode.preAnalyze();
			}
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		methodScope = new MethodScope(null, scope);
		// Объявляем параметры в области видимости метода
		for(ParameterNode param : parameters) {
			result&=param.declare(methodScope);
		}

		List<Symbol> paramSymbols = new ArrayList<>();
		if(result) {
			for (ParameterNode param : parameters) {
				paramSymbols.add(new Symbol(param.getName(), param.getType(), param.isFinal(), false));
			}
		}

		if(result) {
		// Создаем MethodSymbol
			try {
				symbol = new MethodSymbol(	name, returnType, paramSymbols, modifiers.contains(Keyword.FINAL),
											modifiers.contains(Keyword.STATIC),	modifiers.contains(Keyword.NATIVE), canThrow, false, methodScope, this);
				// Устанавливаем обратную ссылку
				methodScope.setSymbol((MethodSymbol)symbol);

				// Проверяем, является ли scope ClassScope или InterfaceScope
				if(scope instanceof ClassScope) {
					ClassScope cScope = (ClassScope) scope;				
					// Добавляем метод или конструктор в область видимости класса
					if(isConstructor()) {
						cScope.addConstructor((MethodSymbol)symbol);
					}
					else {
						// Для обычных методов
						cScope.addMethod((MethodSymbol)symbol);
					}
				}
				else if(scope instanceof InterfaceScope) {
					try{
						validateModifiers(modifiers, Keyword.PUBLIC);
					}
					catch(CompileException e) {
						addMessage(e);
						result = false;
					}
					if(result) {
						InterfaceScope iScope = (InterfaceScope) scope;
						modifiers.add(Keyword.PUBLIC); // Гарантируем, что метод public
						iScope.addMethod((MethodSymbol)symbol);
					}
				}
			}
			catch(CompileException e) {
				markError(e);
				result = false;
			}

			if(result) {
				if(null!=blockNode) {
					if (!(scope instanceof ClassScope)) { //TODO ввести подержу статики
						markError("Method '" + name + "' cannot have body in interface '" + ((InterfaceScope)scope).getName() + "'");
						return false;
					}
					result&=blockNode.declare(methodScope);
				}
			}
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());

		if(null==returnType) {
			cgScope = cg.enterConstructor();
		}
		else {
			cgScope = cg.enterMethod(returnType, name);
		}
		symbol.setCGScope(cgScope);

		if(modifiers.contains(Keyword.NATIVE) && blockNode!=null) {
			markError("Native method cannot have a body");
			result = false;
		}

		if(result) {
			for(ParameterNode param : parameters) {
				result&=param.postAnalyze(methodScope, cg);
			}
		}
		
		if(result) {
			// Анализ тела метода (если есть)
			if(null!=blockNode) {
				// Анализируем тело метода
				result&=blockNode.postAnalyze(methodScope, cg);
			}
		}

		if(result) {
			// Для не-void методов проверяем наличие return
			// TODO переосмыслить после ConstantFolding
			if(null!=returnType && !returnType.equals(VarType.VOID)) {
				if(!BlockNode.hasReturnStatement(blockNode)) {
					markError("Method '" + name + "' must return a value");
					result = false;
				}
			}
		}

		if(result) {
			if(null!=blockNode) {
				List<AstNode> nodes = blockNode.getChildren();
				for(int i=0; i<nodes.size(); i++) {
					if(i>0 && isControlFlowInterrupted(nodes.get(i-1))) {
						markError("Unreachable code in method " + name);
						result = false;
						break;
					}
				}
			}
		}

		if(result) {
			VarType[] types = null;
			if(!parameters.isEmpty()) {
				types = new VarType[parameters.size()];
				for(int i=0; i<parameters.size(); i++) {
					types[i] = parameters.get(i).getType();
				}
			}
			((CGMethodScope)cgScope).setTypes(types);
		}

		((CGClassScope)cgScope.getParent()).addMethod((CGMethodScope)cgScope);

		if(result) {
			if(!isNative()) {
				// Создаем CGScope для каждого параметра
				CGMethodScope mScope = (CGMethodScope)cgScope;
				int offset = 0;
				for(Symbol pSymbol : ((MethodSymbol)symbol).getParameters()) {
					try {
						int size = (-1==pSymbol.getType().getSize() ? cg.getRefSize() : pSymbol.getType().getSize());
						// Создаем CGScope для параметра
						CGVarScope pScope = cg.enterLocal(pSymbol.getType(), size, false, pSymbol.getName());
						pScope.setCells(new CGCells(CGCells.Type.ARGS, size, offset));
						offset+=size;
						cg.leaveLocal();
						// Устанавливаем CGScope для символа параметра
						pSymbol.setCGScope(pScope);
					}
					catch (CompileException ex) {
						markError(ex);
						result = false;
					}
				}
			}
		}
		
		if(null==returnType) {
			cg.leaveConstructor();
		}
		else {
			cg.leaveMethod();
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		for(AstNode node : parameters) {
			if(!node.isDisabled()) {
				node.codeOptimization(methodScope, cg);
			}
		}
		
		if(null!=blockNode) {
			blockNode.codeOptimization(methodScope, cg);
		}
		
		cg.setScope(oldScope);
	}

	public void firstCodeGen(CodeGenerator cg) throws CompileException {
		cgDone = true;
		
		if(null != blockNode) {
			blockNode.firstCodeGen(cg);
		}

		objTypeNode.codeGen(cg, null, false);		

		if(!(objTypeNode instanceof InterfaceNode)) {
			((CGMethodScope)cgScope).build(cg);
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		if(null != blockNode) blockNode.codeGen(cg, null, false);
		
		objTypeNode.codeGen(cg, null, false);

		if(!(objTypeNode instanceof InterfaceNode)) {
			((CGMethodScope)cgScope).build(cg);
		}
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
	
	@Override
	public String toString() {
		return	StrUtils.toString(modifiers) + " " + returnType + " " + name + "(" + StrUtils.toString(parameters) + ")" +
				(canThrow ? " throws" : "");
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.ImplementInfo;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.ImportableScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;

public class ClassNode extends ObjectTypeNode {
	private	ClassBlockNode	blockNode;
	private	boolean			isInner;
	
	public ClassNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, boolean isInner, List<ObjectTypeNode> importedClasses)
																																	throws CompileException {
		super(tb, mc, modifiers, null, importedClasses);
		
		if(null!=name) {
			VarType.addClassName(this.name, false);
		}
		
		this.isInner = isInner;
		
		// Проверка на запрещенное наследование классов
		if(tb.match(TokenType.OOP, J8BKeyword.EXTENDS)) {
			markError("Class inheritance is not supported. Use composition instead of extends.");
			tb.skip(Delimiter.RIGHT_BRACE);
			return;
		}

		// Парсинг интерфейсов (если есть)
		if(tb.match(TokenType.OOP, J8BKeyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					//TODO QualifiedPath
					impl.add((String)consumeToken(tb, TokenType.IDENTIFIER).getValue());
				}
				catch(CompileException e) {
					markFirstError(e); // встретили что-то кроме ID интерфейса
				}
				if(!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}

		// Парсинг тела класса
		blockNode = new ClassBlockNode(tb, mc, this);
	}
	
	@Override
	public ClassBlockNode getBody() {
		return blockNode;
	}
	public void setBody(ClassBlockNode body) {
		blockNode = body;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + impl;
	}

	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(CompileException e) {addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Class name should start with uppercase letter:" + name, sp));
		}
		
		try{validateModifiers(modifiers, J8BKeyword.PUBLIC, J8BKeyword.PRIVATE, J8BKeyword.STATIC);} catch(CompileException e) {addMessage(e);}

		if(null!=importedClasses) {
			for (ObjectTypeNode imported : importedClasses) {
				imported.preAnalyze();
			}
		}

		
		// Анализ тела класса
		blockNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		try {
			ciScope = new ClassScope(scope, name);
			if(null!=scope) {
				((ImportableScope)scope).addCI(ciScope, true);
			}

			if(null!=importedClasses) {
				for(ObjectTypeNode imported : importedClasses) {
					result&=imported.declare(ciScope);

					if(result) {
						try {
							ciScope.addCI(((ObjectTypeNode)imported).getScope(), false);
						}
						catch(CompileException ex) {
							markError(ex);
							result = false;
						}
					}
				}
			}

			List<VarType> implTypes = new ArrayList<>();
			for(String ifaceName : impl) {
				if(null==scope.resolveCI(ifaceName, false)) {
					markError("Interface not found: " + ifaceName);
					result = false;
				}
				else {
					implTypes.add(VarType.fromClassName(ifaceName));
				}
			}
			
			if(result) {
				ciScope.setImplTypes(implTypes);
			}

			if(result) {
				result&=blockNode.declare(ciScope);
			}
		}
		catch(CompileException e) {
			markError(e);
			return false;
		}

		return result;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null != importedClasses) {
			for (ObjectTypeNode imported : importedClasses) {
				imported.postAnalyze(ciScope, cg); // Заменил scope на classScope, надо проверить
			}
		}

		List<ImplementInfo> implInfos = new ArrayList<>();
		if(!impl.isEmpty()) {
			List<InterfaceScope> iScopes = new ArrayList<>();
			for(String ifaceName : impl) {
				fillInterfaces(iScopes, ifaceName);
			}	
			for(InterfaceScope iScope : iScopes) {
				VarType iType = VarType.fromClassName(iScope.getName());
				List<String> signatures = new ArrayList<>();
				for(MethodSymbol mSymbol : iScope.getMethods()) {
					signatures.add(mSymbol.getSignature());
				}
				implInfos.add(new ImplementInfo(iType, signatures));
			}
			
			Collections.sort(implInfos, new Comparator<ImplementInfo>() {
				@Override
				public int compare(ImplementInfo ii1, ImplementInfo ii2) {
					return Integer.compare(ii1.getType().getId(), ii2.getType().getId());
				}
			});
		}

		cgScope = cg.enterClass(VarType.fromClassName(name), name, implInfos, null == parentClassName);

		blockNode.postAnalyze(ciScope, cg);

		cg.leaveClass();
		return true;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		blockNode.codeOptimization(ciScope, cg);
		
		cg.setScope(oldScope);
	}
	
	private void fillInterfaces(List<InterfaceScope> iScopes, String ifaceName) {
		CIScope cis = ciScope.resolveCI(ifaceName, false);
		if(null==cis || !(cis instanceof InterfaceScope)) {
			markError("Interface not found: " + ifaceName);
		}
		else if(!iScopes.contains((InterfaceScope)cis)) {
			checkInterfaceImplementation(ciScope, (InterfaceScope)cis);
			iScopes.add((InterfaceScope)cis);
			for(VarType type : cis.getImpl()) {
				fillInterfaces(iScopes, type.getClassName());
			}
		}
	}
	
	private boolean checkInterfaceImplementation(CIScope classScope, InterfaceScope interfaceSymbol) {
		boolean allMethodsImplemented = true;
		
		// Для каждого метода в интерфейсе
		for(MethodSymbol interfaceMethod : interfaceSymbol.getMethods()) {
			boolean found = false;
			// Получаем методы класса с таким же именем
			List<MethodSymbol> classMethods = classScope.getMethods(interfaceMethod.getName());
			
			// Проверяем каждый метод класса
			if(null!=classMethods) {
				for(MethodSymbol classMethod : classMethods) {
					if(interfaceMethod.getSignature().equals(classMethod.getSignature())) {
						classMethod.setInterfaceImpl(true);
						found = true;
						break;
					}
				}
			}

			if(!found) {
				markError(	"Class '" + classScope.getName() + "' must implement method: " + interfaceMethod.getSignature() + 
							" from interface '" + interfaceSymbol.getName() + "'");
				allMethodsImplemented = false;
			}
		}
		return allMethodsImplemented;
	}
	
	public void firstCodeGen(CodeGenerator cg, MethodNode methodNode, CGExcs excs) throws CompileException {
		cgDone = true;
		
		methodNode.firstCodeGen(cg, excs);
		
		((CGClassScope)cgScope).build(cg, excs);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;
		
		((CGClassScope)cgScope).build(cg, excs);
/*		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				if(isUsed()) imported.codeGen(cg);
			}
		}
*/

		return null;
	}
	
	public boolean isStatic() {
		return modifiers.contains(J8BKeyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(J8BKeyword.PUBLIC);
	}
	public boolean isPrivate() {
		return modifiers.contains(J8BKeyword.PRIVATE);
	}

	public boolean isInner() {
		return isInner;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
}

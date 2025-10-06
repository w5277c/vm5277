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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class ClassNode extends AstNode {
	private		final	List<ClassNode>	importedClasses;
	protected	final	Set<Keyword>	modifiers;
	protected			String			name;
	private				String			parentClassName;
	protected			List<String>	impl			= new ArrayList<>();
	private				ClassBlockNode	blockNode;
	private				ClassScope		classScope;
	
	public ClassNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, String parentClassName, List<ClassNode> importedClasses)
																																	throws CompileException {
		super(tb, mc);
		
		this.importedClasses = importedClasses;
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name);
		}
		catch(CompileException e) {markFirstError(e);} // ошибка в имени, оставляем null
		
        // Парсинг интерфейсов (если есть)
		if (tb.match(TokenType.OOP, Keyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					impl.add((String)consumeToken(tb, TokenType.ID).getValue());
				}
				catch(CompileException e) {markFirstError(e);} // встретили не ID интерфейса, пропускаем
				if (!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}
	}
	
	public void parse() throws CompileException {
		// Парсинг тела класса
		blockNode = new ClassBlockNode(tb, mc, this);
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}
	
	public ClassBlockNode getBody() {
		return blockNode;
	}
	public void setBody(ClassBlockNode body) {
		blockNode = body;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "class";
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
		
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC);} catch(CompileException e) {addMessage(e);}

		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				imported.preAnalyze();
			}
		}

		
		// Анализ тела класса
		blockNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope parentScope) {
		boolean result = true;
		
		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				result &= imported.declare(parentScope);
			}
		}
		
		try {
			List<VarType> implTypes = new ArrayList<>();
			for(String ifaceName : impl) {
				implTypes.add(VarType.fromClassName(ifaceName));
			}
			classScope = new ClassScope(name, parentScope, implTypes);
			if(null != parentScope) ((ClassScope)parentScope).addClass(classScope);
			
			result &= blockNode.declare(classScope);
		}
		catch(CompileException e) {markError(e); return false;}

		return result;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				imported.postAnalyze(classScope, cg); // Заменил scope на classScope, надо проверить
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

		blockNode.postAnalyze(classScope, cg);

		cg.leaveClass();
		return true;
	}
	
	private void fillInterfaces(List<InterfaceScope> iScopes, String ifaceName) {
		InterfaceScope iScope = classScope.resolveInterface(ifaceName);
		if(null == iScope) {
			markError("Interface not found: " + ifaceName);
		}
		else if(!iScopes.contains(iScope)) {
			checkInterfaceImplementation(classScope, iScope);
			iScopes.add(iScope);
			for(VarType type : iScope.getImpl()) {
				fillInterfaces(iScopes, type.getClassName());
			}
		}
	}
	
	private boolean checkInterfaceImplementation(ClassScope classScope, InterfaceScope interfaceSymbol) {
		boolean allMethodsImplemented = true;
		
		// Для каждого метода в интерфейсе
		for (MethodSymbol interfaceMethod : interfaceSymbol.getMethods()) {
			boolean found = false;
			// Получаем методы класса с таким же именем
			List<MethodSymbol> classMethods = classScope.getMethods(interfaceMethod.getName());
			
			// Проверяем каждый метод класса
			if(null != classMethods) {
				for (MethodSymbol classMethod : classMethods) {
					if (interfaceMethod.getSignature().equals(classMethod.getSignature())) {
						found = true;
						break;
					}
				}
			}

			if (!found) {
				markError(	"Class '" + classScope.getName() + "' must implement method: " + interfaceMethod.getSignature() + 
							" from interface '" + interfaceSymbol.getName() + "'");
				allMethodsImplemented = false;
			}
		}
		return allMethodsImplemented;
	}
	
	public void firstCodeGen(CodeGenerator cg, MethodNode methodNode) throws Exception {
		cgDone = true;

		methodNode.firstCodeGen(cg);

		((CGClassScope)cgScope).build(cg);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone || disabled) return null;
		cgDone = true;

		((CGClassScope)cgScope).build(cg);
/*		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				if(isUsed()) imported.codeGen(cg);
			}
		}
*/
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockNode);
	}
}

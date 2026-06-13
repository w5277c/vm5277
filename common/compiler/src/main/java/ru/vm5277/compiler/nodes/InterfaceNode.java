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
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.compiler.semantic.ImportableScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.compiler.Instance;

public class InterfaceNode extends ObjectTypeNode {
	private			InterfaceBodyNode	blockIfaceNode;
	private			String				runtimePath;
	
	public InterfaceNode(Instance inst, TokenBuffer tb, Set<Keyword> modifiers, String classPath, List<ObjectTypeNode> importedClasses)
																																	throws CompileException {
		super(inst, tb, modifiers, classPath, importedClasses);

		if(null!=name) {
			VarType.addClassName(this.name, false);
		}

		// Проверка на запрещенное наследование классов
		if(tb.match(TokenType.OOP, J8BKeyword.IMPLEMENTS)) {
			markError("Interfaces cannot implement other interfaces. Use 'extends' for interface inheritance.");
			tb.skip(Delimiter.RIGHT_BRACE);
			return;
		}

		// Парсинг интерфейсов (если есть)
		if(tb.match(TokenType.OOP, J8BKeyword.EXTENDS)) {
			consumeToken(tb);
			while(true) {
				try {
					String interfaceName = (String)consumeToken(tb, TokenType.IDENTIFIER).getValue();
					resolveSameDirImport(inst, tb.getSP().getSourceFile(), interfaceName);
					//TODO QualifiedPath
					impl.add(interfaceName);
				}
				catch(CompileException e) {
					markFirstError(e); // встретили что-то кроме ID интерфейса
				}
				if(!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}

		// Парсинг тела интерфейса
		blockIfaceNode = new InterfaceBodyNode(inst, tb, name, this);
	}
	
	@Override
	public InterfaceBodyNode getBody() {
		return blockIfaceNode;
	}
	
	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(CompileException e) {addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Interface name should start with uppercase letter:" + name, sp));
		}
		
		try{validateModifiers(modifiers, J8BKeyword.PUBLIC);} catch(CompileException e) {addMessage(e);}
		
		// Анализ тела интерфейса
		blockIfaceNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		try {
			ciScope = new InterfaceScope(name, classPath, scope, null);
			if(null!=scope) {
				((ImportableScope)scope).addCI(ciScope, true);
			}

			if(null!=imported) {
				for(ObjectTypeNode imported : imported) {
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
				if(null==ciScope.resolveCI(null, ifaceName, false)) {
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
				result&=blockIfaceNode.declare(ciScope);
			}
		}
		catch(CompileException e) {
			markError(e);
			return false;
		}

/*		
		try {
			List<VarType> implTypes = new ArrayList<>();
			for(String ifaceName : impl) {
				if(null==ciScope.resolveCI(ifaceName, false)) {
					markError("Interface not found: " + ifaceName);
					result = false;
				}
				else {
					implTypes.add(VarType.fromClassName(ifaceName));
				}
			}
			
			String filePath = sp.getSourceFile().getAbsolutePath();
			String runtimeDir = Main.toolkitPath.resolve("runtime").normalize().toString();
			String runtimePath = null;
			if(filePath.startsWith(runtimeDir) && filePath.endsWith(".j8b")) {
				runtimePath = filePath.substring(runtimeDir.length()+1, filePath.length()-".j8b".length()).replace(File.separatorChar, '.');
			}
			ciScope = new InterfaceScope(name, runtimePath,	scope, implTypes);
			
			((ImportableScope)scope).addCI(ciScope, true);

			//TODO добавить импорты
			
			blockIfaceNode.declare(ciScope);
		} 
		catch (CompileException e) {
			markError(e);
		}*/
		return result;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		cgScope = cg.enterInterface(parent, VarType.fromClassName(name), name);

		// Проверка что интерфейс не содержит конструкторов
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
			if (decl instanceof MethodNode) {
				MethodNode method = (MethodNode)decl;
				if (method.isConstructor()) {
					markError("Interfaces cannot contain constructors");
					break;
				}
			}
		}

		
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
			if (decl instanceof FieldNode) { // Проверка что все поля - public static final и инициализированы
				FieldNode field = (FieldNode)decl;

				if (!field.getModifiers().contains(J8BKeyword.PUBLIC)) markError("Interface field '" + field.getName() + "' must be public");
				if (!field.getModifiers().contains(J8BKeyword.STATIC)) markError("Interface field '" + field.getName() + "' must be static");
				if (!field.getModifiers().contains(J8BKeyword.FINAL)) markError("Interface field '" + field.getName() + "' must be final");

				if (null == field.getInitializer()) markError("Interface field '" + field.getName() + "' must be initialized");
			}
			else if (decl instanceof MethodNode) { // Проверка что методы не имеют реализации
				MethodNode method = (MethodNode)decl;
				if (null != method.getBody()) {
					markError("Interface method '" + method.getName() + "' cannot have a body");
				}
			}
		}

		// Проверка вложенных интерфейсов
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
			if (decl instanceof InterfaceNode) {
				decl.postAnalyze(ciScope, cg, cgScope);
			}
		}

		return true;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		for(ObjectTypeNode node : imported) {
			node.codeOptimization(scope, cg);
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;
		
/* Уже есть в postAnalyze
int[] interfaceIds = null;
		if(!impl.isEmpty()) {
			interfaceIds = new int[impl.size()];
			for(int i=0; i<impl.size(); i++) {
				interfaceIds[i] = VarType.fromClassName(impl.get(i)).getId();
			}
		}
			
		cg.enterInterface(VarType.fromClassName(name), interfaceIds, name);
*/
		blockIfaceNode.codeGen(cg, false, excs);
		
/*		finally {
			cg.leaveInterface();
		}
*/		
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockIfaceNode);
	}
}


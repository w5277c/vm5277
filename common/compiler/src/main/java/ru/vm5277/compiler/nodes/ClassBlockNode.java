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
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class ClassBlockNode extends AstNode {
	protected	List<AstNode>	children	= new ArrayList<>();
	
	public ClassBlockNode(TokenBuffer tb, MessageContainer mc, ClassNode classNode) throws CompileException {
		super(tb, mc);
        
		consumeToken(tb, Delimiter.LEFT_BRACE); // в случае ошибки, останавливаем парсинг файла

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {		
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.CLASS)) {
				ClassNode cNode = new ClassNode(tb, mc, modifiers, null, null);
				cNode.parse();
				children.add(cNode);
				continue;
			}
			// Обработка интерфейсов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
				InterfaceNode iNode = new InterfaceNode(tb, mc, modifiers, null, null);
				iNode.parse();
				children.add(iNode);
				continue;
			}

			// Определение типа (примитив, класс или конструктор)
			VarType type = checkPrimtiveType();
			boolean isClassName = false;
			if (null == type) {
				isClassName = checkClassName(classNode.getName());
				if(!isClassName) type = checkClassType();
			}
			
			// Получаем имя метода/конструктора
			String name = null;
			if(tb.match(TokenType.ID)) {
				if(isClassName) {
					isClassName = false;
					type = VarType.fromClassName(classNode.getName());
				}
				name = consumeToken(tb).getStringValue();
			}

			if(tb.match(Delimiter.LEFT_PAREN)) { //'(' Это метод
				if(isClassName) {
					children.add(new MethodNode(tb, mc, modifiers, null, classNode.getName(), classNode));
				}
				else {
					children.add(new MethodNode(tb, mc, modifiers, type, name, classNode));
				}
				continue;
			}

			if(null != type) {
				if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
					children.add(new ArrayDeclarationNode(tb, mc, modifiers, type, name));
				}
				else { // Поле
					children.add(new FieldNode(tb, mc, modifiers, type, name));
				}
			}

			if(tb.match(Delimiter.LEFT_BRACE)) {
				tb.getLoopStack().add(this);
				try {
					children.add(new BlockNode(tb, mc));
				}
				catch(CompileException e) {}
				tb.getLoopStack().remove(this);
			}
		}
		
		//Попытка потребить '}'
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(CompileException e) {markFirstError(e);}
    }

	public ClassBlockNode(MessageContainer mc) throws CompileException {
		super(null, mc);
	}
	
	@Override
	public String getNodeType() {
		return "class body";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка всех объявлений в блоке
		for (AstNode node : children) {
			node.preAnalyze(); // Не реагируем на критические ошибки
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		for (AstNode node : children) {
			node.declare(scope);
		}
		return true;
	}

	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		ClassScope classScope = (ClassScope)scope;
		
		// Проверка наличия конструктора
		if (checkNonStaticMembers(classScope)) {
			List<MethodSymbol> constructors = classScope.getConstructors();
			if(null == constructors || constructors.isEmpty()) {
				//Есть не статические методы, но нет конструктора
				try {
					MethodScope methodScope = new MethodScope(null, classScope);
					MethodSymbol methodSymbol = new MethodSymbol(classScope.getName(), null, new ArrayList<>(), false, false, false, false, methodScope, null);
					methodScope.setSymbol(methodSymbol);
					classScope.addConstructor(methodSymbol);
					markWarning("Class must have at least one constructor");
				}
				catch(CompileException e) {markError(e);}
			}
		}
		
		for (AstNode node : children) {
			node.postAnalyze(scope, cg);
		}
		return true;
	}
	
	private boolean checkNonStaticMembers(ClassScope classScope) {
		// Проверка полей
		for (Symbol field : classScope.getFields().values()) {
			if (!field.isStatic()) return true;
		}

		// Проверка методов
		for (MethodSymbol method : classScope.getMethods()) {
			if (!method.isStatic()) return true;
		}

		return false;
	}

	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;
		
		for(AstNode node : children) {
			node.codeGen(cg);
		}
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return children;
	}
}
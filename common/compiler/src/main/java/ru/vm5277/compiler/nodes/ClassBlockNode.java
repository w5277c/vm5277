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
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class ClassBlockNode extends AstNode {
	protected	List<AstNode>	children	= new ArrayList<>();
	private		ClassNode		classNode;
	
	public ClassBlockNode(TokenBuffer tb, MessageContainer mc, ClassNode classNode) throws CompileException {
		super(tb, mc);
        
		this.classNode = classNode;
		
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
			if(null != type) type = checkArrayType(type);
			
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
					//TODO рудимент?
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
		boolean result = true;
		ClassScope classScope = (ClassScope)scope;
		
		for (AstNode node : children) {
			result &= node.declare(scope);
		}
		
		// Проверка наличия конструктора
		if (checkNonStaticMembers(classScope)) {
			List<MethodSymbol> constructors = classScope.getConstructors();
			if(null == constructors || constructors.isEmpty()) {
				MethodNode mNode = new MethodNode(classNode);
				mNode.declare(scope);
				children.add(mNode);
				markWarning("Class must have at least one constructor");
			}
		}
		
		return result;
	}

	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		
		for (AstNode node : children) {
			result &= node.postAnalyze(scope, cg);
		}
		return result;
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
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone || disabled) return null;
		cgDone = true;
		
		for(AstNode node : children) {
			node.codeGen(cg, null, false);
		}
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return children;
	}
}
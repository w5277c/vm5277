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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.commands.CommandNode.AstCase;
import ru.vm5277.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.compiler.nodes.commands.ForNode;
import ru.vm5277.compiler.nodes.commands.IfNode;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.commands.SwitchNode;
import ru.vm5277.compiler.nodes.commands.TryNode;
import ru.vm5277.compiler.nodes.commands.WhileNode;
import ru.vm5277.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class BlockNode extends AstNode {
	private	List<AstNode>			children	= new ArrayList<>();
	private	Map<String, LabelNode>	labels		= new HashMap<>();
	private	BlockScope				blockScope;
	private	CGBlockScope			cgScope;
	
	public BlockNode() {
	}
	
	public BlockNode(TokenBuffer tb, MessageContainer mc, AstNode singleStatement) {
		super(tb, mc);
		
		children.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb, MessageContainer mc) throws CompileException {
		this(tb, mc, false);
	}
	public BlockNode(TokenBuffer tb, MessageContainer mc, boolean leftBraceConsumed) throws CompileException {
		super(tb, mc);
        
		if(!leftBraceConsumed) consumeToken(tb, Delimiter.LEFT_BRACE); //Наличие токена должно быть гарантировано вызывающим

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			if(tb.match(TokenType.LABEL)) {
				LabelNode label = new LabelNode(tb, mc);
				labels.put(label.getName(), label);
				children.add(label);
			}
			if (tb.match(TokenType.COMMAND)) {
				try {
					children.add(parseCommand());
				}
				catch(CompileException e) {
					addMessage(e.getErrorMessage());
					markFirstError(e);
				} // Фиксируем ошибку(Unexpected command token)
				continue;
			}
			if(tb.match(Keyword.FREE)) {
				children.add(new FreeNode(tb, mc));
				continue;
			}
			
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
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

			try {
				// Определение типа (примитив или класс)
				VarType type = checkPrimtiveType();
				if (null == type) type = checkClassType();


				if(null != type) {
					if(tb.match(Delimiter.DOT)) {
						while (tb.match(Delimiter.DOT)) {
							tb.consume();
							
							String methodName = (String)consumeToken(tb, TokenType.ID).getValue();
							if (tb.match(Delimiter.LEFT_PAREN)) {
								// Это вызов метода
								children.add(	new MethodCallExpression(tb, mc, new TypeReferenceExpression(tb, mc, type.getClassName()), methodName,
													parseArguments(tb)));
								consumeToken(tb, Delimiter.SEMICOLON);
								break;
							}
							else {
								// Доступ к полю (можно добавить FieldAccessNode)
								throw new CompileException("Field access not implemented yet", tb.current().getSP());
							}
						}
						continue;
					}
					else {
						// Получаем имя метода/конструктора
						String name = null;
						try {name = consumeToken(tb, TokenType.ID).getStringValue();}catch(CompileException e) {markFirstError(e);} // Нет имени сущности, пытаемся парсить дальше

						if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
							ArrayDeclarationNode node = new ArrayDeclarationNode(tb, mc, modifiers, type, name);
							if(null != name) children.add(node);
						}
						else { // Переменная
							VarNode varNode = new VarNode(tb, mc, modifiers, type, name);
							if(null != name) children.add(varNode);
						}
						continue;
					}
				}
			}
			catch(CompileException e) {
				markFirstError(e);
			}

			// Обработка остальных statement (if, while, вызовы и т.д.)
			try {
				children.add(parseStatement());
			}
			catch(CompileException e) {
				markFirstError(e);
			}
		}
		consumeToken(tb, Delimiter.RIGHT_BRACE);
    }

	public static  boolean hasReturnStatement(AstNode node) {
		if (null == node) return false;

		// Если это return-выражение
		if (node instanceof ReturnNode) return true;

		// Проверяем дочерние блоки
		if(null != node.getChildren()) {
			for (AstNode _node : node.getChildren()) {
				if (hasReturnStatement(_node)) {
					return true;
				}
			}
		}

		// Особые случаи для управляющих конструкций
		if (node instanceof IfNode) {
			IfNode ifNode = (IfNode)node;
			// Проверяем обе ветки if
			return hasReturnStatement(ifNode.getThenBlock()) && (ifNode.getElseBlock() == null || hasReturnStatement(ifNode.getElseBlock()));
		}
		else if (node instanceof SwitchNode) {
			// Для switch нужно проверить все case-блоки
			SwitchNode switchNode = (SwitchNode)node;
			for (AstCase c : switchNode.getCases()) {
				if (!hasReturnStatement(c.getBlock())) {
					return false;
				}
			}
			return true;
		}
		else if(node instanceof DoWhileNode) {
			DoWhileNode dowhileNode = (DoWhileNode)node;
			return hasReturnStatement(dowhileNode.getBody());
		}
		else if(node instanceof WhileNode) {
			WhileNode whileNode = (WhileNode)node;
			return hasReturnStatement(whileNode.getBody());
		}
		else if(node instanceof ForNode) {
			ForNode forNode = (ForNode)node;
			return hasReturnStatement(forNode.getBody()) && (forNode.getElseBlock() == null || hasReturnStatement(forNode.getElseBlock()));
		}
		return false;
	}
	
	public Map<String, LabelNode> getLabels() {
		return labels;
	}
	
	@Override
	public String getNodeType() {
		return "code block";
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
		// Создаем новую область видимости для блока
		blockScope = new BlockScope(scope);

		// Объявляем все элементы в блоке
		for (AstNode node : children) {
			node.declare(blockScope);
		}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		cgScope = cg.enterBlock();
		
		boolean inTryBlock = false;
		TryNode currentTryNode = null;

		for (int i = 0; i < children.size(); i++) {
			AstNode node = children.get(i);

			// Анализируем текущую ноду
			node.postAnalyze(blockScope, cg);
			
			// Если нашли try-block, отмечаем начало зоны обработки исключений
			if (node instanceof TryNode) {
				currentTryNode = (TryNode)node;
				inTryBlock = true;
			}

			// Проверка вызовов методов
			if (node instanceof MethodCallExpression) {
				MethodCallExpression call = (MethodCallExpression)node;
				MethodSymbol methodSymbol = (MethodSymbol)call.getSymbol();

				if (null != methodSymbol && methodSymbol.canThrow() && !inTryBlock) {
					markWarning("Call to throwing method '" + methodSymbol.getName() + "' without try-catch at line " + call.getSP());
				}
			}

			// Если это конец try-block, сбрасываем флаг
			if (inTryBlock && node == currentTryNode.getEndNode()) {
				inTryBlock = false;
				currentTryNode = null;
			}

			// Проверяем недостижимый код после прерывающих инструкций
			if (i > 0 && isControlFlowInterrupted(children.get(i - 1))) {
				markError("Unreachable code after " + children.get(i - 1).getClass().getSimpleName());
				// Можно пропустить анализ остального кода, так как он недостижим
				break;
			}
		}
		
		cg.leaveBlock();
		return true;
	}

	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;
		
		cgScope.build(cg);
		
		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!(node instanceof VarNode)) {
				node.codeGen(cg);
			}
		}
		
		cgScope.restoreRegsPool();
		
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public CGBlockScope getCGScope() {
		return cgScope;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return children;
	}
}
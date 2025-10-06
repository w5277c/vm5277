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
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import ru.vm5277.compiler.nodes.commands.CommandNode.AstCase;
import ru.vm5277.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.compiler.nodes.commands.ForNode;
import ru.vm5277.compiler.nodes.commands.IfNode;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.commands.SwitchNode;
import ru.vm5277.compiler.nodes.commands.TryNode;
import ru.vm5277.compiler.nodes.commands.WhileNode;
import ru.vm5277.compiler.nodes.expressions.ArrayInitExpression;
import ru.vm5277.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.compiler.nodes.expressions.CastExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.compiler.nodes.expressions.TernaryExpression;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.UnresolvedReferenceExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class BlockNode extends AstNode {
	private	List<AstNode>			children	= new ArrayList<>();
	private	Map<String, LabelNode>	labels		= new HashMap<>();
	private	BlockScope				blockScope;
	
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
				if(null == type) type = checkClassType();
				if(null != type) type = checkArrayType(type);

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
							//TODO рудимент?
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
		boolean result = true;
		
		// Проверка всех объявлений в блоке
		for (AstNode node : children) {
			if(node instanceof ExpressionNode) {
				boolean noEffect =	node instanceof CastExpression || node instanceof InstanceOfExpression || node instanceof UnresolvedReferenceExpression ||
									node instanceof ArrayInitExpression || node instanceof LiteralExpression || node instanceof TernaryExpression;
				if(!noEffect && node instanceof UnaryExpression) {
					Operator op = ((UnaryExpression)node).getOperator();
					noEffect = Operator.POST_DEC!=op && Operator.POST_INC!=op && Operator.PRE_DEC!=op && Operator.PRE_INC!=op;
				}
				if(!noEffect && node instanceof BinaryExpression) {
					noEffect = !((BinaryExpression)node).getOperator().isAssignment();
				}
				
				if(noEffect) {
					node.disable();
					String text = "Statement has no practical effect";
					if(AssemblerInterface.STRICT_STRONG == Main.getStrictLevel()) {
						markError(text);
						result = false;
					}
					else if(AssemblerInterface.STRICT_LIGHT == Main.getStrictLevel()) {
						markWarning(text);
					}
				}
			}
			result &= node.preAnalyze();
		}
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		// Создаем новую область видимости для блока
		blockScope = new BlockScope(scope);

		// Объявляем все элементы в блоке
		for (AstNode node : children) {
			if(!node.isDisabled()) {
				result &= node.declare(blockScope);
			}
		}

		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterBlock();
		
		boolean inTryBlock = false;
		TryNode currentTryNode = null;

		for (int i = 0; i < children.size(); i++) {
			AstNode node = children.get(i);
			if(node.isDisabled()) continue;
			
			sp = node.getSP();
			
			// Анализируем текущую ноду
			result&=node.postAnalyze(blockScope, cg);

			if(node instanceof ExpressionNode) {
				try {
					ExpressionNode optimizedNode = ((ExpressionNode)node).optimizeWithScope(blockScope, cg);
					if(null != optimizedNode) {
						node = optimizedNode;
						node.postAnalyze(blockScope, cg);
						children.set(i, node);
					}
				}
				catch(Exception ex) {}
			}
			
			// Если нашли try-block, отмечаем начало зоны обработки исключений
			if(node instanceof TryNode) {
				currentTryNode = (TryNode)node;
				inTryBlock = true;
			}

			// Проверка вызовов методов
			if(node instanceof MethodCallExpression) {
				MethodCallExpression call = (MethodCallExpression)node;
				MethodSymbol methodSymbol = (MethodSymbol)call.getSymbol();

				if (null != methodSymbol && methodSymbol.canThrow() && !inTryBlock) {
					markWarning("Call to throwing method '" + methodSymbol.getName() + "' without try-catch at line " + call.getSP());
				}
			}

			// Если это конец try-block, сбрасываем флаг
			if(inTryBlock && node == currentTryNode.getEndNode()) {
				inTryBlock = false;
				currentTryNode = null;
			}

			// Проверяем недостижимый код после прерывающих инструкций
			if(i > 0 && isControlFlowInterrupted(children.get(i - 1))) {
				markError("Unreachable code after " + children.get(i - 1).getClass().getSimpleName());
				// Можно пропустить анализ остального кода, так как он недостижим
				break;
			}
		}
		
		cg.leaveBlock();
		return result;
	}

	
	public void firstCodeGen(CodeGenerator cg) throws Exception {
		cgDone = true;
		
		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!node.isDisabled() && !(node instanceof VarNode)) {
				if(node instanceof ExpressionNode) {
					((ExpressionNode)node).codeGen(cg, null, false);
				}
				else {
					node.codeGen(cg, null, false);
				}
			}
		}
		
		((CGBlockScope)cgScope).build(cg, true);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone || disabled) return null;
		cgDone = true;
		
		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!(node instanceof VarNode)) {
				node.codeGen(cg, null, false);
			}
		}
		
		((CGBlockScope)cgScope).build(cg, false);
		((CGBlockScope)cgScope).restoreRegsPool();
		
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public CGBlockScope getCGScope() {
		return ((CGBlockScope)cgScope);
	}
	
	@Override
	public List<AstNode> getChildren() {
		return children;
	}
}
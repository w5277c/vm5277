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
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.commands.CommandNode.AstCase;
import ru.vm5277.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.compiler.nodes.commands.ForNode;
import ru.vm5277.compiler.nodes.commands.IfNode;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.commands.SwitchNode;
import ru.vm5277.compiler.nodes.commands.WhileNode;
import ru.vm5277.compiler.nodes.expressions.ArrayInitExpression;
import ru.vm5277.compiler.nodes.expressions.CastExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.TernaryExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;

public class BlockNode extends AstNode {
	protected	List<AstNode>			children	= new ArrayList<>();
	private		Map<String, LabelNode>	labels		= new HashMap<>();
	private		boolean					isTry		= false;
	protected	BlockScope				blockScope;
	private		String					comment		= "";
	
	public BlockNode(String comment) {
		this.comment = comment;
	}
	
	public BlockNode(TokenBuffer tb, MessageContainer mc, AstNode singleStatement, String comment) {
		super(tb, mc);
		
		this.comment = comment;
		children.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb, MessageContainer mc, String comment) throws CompileException {
		this(tb, mc, false, false, comment);
	}
	public BlockNode(TokenBuffer tb, MessageContainer mc, boolean leftBraceConsumed, boolean isTry, String comment) throws CompileException {
		super(tb, mc);
        
		this.isTry = isTry;
		this.comment = comment;
		
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
			if (tb.match(TokenType.OOP) && J8BKeyword.CLASS == tb.current().getValue()) {
				ClassNode cNode = new ClassNode(tb, mc, modifiers, true, null);
				children.add(cNode);
				continue;
			}
			// Обработка enum с модификаторами
			if (tb.match(TokenType.OOP) && J8BKeyword.ENUM == tb.current().getValue()) {
				EnumNode eNode = new EnumNode(tb, mc, modifiers);
				children.add(eNode);
				continue;
			}
			// Обработка интерфейсов с модификаторами
			if (tb.match(TokenType.OOP, J8BKeyword.INTERFACE)) {
				InterfaceNode iNode = new InterfaceNode(tb, mc, modifiers, null, null);
				children.add(iNode);
				continue;
			}
			// Обработка exception с модификаторами
			if (tb.match(TokenType.OOP, J8BKeyword.EXCEPTION)) {
				ExceptionNode eNode = new ExceptionNode(tb, mc, modifiers, null);
				children.add(eNode);
				continue;
			}

			try {
				// Определение типа (примитив или класс)
				VarType type = checkPrimtiveType();
				if(null==type) type = checkClassType();
				if(null!=type) type = checkArrayType(type);

				if(null!=type) {
					if(tb.match(Delimiter.DOT)) {
						tb.back();
						ExpressionNode expr = new ExpressionNode(tb, mc).parse();
						consumeToken(tb, Delimiter.SEMICOLON);
						children.add(expr);
						continue;
					}
					else {
						// Получаем имя метода/конструктора
						String name = null;
						try {name = consumeToken(tb, TokenType.IDENTIFIER).getStringValue();}catch(CompileException e) {markFirstError(e);} // Нет имени сущности, пытаемся парсить дальше

						VarNode varNode = new VarNode(tb, mc, modifiers, type, name);
						if(null!=name) children.add(varNode);
						continue;
					}
				}
			}
			catch(CompileException e) {
				//markFirstError(e);
				markError(e);
			}

			// Обработка остальных statement (if, while, вызовы и т.д.)
			try {
				children.add(parseStatement());
			}
			catch(CompileException e) {
				markFirstError(e);
				tb.skip(Delimiter.SEMICOLON, Delimiter.RIGHT_BRACE);
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
			return hasReturnStatement(dowhileNode.getChildren().get(0));
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
	
	public BlockScope getScope() {
		return blockScope;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		// Проверка всех объявлений в блоке
		for (AstNode node : children) {
			if(node instanceof ExpressionNode) {
				boolean noEffect =	node instanceof CastExpression || node instanceof InstanceOfExpression ||
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
		for(AstNode node : children) {
			if(!node.isDisabled()) {
				result&=node.declare(blockScope);
			}
		}

		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null!=postResult) return postResult;
		postResult = true;

		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterBlock(isTry, comment);
		
		for(int i=0; i<children.size(); i++) {
			AstNode node = children.get(i);
			if(node.isDisabled()) continue;
			
			sp = node.getSP();
			
			// Анализируем текущую ноду
			postResult&=node.postAnalyze(blockScope, cg);
			if(postResult) {
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

				// Проверяем недостижимый код после прерывающих инструкций
				if(i > 0 && isControlFlowInterrupted(children.get(i - 1))) {
					markWarning("Unreachable code after " + children.get(i - 1).getClass().getSimpleName());
					// Можно пропустить анализ остального кода, так как он недостижим
					break;
				}
			}
		}
		
		cg.leaveBlock();
		debugAST(this, POST, false, postResult, getFullInfo());
		return postResult;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		for(AstNode node : children) {
			if(!node.isDisabled()) {
				node.codeOptimization(blockScope, cg);
			}
		}
		cg.setScope(oldScope);
	}
	
	public void firstCodeGen(CodeGenerator cg, CGExcs excs) throws CompileException {
		cgDone = true;
		int producedQnt = excs.getProduced().size();
		
		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!node.isDisabled() && !(node instanceof VarNode)) {
				if(node instanceof ExpressionNode) {
					((ExpressionNode)node).codeGen(cg, null, false, excs);
				}
				else {
					node.codeGen(cg, null, false, excs);
				}
			}
		}
		
		((CGBlockScope)cgScope).build(cg, true, excs);
/*		if(producedQnt!=excs.getProduced().size()) {
			CGMethodScope mScope = (CGMethodScope)cgScope.getScope(CGMethodScope.class);
			cg.getTargetInfoBuilder().addExcsThrowPoint(cg, sp, mScope.getSignature());
		}*/
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;
		
		CGScope cgs = null == parent ? cgScope : parent;

		for(AstNode node : children) {
			//Не генерирую безусловно переменные, они будут сгенерированы только при обращении
			if(!(node instanceof VarNode)) {
				node.codeGen(cg, cgs, false, excs);
			}
		}
		
		((CGBlockScope)cgScope).build(cg, false, excs);
		((CGBlockScope)cgScope).restoreRegsPool();
		
		return null;
	}
	
	@Override
	public String toString() {
		return "";
	}

	public String getFullInfo() {
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

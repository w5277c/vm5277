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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.compiler.nodes.commands.IfNode;
import ru.vm5277.compiler.nodes.commands.ReturnNode;
import ru.vm5277.compiler.nodes.commands.WhileNode;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.compiler.nodes.commands.BreakNode;
import ru.vm5277.compiler.nodes.commands.ContinueNode;
import ru.vm5277.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.compiler.nodes.commands.ForNode;
import ru.vm5277.compiler.nodes.commands.SwitchNode;
import ru.vm5277.compiler.SemanticAnalyzer;
import ru.vm5277.common.messages.Message;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.compiler.nodes.commands.CommandNode.AstCase;
import ru.vm5277.compiler.nodes.commands.TryNode;
import ru.vm5277.compiler.nodes.expressions.QualifiedPathExpression;
import ru.vm5277.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.tokens.Token;

public abstract class AstNode extends SemanticAnalyzer {
    private				static	int						globalCntr	= 0;
    protected					int						sn;
	protected					TokenBuffer				tb;
	protected					SourcePosition			sp;
	private						ErrorMessage			error;
	protected					MessageContainer		mc;
	protected					Symbol					symbol;
	protected					boolean					cgDone;
	protected					Boolean					postResult;
	protected					boolean					disabled;
	protected					CGScope					cgScope;
	protected			static	HashMap<AstNode, Scope>	declarationPendingNodes	= new HashMap<>();
	
	protected AstNode() {
		sn = globalCntr++;
	}
	
	protected AstNode(TokenBuffer tb, MessageContainer mc) {
		sn = globalCntr++;
		
		this.tb = tb;
		this.sp = (null==tb ? null : tb.current().getSP());
		this.mc = mc;
    }

	protected AstNode(TokenBuffer tb, MessageContainer mc, SourcePosition sp) {
		sn = globalCntr++;
		
		this.tb = tb;
		this.sp = sp;
		this.mc = mc;
    }

	protected AstNode parseCommand() throws CompileException {
		Keyword kw = (Keyword)tb.current().getValue();
		if(J8BKeyword.IF == kw) return new IfNode(tb, mc);
		if(J8BKeyword.FOR == kw) return new ForNode(tb, mc);
		if(J8BKeyword.DO == kw) return new DoWhileNode(tb, mc);
		if(J8BKeyword.WHILE == kw) return new WhileNode(tb, mc);
		if(J8BKeyword.CONTINUE == kw) return new ContinueNode(tb, mc);
		if(J8BKeyword.BREAK == kw) return new BreakNode(tb, mc);
		if(J8BKeyword.RETURN == kw) return new ReturnNode(tb, mc);
		if(J8BKeyword.SWITCH == kw) return new SwitchNode(tb, mc);
		if(J8BKeyword.TRY == kw) return new TryNode(tb, mc);
		markFirstError(error);
		throw new CompileException("Unexpected command token " + tb.current(), sp);
	}

	protected AstNode parseStatement() throws CompileException {
		sp = tb.getSP();
		if (tb.match(TokenType.COMMAND)) {
			return parseCommand();
		}
		else if (tb.match(TokenType.IDENTIFIER) || tb.match(TokenType.OPERATOR) || tb.match(TokenType.OOP, J8BKeyword.THIS)) {
			// Делегируем всю работу парсеру выражений
			ExpressionNode expr = new ExpressionNode(tb, mc).parse();
			if(!tb.match(Delimiter.SEMICOLON)) {
				markError("Invalid expression statement - unexpected content after '" + expr + "'");
				tb.skip(Delimiter.SEMICOLON);
			}
			else {
				consumeToken(tb, Delimiter.SEMICOLON);
			}
			return expr;
		}
		else if (tb.match(Delimiter.LEFT_BRACE)) {
			return new BlockNode(tb, mc);
		}
		CompileException e = parserError("Unexpected statement token: " + tb.current());
		tb.skip(Delimiter.SEMICOLON, Delimiter.LEFT_BRACE);
		throw e;
	}

	protected TypeReferenceExpression parseTypeReference() throws CompileException {
		//TODO дублирование кода?
		// Парсим цепочку идентификаторов через точки (для вложенных классов)
		StringBuilder typeName = new StringBuilder();
		Token token = consumeToken(tb, TokenType.IDENTIFIER);
		typeName.append(token.getValue().toString());

		while (tb.match(Delimiter.DOT)) {
			consumeToken(tb); // Пропускаем точку
			token = consumeToken(tb, TokenType.IDENTIFIER);
			typeName.append(".").append(token.getValue().toString());
		}

		return new TypeReferenceExpression(tb, mc, null, typeName.toString());
	}

	protected VarType checkPrimtiveType() throws CompileException {
		if(tb.match(TokenType.TYPE)) {
			Keyword kw = (Keyword)consumeToken(tb).getValue();
			if(J8BKeyword.VOID == kw) return VarType.VOID;
			if(J8BKeyword.BOOL == kw) return VarType.BOOL;
			if(J8BKeyword.BYTE == kw) return VarType.BYTE;
			if(J8BKeyword.SHORT == kw) return VarType.SHORT;
			if(J8BKeyword.INT == kw) return VarType.INT;
			if(J8BKeyword.FIXED == kw) return VarType.FIXED;
			if(J8BKeyword.CSTR == kw) return VarType.CSTR;
			if(J8BKeyword.CLASS == kw) return VarType.CLASS;
			return VarType.UNKNOWN;
		}
		return null;
	}

	protected boolean checkClassName(String className) {
		if (tb.match(TokenType.IDENTIFIER)) {
			String typeName = (String)tb.current().getValue();
			if (typeName.equals(className)) {
				consumeToken(tb);
				return true;
			}
		}
		return false;
	}
	protected VarType checkClassType() throws CompileException {
		if(tb.match(TokenType.IDENTIFIER)) {
			VarType type = VarType.fromClassName((String)tb.current().getValue());
			if(null!=type) {
				consumeToken(tb);
				return checkArrayType(type);
			}
		}
		return null;
	}
	protected VarType checkExceptionType() throws CompileException {
		if(tb.match(TokenType.IDENTIFIER)) {
			if(-1!=VarType.getExceptionId((String)tb.current().getValue())) {
				consumeToken(tb);
				return VarType.EXCEPTION;
			}
		}
		return null;
	}

	//TODO Не корректно, необходимо возвращать Expression.
	protected VarType checkArrayType(VarType type) throws CompileException {
		// Обработка полей-массивов
		if (null != type && tb.match(Delimiter.LEFT_BRACKET)) { //'['
			while (tb.match(Delimiter.LEFT_BRACKET)) { //'['
				consumeToken(tb); // Потребляем '['

				int depth = 0;
				Integer size = null;
				if(tb.match(TokenType.NUMBER)) {
					depth++;
					if (depth > 3) {
						throw new CompileException("Maximum array nesting depth is 3", sp);
					}
					size = (Integer)consumeToken(tb).getValue();
					if (size <= 0) {
						throw new CompileException("Array size must be positive", sp);
					}
					type = VarType.arrayOf(type, size);
				}
				consumeToken(tb, Delimiter.RIGHT_BRACKET);  // Потребляем ']'
				if(null == size) type = VarType.arrayOf(type);
			}
		}
		return type;
	}

	protected List<ExpressionNode> parseArrayDimensions() throws CompileException {
		List<ExpressionNode> result = null;
		
		if (tb.match(Delimiter.LEFT_BRACKET)) { //'['
			result = new ArrayList<>();
			while (tb.match(Delimiter.LEFT_BRACKET)) { //'['
				consumeToken(tb); // Потребляем '['

				if(!tb.match(Delimiter.RIGHT_BRACKET)) { //']'
					if(0x03<result.size()) {
						throw new CompileException("Maximum array nesting depth is 3", sp);
					}
					result.add(new ExpressionNode(tb, mc).parse());
				}
				else {
					result.add(null);
				}
				consumeToken(tb, Delimiter.RIGHT_BRACKET);  // Потребляем ']'
			}
		}
		return result;
	}

/*	protected ExpressionNode parseFullQualifiedExpression(TokenBuffer tb) throws CompileException {
		List<Object> ids = new ArrayList<>();
		while(true) {
			ids.add(consumeToken(tb, TokenType.ID).getStringValue());
			if(tb.match(Delimiter.LEFT_PAREN)) {
				ids.add(parseArguments(tb));
			}
			if(!tb.match(Delimiter.DOT)) {
				break;
			}
			tb.consume();
		}
		
		if(0x01==ids.size() && J8BKeyword.THIS.getName().equals(ids.get(0))) {
			return new ThisExpression(tb, mc);
		}
		if(0x02==ids.size() || 0x03==ids.size() || 0x04==ids.size()) {
			VarType enumVarType = VarType.fromEnumName((String)ids.get(0));
			if(null!=enumVarType) {
				if(ids.get(1) instanceof List) {
					throw new CompileException("TODO invalid statement");
				}
				EnumExpression ee = new EnumExpression(tb, mc, enumVarType, (String)ids.get(1));
				if(0x02==ids.size()) {
					return ee;
				}
				else {
					if(0x04==ids.size() && !(ids.get(3) instanceof List)) {
						throw new CompileException("TODO invalid statement");
					}
					Property prop = null;
					try {prop = Property.valueOf(((String)ids.get(2)).toUpperCase());} catch(Exception ex){}
					return new PropertyExpression(tb, mc, ee, prop, 0x03==ids.size() ? null : (List<ExpressionNode>)ids.get(3));
				}
			}
		}
		
		UnresolvedReferenceExpression ure = null;
		for(int i=0; i<ids.size(); i++) {
			String id = (String)ids.get(i);
			List<ExpressionNode> args = null;
			if(i!=ids.size()-1) {
				if(ids.get(i+1) instanceof List) {
					args = (List<ExpressionNode>)ids.get(++i);
				}
			}
			ure = new UnresolvedReferenceExpression(tb, mc, ure, id,  args);
		}

		if(tb.match(Delimiter.LEFT_BRACKET)) {
			return new ArrayExpression(tb, mc, ure);
		}
		return ure;
	}*/
	
/*	protected ExpressionNode parseFullQualifiedExpression(TokenBuffer tb) throws CompileException {
		QualifiedPathExpression qpe = new QualifiedPathExpression(tb, mc, null);
		QualifiedPathExpression result = qpe;
		
		qpe.add(consumeToken(tb, TokenType.ID).getStringValue());
		
		while(true) {
			if (tb.match(Delimiter.LEFT_PAREN)) {
				qpe.setTarget(new MethodCallExpression(tb, mc, qpe.getBase(), qpe.getLast()));
				qpe = new QualifiedPathExpression(tb, mc, qpe.getTarget());
			} 
			else if(tb.match(Delimiter.LEFT_BRACKET)) {
				qpe.setTarget(new ArrayExpression(tb, mc, qpe));
				qpe = new QualifiedPathExpression(tb, mc, qpe.getTarget());
			}
			else if(tb.match(Delimiter.DOT)) {
				consumeToken(tb, Delimiter.DOT);
				qpe.add(consumeToken(tb, TokenType.ID).getStringValue());
			}
			else {
				break;
			}
		}
		
		return result;
	}*/
	
	protected QualifiedPathExpression parseFullQualifiedExpression(TokenBuffer tb) throws CompileException {
		QualifiedPathExpression path = new QualifiedPathExpression(tb, mc);

		// Первый идентификатор
		if(tb.match(TokenType.OOP, J8BKeyword.THIS)) {
			consumeToken(tb);
			path.addSegment(new QualifiedPathExpression.ThisSegment());
		}
		else {
			String name = consumeToken(tb, TokenType.IDENTIFIER).getStringValue();
			path.addSegment(new QualifiedPathExpression.QualifiedSegment(name));
		}

		// Парсим остальную цепочку
		while(true) {
			if(tb.match(Delimiter.LEFT_PAREN)) {
				path.addSegment(new QualifiedPathExpression.MethodSegment(parseArguments(tb)));
			}
			else if (tb.match(Delimiter.LEFT_BRACKET)) {
				path.addSegment(new QualifiedPathExpression.ArraySegment(parseIndices(tb)));
			}
			else if (tb.match(Delimiter.DOT)) {
				consumeToken(tb, Delimiter.DOT);
				String nextName = consumeToken(tb, TokenType.IDENTIFIER).getStringValue();
				path.addSegment(new QualifiedPathExpression.QualifiedSegment(nextName));
			}
			else {
				break;
			}
		}

		return path;
	}
	public List<ExpressionNode> parseArguments(TokenBuffer tb) throws CompileException {
		List<ExpressionNode> args = new ArrayList<>();
		consumeToken(tb, Delimiter.LEFT_PAREN);

		if (!tb.match(Delimiter.RIGHT_PAREN)) {
			while(true) {
				//Изменяем логику - вместо проверки конкретных токенов 
				//просто парсим выражение целиком
				//Необходимо для поддерки приведения типов
				args.add(new ExpressionNode(tb, mc).parse());

				// Если после выражения нет запятой - выходим из цикла
				if (!tb.match(Delimiter.COMMA)) break;
            
	            // Потребляем запятую
		        AstNode.this.consumeToken(tb, Delimiter.COMMA);
			} 
		}

		AstNode.this.consumeToken(tb, Delimiter.RIGHT_PAREN);
		return args;
	}
	public List<ExpressionNode> parseIndices(TokenBuffer tb) throws CompileException {
		List<ExpressionNode> indices = new ArrayList<>();
		while(true) {
			consumeToken(tb, Delimiter.LEFT_BRACKET);
			indices.add(new ExpressionNode(tb, mc).parse());
			consumeToken(tb, Delimiter.RIGHT_BRACKET);
			if(!tb.match(Delimiter.LEFT_BRACKET)) {
				break;
			}
		}
		return indices;
	}
	
	public final Set<Keyword> collectModifiers(TokenBuffer tb) {
        Set<Keyword> modifiers = new HashSet<>();
        while (tb.match(TokenType.MODIFIER)) {
			Keyword modifier = (Keyword)consumeToken(tb).getValue();
			if(!modifiers.add(modifier)) markError("Duplicate modifier: " + modifier);
        }
        return modifiers;
    }
	
	protected boolean isControlFlowInterrupted(AstNode node) {
		if (null == node) return false;

		// Проверяем явные прерывания потока
		if (node instanceof ReturnNode || node instanceof BreakNode || node instanceof ContinueNode) {
			return true;
		}

		// Для блоков проверяем последнюю инструкцию
		if (node instanceof BlockNode) {
			List<AstNode> nodes = ((BlockNode)node).getChildren();
			if (!nodes.isEmpty()) {
				return isControlFlowInterrupted(nodes.get(nodes.size() - 1));
			}
		}

		// Для условных конструкций
		if (node instanceof IfNode) {
			IfNode ifNode = (IfNode)node;
			return hasAllBranchesReturn(ifNode.getThenBlock(), ifNode.getElseBlock());
		}

		// Для циклов
		if (node instanceof WhileNode || node instanceof DoWhileNode || node instanceof ForNode) {
			// Циклы могут быть бесконечными, поэтому не прерывают поток
			return false;
		}

		// Для switch
		if (node instanceof SwitchNode) {
			SwitchNode switchNode = (SwitchNode)node;
			for (AstCase c : switchNode.getCases()) {
				if (!isControlFlowInterrupted(c.getBlock())) {
					return false;
				}
			}
			return true;
		}

		return false;
	}

	private boolean hasAllBranchesReturn(AstNode thenBranch, AstNode elseBranch) {
		boolean thenReturns = isControlFlowInterrupted(thenBranch);
		boolean elseReturns = elseBranch != null && isControlFlowInterrupted(elseBranch);
		return thenReturns && elseReturns;
	}

	public static boolean canCast(Scope scope, VarType source, VarType target) throws CompileException {
		// Приведение к тому же типу
		if (source == target) return true;
		
		// CSTR не может участвовать в приведении
		if(VarType.CSTR == source || VarType.CSTR == target) return false;
		
		// Разрешаем приведение между числовыми типами
		if (target.isPrimitive() && source.isPrimitive()) return true;
		
		// Object -> что угодно (проверка в runtime)
		if("Object".equals(source.getClassName())) return true;
		
			// Проверка классовых типов
		if (source.isClassType() && target.isClassType()) {
			if(null == source.getClassName()) return false;
			if(source.getClassName().equals(target.getClassName())) return true;
			CIScope cis = scope.getThis().resolveCI(target.getName(), false);
			return null!=cis && cis instanceof InterfaceScope;
		}

		return false;
	}
	
	public static boolean isCompatibleWith(Scope scope, VarType left, VarType right) {
		// Проверка одинаковых типов
		if(left==right) {
			return true;
		}

		//TODO Любой тип данных можно привести к Object?
		if("Object".equals(left.getClassName())) {
			return true;
		}
		
		// Специальные случаи для NULL
		if(VarType.NULL==left || VarType.NULL==right) {
			return left.isReferenceType() || right.isReferenceType();
		}

		//TODO РАЗВЕ? Большинство типов можно объединить со строковой константой
		//if(VarType.CSTR == left && VarType.VOID != right) return true;
		if(VarType.CSTR==left || VarType.CSTR==right) {
			return false;
		}
		
		if(left.isNumeric() && right.isBoolean()) {
			return true;
		}
		
		// Проверка числовых типов
		if(left.isNumeric() && right.isNumeric()) {
			// FIXED совместим только с FIXED
			if(left==VarType.FIXED || right==VarType.FIXED) {
				return left==VarType.FIXED && right==VarType.FIXED;
			}

			// Для арифметических операций разрешаем смешивание любых целочисленных типов
			return left.getSize()>=right.getSize();
		}

		// Проверка классовых типов
		if(left.isClassType() && right.isClassType()) {
			if(null==left.getClassName()) {
				return false;
			}
			if(left.getClassName().equals(right.getClassName())) {
				return true;
			}
			CIScope cis = scope.getThis().resolveCI(right.getName(), false);
			return null!=cis && cis instanceof InterfaceScope;
		}
		
		// Проверка массивов
		if(left.isArray() && right.isArray()) {
			return isCompatibleWith(scope, left.getElementType(), right.getElementType());
		}
    
		return false;
	}
	
	public SourcePosition getSP() {
		return sp;
	}
	
	public Token consumeToken(TokenBuffer tb) {
		return tb.consume();
    }
	public Token consumeToken(TokenBuffer tb, TokenType expectedType) throws CompileException {
		if (tb.current().getType() == expectedType) {
			return consumeToken(tb);
		}
		else {
			throw parserError("Expected " + expectedType + ", but got " + tb.current().getType());
		}
    }
	public Token consumeToken(TokenBuffer tb, Operator op) throws CompileException {
		if (TokenType.OPERATOR == tb.current().getType()) {
            if(op == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected operator " + op + ", but got " + tb.current().getValue());
        }
		else {
			throw parserError("Expected " + TokenType.OPERATOR + ", but got " + tb.current().getType());
		}
    }
	public Token consumeToken(TokenBuffer tb, Delimiter delimiter) throws CompileException {
		if (TokenType.DELIMITER == tb.current().getType()) {
            if(delimiter == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected delimiter " + delimiter + ", but got " + tb.current().getValue());
        }
		else {
			throw parserError("Expected " + TokenType.DELIMITER + ", but got " + tb.current().getType());
		}
    }
	public Token consumeToken(TokenBuffer tb, TokenType type, Keyword keyword) throws CompileException {
		if (type == tb.current().getType()) {
            if(keyword == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected keyword " + keyword + ", but got " + tb.current().getValue());
        }
		else {
			throw parserError("Expected " + TokenType.KEYWORD + ", but got " + tb.current().getType());
		}
    }
	
	public CompileException parserError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		addMessage(message);
		return new CompileException(message);
	}
	public CompileException semanticError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		addMessage(message);
		return new CompileException(text);
	}

	public void addMessage(Message message) {
		mc.add(message);
	}
	public void addMessage(Exception e) {
		mc.add(new ErrorMessage(e.getMessage(), sp));
	}
	
	public void markFirstError(CompileException e) {
		if(null != e && null == error) error = e.getErrorMessage();
	}
	public void markFirstError(ErrorMessage message) {
		if(null != message && null == error) error = message;
	}
	
	
	// должно вызываться из текущей ноды, ее мы помеим как содержащую ошибку.
	public ErrorMessage markError(CompileException e) {
		ErrorMessage message = new ErrorMessage(e.getMessage(), sp);
		if(null == error) error = message;
		addMessage(message);
		return message;
	}
	public ErrorMessage markError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		if(null == error) error = message;
		addMessage(message);
		return message;
	}
	public WarningMessage markWarning(String text) {
		WarningMessage message = new WarningMessage(text, sp);
		addMessage(message);
		return message;
	}
	public WarningMessage markWarning(String text, SourcePosition sp) {
		WarningMessage message = new WarningMessage(text, sp);
		addMessage(message);
		return message;
	}
	
	public void setError(ErrorMessage message) {
		if(null != message && null == error) error = message;
	}
	
	public MessageContainer getMessageContainer() {
		return mc;
	}

	public Symbol getSymbol() {
		return symbol;
	}
	
	public abstract List<AstNode> getChildren();
	
	// Формирует код AST ноды единожды(см. cgDone), загружает результат в аккумулятор, если включен toAccum
	// Код записываем в parent, но если null, то записываем в cg.getScope() он содержит текущий cgScope AST ноды
//	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
//		return codeGen(cg, parent, true, null);
//	}
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException { 
		throw new UnsupportedOperationException(this.toString());
	}
	
	public CGScope depCodeGen(CodeGenerator cg, CGExcs excs) throws CompileException {
		return depCodeGen(cg, symbol, excs);
	}
	protected CGScope depCodeGen(CodeGenerator cg, Symbol symbol, CGExcs excs) throws CompileException {
		if(null != symbol && symbol instanceof AstHolder) {
			AstNode node = ((AstHolder)symbol).getNode();
			if(null != node) {
				// Генерация кода зависимостей не должна влиять на текущий размер аккумулятора
				int accSize = cg.getAccumSize();
				Object obj = node.codeGen(cg, null, false, excs);
				cg.setAccumSize(accSize);
				if(null != obj) return symbol.getCGScope();
			}
		}
		return null;
	}
	
	public CGScope getCGScope() {
		return cgScope;
	}
	
	public boolean isCGDone() {
		return cgDone;
	}
	
	public void disable() {
		disabled = true;
	}
	public boolean isDisabled() {
		return disabled;
	}
	
	public int getSN() {
		return sn;
	}
	
	public static HashMap<AstNode, Scope> getDeclarationPendingNodes() {
		return declarationPendingNodes;
	}
}

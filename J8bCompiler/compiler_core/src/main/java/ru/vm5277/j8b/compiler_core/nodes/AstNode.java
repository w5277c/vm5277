/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.SourcePosition;
import ru.vm5277.j8b.compiler_core.nodes.commands.IfNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.WhileNode;
import ru.vm5277.j8b.compiler_core.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler_core.nodes.expressions.MethodCallExpression;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.BREAK;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.CONTINUE;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.DO;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.FOR;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.IF;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.RETURN;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.SWITCH;
import static ru.vm5277.j8b.compiler_core.enums.Keyword.WHILE;
import ru.vm5277.j8b.compiler.common.enums.Operator;
import ru.vm5277.j8b.compiler_core.enums.TokenType;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.messages.ErrorMessage;
import ru.vm5277.j8b.compiler_core.nodes.commands.BreakNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.ContinueNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.DoWhileNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.ForNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.SwitchNode;
import ru.vm5277.j8b.compiler_core.tokens.Token;
import ru.vm5277.j8b.compiler_core.SemanticAnalyzer;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.common.messages.Message;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler.common.messages.WarningMessage;
import ru.vm5277.j8b.compiler_core.nodes.commands.CommandNode.AstCase;
import ru.vm5277.j8b.compiler_core.nodes.commands.ThrowNode;
import ru.vm5277.j8b.compiler_core.nodes.commands.TryNode;
import ru.vm5277.j8b.compiler_core.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public abstract class AstNode extends SemanticAnalyzer {
	protected			TokenBuffer				tb;
	protected			SourcePosition			sp;
	private				ErrorMessage			error;
	protected	final	ArrayList<BlockNode>	blocks	= new ArrayList<>();
	protected			MessageContainer		mc;
	
	protected AstNode() {
	}
	
	protected AstNode(TokenBuffer tb, MessageContainer mc) {
        this.tb = tb;
		this.sp = null == tb ? null : tb.current().getSP();
		this.mc = mc;
    }

	protected AstNode parseCommand() throws ParseException {
		Keyword keyword = (Keyword)tb.current().getValue();
		switch(keyword) {
			case IF:		return new IfNode(tb, mc);
			case FOR:		return new ForNode(tb, mc);
			case DO:		return new DoWhileNode(tb, mc);
			case WHILE:		return new WhileNode(tb, mc);
			case CONTINUE:	return new ContinueNode(tb, mc);
			case BREAK:		return new BreakNode(tb, mc);
			case RETURN:	return new ReturnNode(tb, mc);
			case SWITCH:	return new SwitchNode(tb, mc);
			case TRY:		return new TryNode(tb, mc);
			case THROW:		return new ThrowNode(tb, mc);
			default:
				markFirstError(error);
				throw new ParseException("Unexpected command token " + tb.current(), sp);
		}
	}

	protected AstNode parseStatement() throws ParseException {
		if (tb.match(TokenType.COMMAND)) {
			return parseCommand();
		}
		else if (tb.match(TokenType.ID)) {
			// Парсим цепочку выражений (включая вызовы методов)
			ExpressionNode expr = parseFullQualifiedExpression(tb);			

			// Если statement заканчивается точкой с запятой (но не в case-выражениях и т.д.)
			if (tb.match(Delimiter.SEMICOLON)) {
				AstNode.this.consumeToken(tb, Delimiter.SEMICOLON);
			}
			else {
				throw new ParseException("Expected ';' after statement", sp);
			}
			return expr;
		}
		else if (tb.match(Operator.INC) || tb.match(Operator.DEC)) {
				ExpressionNode expr = new ExpressionNode(tb, mc).parse();
				consumeToken(tb, Delimiter.SEMICOLON);
				return expr;
		}
		else if (tb.match(Delimiter.LEFT_BRACE)) {
			return new BlockNode(tb, mc);
		}
/*		else if(tb.match(TokenType.OPERATOR)) {
			Operator operator = (Operator)tb.current().getValue();
			if (Operator.INC == operator || Operator.DEC == operator || operator.isAssignment()) {
				ExpressionNode expr = new ExpressionParser(tb).parse();
				tb.consume(Delimiter.SEMICOLON);
				return expr;
			}
			throw new ParseError("Unexpected operator: " + operator, tb.current().getLine(), tb.current().getColumn());
		}*/
		ParseException e = parserError("Unexpected statement token: " + tb.current());
		tb.skip(Delimiter.SEMICOLON, Delimiter.LEFT_BRACE);
		throw e;
	}

	protected ExpressionNode parseTypeReference() throws ParseException {
		//TODO дублирование кода?
		// Парсим цепочку идентификаторов через точки (для вложенных классов)
		StringBuilder typeName = new StringBuilder();
		Token token = consumeToken(tb, TokenType.ID);
		typeName.append(token.getValue().toString());

		while (tb.match(Delimiter.DOT)) {
			consumeToken(tb); // Пропускаем точку
			token = consumeToken(tb, TokenType.ID);
			typeName.append(".").append(token.getValue().toString());
		}

		return new TypeReferenceExpression(tb, mc, typeName.toString());
	}

	protected VarType checkPrimtiveType() throws ParseException {
		if(tb.match(TokenType.TYPE)) {
			Keyword value = (Keyword)consumeToken(tb).getValue();
			VarType type;
			if (null == value) type = VarType.UNKNOWN;
			else {
				switch (value) {
					case VOID: type = VarType.VOID; break;
					case BOOL: type = VarType.BOOL; break;
					case BYTE: type = VarType.BYTE; break;
					case SHORT: type = VarType.SHORT; break;
					case INT: type = VarType.INT; break;
					case FIXED: type = VarType.FIXED; break;
					case CSTR: type = VarType.CSTR; break;
					case CLASS: type = VarType.CLASS; break;
					default: type = VarType.UNKNOWN;
				}
			}
			return checkArrayType(type);
		}
		return null;
	}

	protected boolean checkClassName(String className) {
		if (tb.match(TokenType.ID)) {
			String typeName = (String)tb.current().getValue();
			if (typeName.equals(className)) {
				consumeToken(tb);
				return true;
			}
		}
		return false;
	}
	protected VarType checkClassType() throws ParseException {
		if (tb.match(TokenType.ID)) {
			VarType type = VarType.fromClassName((String)tb.current().getValue());
			if(null != type) {
				consumeToken(tb);
				return checkArrayType(type);
			}
		}
		return null;
	}
	protected VarType checkArrayType(VarType type) throws ParseException {
		// Обработка полей-массивов
		if (null != type && tb.match(Delimiter.LEFT_BRACKET)) { //'['
			while (tb.match(Delimiter.LEFT_BRACKET)) { //'['
				consumeToken(tb); // Потребляем '['

				int depth = 0;
				Integer size = null;
				if(tb.match(TokenType.NUMBER)) {
					depth++;
					if (depth > 3) {
						throw new ParseException("Maximum array nesting depth is 3", sp);
					}
					size = (Integer)consumeToken(tb).getValue();
					if (size <= 0) {
						throw new ParseException("Array size must be positive", sp);
					}
					type = VarType.arrayOf(type, size);
				}
				AstNode.this.consumeToken(tb, Delimiter.RIGHT_BRACKET);  // Потребляем ']'
				type = size != null ? VarType.arrayOf(type, size) : VarType.arrayOf(type);
			}
		}
		return type;
	}

	private ExpressionNode parseFullQualifiedExpression(TokenBuffer tb) throws ParseException {
		ExpressionNode parent = new ExpressionNode(tb, mc).parse();

		// Обрабатываем цепочки вызовов через точку
		while (tb.match(Delimiter.DOT)) {
			consumeToken(tb);
			String methodName = (String)AstNode.this.consumeToken(tb, TokenType.ID).getValue();

			if (tb.match(Delimiter.LEFT_PAREN)) {
				// Это вызов метода
				parent = new MethodCallExpression(tb, mc, parent, methodName, parseArguments(tb));
			}
			else {
				// Доступ к полю (можно добавить FieldAccessNode)
				throw new ParseException("Field access not implemented yet", tb.current().getSP());
			}
		}

		return parent;
	}
	public List<ExpressionNode> parseArguments(TokenBuffer tb) throws ParseException {
		List<ExpressionNode> args = new ArrayList<>();
		consumeToken(tb, Delimiter.LEFT_PAREN);

		if (!tb.match(Delimiter.RIGHT_PAREN)) {
			while(true) {
				// Парсим выражение-аргумент
				args.add(parseFullQualifiedExpression(tb));
				
				// Если после выражения нет запятой - выходим из цикла
				if (!tb.match(Delimiter.COMMA)) break;
            
	            // Потребляем запятую
		        AstNode.this.consumeToken(tb, Delimiter.COMMA);
			} 
		}

		AstNode.this.consumeToken(tb, Delimiter.RIGHT_PAREN);
		return args;
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
			List<AstNode> declarations = ((BlockNode)node).getDeclarations();
			if (!declarations.isEmpty()) {
				return isControlFlowInterrupted(declarations.get(declarations.size() - 1));
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


	public static boolean isCompatibleWith(Scope scope, VarType left, VarType right) {
		// Проверка одинаковых типов
		if (left == right) return true;

		// Специальные случаи для NULL
		if (VarType.NULL == left || VarType.NULL == right) return left.isReferenceType() || right.isReferenceType();

		// Большинство типов можно объединить со строковой константой
		if(VarType.CSTR == left && VarType.VOID != right) return true;

		// Проверка числовых типов
		if (left.isNumeric() && right.isNumeric()) {
			// FIXED совместим только с FIXED
			if (left == VarType.FIXED || right == VarType.FIXED) return left == VarType.FIXED && right == VarType.FIXED;

			// Для арифметических операций разрешаем смешивание любых целочисленных типов
			return true;
		}

		// Проверка классовых типов
		if (left.isClassType() && right.isClassType()) {
			if(null == left.getClassName()) return false;
			if(left.getClassName().equals(right.getClassName())) return true;
			return null != scope.getThis().resolveInterface(left.getName());
		}
		
		// Проверка массивов
		if (left.isArray() && right.isArray()) return isCompatibleWith(scope, left.getElementType(), right.getElementType());
    
		return false;
	}

	
	public List<BlockNode> getBlocks() {
		return blocks;
	}
	
	public SourcePosition getSP() {
		return sp;
	}
	
	public abstract String getNodeType();
	
	public Token consumeToken(TokenBuffer tb) {
		markFirstError(tb.current().getError());
		return tb.consume();
    }
	public Token consumeToken(TokenBuffer tb, TokenType expectedType) throws ParseException {
		if (tb.current().getType() == expectedType) return consumeToken(tb);
		else throw parserError("Expected " + expectedType + ", but got " + tb.current().getType());
    }
	public Token consumeToken(TokenBuffer tb, Operator op) throws ParseException {
		if (TokenType.OPERATOR == tb.current().getType()) {
            if(op == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected operator " + op + ", but got " + tb.current().getValue());
        }
		else throw parserError("Expected " + TokenType.OPERATOR + ", but got " + tb.current().getType());
    }
	public Token consumeToken(TokenBuffer tb, Delimiter delimiter) throws ParseException {
		if (TokenType.DELIMITER == tb.current().getType()) {
            if(delimiter == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected delimiter " + delimiter + ", but got " + tb.current().getValue());
        }
		else throw parserError("Expected " + TokenType.DELIMITER + ", but got " + tb.current().getType());
    }
	public Token consumeToken(TokenBuffer tb, TokenType type, Keyword keyword) throws ParseException {
		if (type == tb.current().getType()) {
            if(keyword == tb.current().getValue()) return consumeToken(tb);
			else throw parserError("Expected keyword " + keyword + ", but got " + tb.current().getValue());
        }
		else throw parserError("Expected " + TokenType.KEYWORD + ", but got " + tb.current().getType());
    }
	
	
	public ParseException parserError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		addMessage(message);
		return new ParseException(message);
	}
	public SemanticException semanticError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		addMessage(message);
		return new SemanticException(text);
	}

	public void addMessage(Message message) {
		mc.add(message);
	}
	public void addMessage(Exception e) {
		mc.add(new ErrorMessage(e.getMessage(), sp));
	}
	
	public void markFirstError(ParseException e) {
		if(null != e && null == error) error = e.getErrorMessage();
	}
	public void markFirstError(ErrorMessage message) {
		if(null != message && null == error) error = message;
	}
	
	
	// должно вызываться из текущей ноды, ее мы помеим как содержащую ошибку.
	public ErrorMessage markError(SemanticException e) {
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
	
	public void codeGen(CodeGenerator cg) {
		throw new UnsupportedOperationException(this.toString());
	}
}

/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.nodes.commands.IfNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.commands.WhileNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import static ru.vm5277.j8b.compiler.enums.Keyword.BREAK;
import static ru.vm5277.j8b.compiler.enums.Keyword.CONTINUE;
import static ru.vm5277.j8b.compiler.enums.Keyword.DO;
import static ru.vm5277.j8b.compiler.enums.Keyword.FOR;
import static ru.vm5277.j8b.compiler.enums.Keyword.GOTO;
import static ru.vm5277.j8b.compiler.enums.Keyword.IF;
import static ru.vm5277.j8b.compiler.enums.Keyword.RETURN;
import static ru.vm5277.j8b.compiler.enums.Keyword.SWITCH;
import static ru.vm5277.j8b.compiler.enums.Keyword.WHILE;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.commands.BreakNode;
import ru.vm5277.j8b.compiler.nodes.commands.ContinueNode;
import ru.vm5277.j8b.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.j8b.compiler.nodes.commands.ForNode;
import ru.vm5277.j8b.compiler.nodes.commands.GotoNode;
import ru.vm5277.j8b.compiler.nodes.commands.SwitchNode;

public abstract class AstNode {
	protected			TokenBuffer				tb;
	protected			SourceBuffer			sb;
	protected	final	ArrayList<BlockNode>	blocks	= new ArrayList<>();
	
	protected AstNode() {
	}
	
	protected AstNode(TokenBuffer tb) {
        this.tb = tb;
		this.sb = tb.current().getSB().clone();
    }

	protected AstNode parseCommand() {
		Keyword keyword = (Keyword)tb.current().getValue();
		switch(keyword) {
			case IF:		return new IfNode(tb);
			case FOR:		return new ForNode(tb);
			case DO:		return new DoWhileNode(tb);
			case WHILE:		return new WhileNode(tb);
			case CONTINUE:	return new ContinueNode(tb);
			case BREAK:		return new BreakNode(tb);
			case RETURN:	return new ReturnNode(tb);
			case GOTO:		return new GotoNode(tb);
			case SWITCH:	return new SwitchNode(tb);
			default:
				throw new ParseError("Unexpected command token " + tb.current(), tb.getSB());
		}
	}

	protected AstNode parseStatement() {
		if (tb.match(TokenType.COMMAND)) {
			return parseCommand();
		}
		else if (tb.match(TokenType.ID)) {
			// Парсим цепочку выражений (включая вызовы методов)
			ExpressionNode expr = parseFullQualifiedExpression(tb);			

			// Если statement заканчивается точкой с запятой (но не в case-выражениях и т.д.)
			if (tb.match(Delimiter.SEMICOLON)) {
				tb.consume(Delimiter.SEMICOLON);
			}
			else {
				throw new ParseError("Expected ';' after statement", tb.getSB());
			}
			return expr;
		}
		else if (tb.match(Operator.INC) || tb.match(Operator.DEC)) {
				ExpressionNode expr = new ExpressionParser(tb).parse();
				tb.consume(Delimiter.SEMICOLON);
				return expr;
		}
		else if (tb.match(Delimiter.LEFT_BRACE)) {
			return new BlockNode(tb);
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
		throw new ParseError("Unexpected statement token: " + tb.current(), tb.getSB());
	}

	protected VarType checkPrimtiveType() {
		if(tb.match(TokenType.TYPE)) {
			VarType type = VarType.fromKeyword((Keyword)tb.consume().getValue());
			if(null != type) return checkArrayType(type);
		}
		return null;
	}
	protected boolean checkClassName(String className) {
		if (tb.match(TokenType.ID)) {
			String typeName = (String)tb.current().getValue();
			if (typeName.equals(className)) {
				tb.consume();
				return true;
			}
		}
		return false;
	}
	protected VarType checkClassType() {
		if (tb.match(TokenType.ID)) {
			VarType type = VarType.fromClassName((String)tb.current().getValue());
			if(null != type) {
				tb.consume();
				return checkArrayType(type);
			}
		}
		return null;
	}
	protected VarType checkArrayType(VarType type) {
		// Обработка полей-массивов
		if (null != type && tb.match(Delimiter.LEFT_BRACKET)) { //'['
			while (tb.match(Delimiter.LEFT_BRACKET)) { //'['
				tb.consume(); // Пропускаем '['

				int depth = 0;
				Integer size = null;
				if(tb.match(TokenType.NUMBER)) {
					depth++;
					if (depth > 3) {
						throw new ParseError("Maximum array nesting depth is 3", tb.getSB());
					}
					size = (Integer)tb.consume().getValue();
					if (size <= 0) {
						throw new ParseError("Array size must be positive", tb.getSB());
					}
					type = VarType.arrayOf(type, size);
				}
				tb.consume(Delimiter.RIGHT_BRACKET);  // Пропускаем ']'
				type = size != null ? VarType.arrayOf(type, size) : VarType.arrayOf(type);
			}
		}
		return type;
	}

	private static ExpressionNode parseFullQualifiedExpression(TokenBuffer tb) {
		ExpressionNode parent = new ExpressionParser(tb).parse();

		// Обрабатываем цепочки вызовов через точку
		while (tb.match(Delimiter.DOT)) {
			tb.consume();
			String methodName = (String)tb.consume(TokenType.ID).getValue();

			if (tb.match(Delimiter.LEFT_PAREN)) {
				// Это вызов метода
				parent = new MethodCallExpression(tb, parent, methodName, parseArguments(tb));
			}
			else {
				// Доступ к полю (можно добавить FieldAccessNode)
				throw new ParseError("Field access not implemented yet", tb.getSB());
			}
		}

		return parent;
	}
	public static List<ExpressionNode> parseArguments(TokenBuffer tb) {
		List<ExpressionNode> args = new ArrayList<>();
		tb.consume(Delimiter.LEFT_PAREN);

		if (!tb.match(Delimiter.RIGHT_PAREN)) {
			while(true) {
				// Парсим выражение-аргумент
				args.add(parseFullQualifiedExpression(tb));
				
				// Если после выражения нет запятой - выходим из цикла
				if (!tb.match(Delimiter.COMMA)) break;
            
	            // Пропускаем запятую
		        tb.consume(Delimiter.COMMA);
			} 
		}

		tb.consume(Delimiter.RIGHT_PAREN);
		return args;
	}
	
	public static final Set<Keyword> collectModifiers(TokenBuffer tb) {
        Set<Keyword> modifiers = new HashSet<>();
        while (tb.current().getType() == TokenType.MODIFIER) {
			modifiers.add((Keyword)tb.consume().getValue());
        }
        return modifiers;
    }
	
	public List<BlockNode> getBlocks() {
		return blocks;
	}
	
	public SourceBuffer getSB() {
		return sb;
	}
}

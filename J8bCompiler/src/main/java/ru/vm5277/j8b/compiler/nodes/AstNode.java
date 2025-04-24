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
import ru.vm5277.j8b.compiler.nodes.commands.IfNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.commands.WhileNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public abstract class AstNode {
	protected			TokenBuffer				tb;
	protected			int						line;
	protected			int						column;
	protected	final	ArrayList<BlockNode>	blocks	= new ArrayList<>();
	
	protected AstNode(TokenBuffer tb) {
        this.tb = tb;
		this.line = tb.current().getLine();
		this.column = tb.current().getColumn();
    }

	protected AstNode parseStatement() {
		if (tb.match(TokenType.COMMAND)) {
			Keyword keyword = (Keyword)tb.current().getValue();

			switch(keyword) {
				case IF: return new IfNode(tb);
				case WHILE:	return new WhileNode(tb);
				case RETURN: return new ReturnNode(tb);
				default:
					throw new ParseError("Unexpected command token " + tb.current(), tb.current().getLine(), tb.current().getColumn());
			}
		}
		else if (tb.match(TokenType.ID)) {
			// Парсим цепочку выражений (включая вызовы методов)
			ExpressionNode expr = parseFullQualifiedExpression();			

			// Если statement заканчивается точкой с запятой (но не в case-выражениях и т.д.)
			if (tb.match(Delimiter.SEMICOLON)) {
				tb.consume(Delimiter.SEMICOLON);
			}
			else {
				throw new ParseError("Expected ';' after statement", tb.current().getLine(), tb.current().getColumn());
			}
			return expr;
		}
		else if (tb.match(Delimiter.LEFT_BRACE)) {
			
			return new BlockNode(tb, "");
		}
		throw new ParseError("Unexpected statement token: " + tb.current(),	tb.current().getLine(),	tb.current().getColumn());
	}

	private ExpressionNode parseFullQualifiedExpression() {
		ExpressionNode base = new ExpressionParser(tb).parse();

		// Обрабатываем цепочки вызовов через точку
		while (tb.match(Delimiter.DOT)) {
			tb.consume();
			String methodName = (String)tb.consume(TokenType.ID).getValue();

			if (tb.match(Delimiter.LEFT_PAREN)) {
				// Это вызов метода
				base = new MethodCallExpression(tb, base, methodName, parseArguments());
			}
			else {
				// Доступ к полю (можно добавить FieldAccessNode)
				throw new ParseError("Field access not implemented yet", tb.current().getLine(), tb.current().getColumn());
			}
		}

		return base;
	}
	private List<ExpressionNode> parseArguments() {
		List<ExpressionNode> args = new ArrayList<>();
		tb.consume(Delimiter.LEFT_PAREN);

		if (!tb.match(Delimiter.RIGHT_PAREN)) {
			while(true) {
				// Парсим выражение-аргумент
				args.add(parseFullQualifiedExpression());
				
				// Если после выражения нет запятой - выходим из цикла
				if (!tb.match(Delimiter.COMMA)) break;
            
	            // Пропускаем запятую
		        tb.consume(Delimiter.COMMA);
			} 
		}

		tb.consume(Delimiter.RIGHT_PAREN);
		return args;
	}
	
	protected final Set<Keyword> collectModifiers() {
        Set<Keyword> modifiers = new HashSet<>();
        while (tb.current().getType() == TokenType.MODIFIER) {
			modifiers.add((Keyword)tb.current().getValue());
            tb.consume();
        }
        return modifiers;
    }
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
	
	public List<BlockNode> getBlocks() {
		return blocks;
	}
}

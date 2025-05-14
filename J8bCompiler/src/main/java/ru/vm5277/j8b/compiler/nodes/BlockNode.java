/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.j8b.compiler.nodes.commands.ForNode;
import ru.vm5277.j8b.compiler.nodes.commands.IfNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.commands.SwitchNode;
import ru.vm5277.j8b.compiler.nodes.commands.SwitchNode.Case;
import ru.vm5277.j8b.compiler.nodes.commands.WhileNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.j8b.compiler.semantic.BlockScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class BlockNode extends AstNode {
	private	List<AstNode>			declarations	= new ArrayList<>();
	private	Map<String, LabelNode>	labels			= new HashMap<>();
	private	BlockScope				blockScope;
	
	public BlockNode() {
	}
	
	public BlockNode(TokenBuffer tb, MessageContainer mc, AstNode singleStatement) {
		super(tb, mc);
		
		declarations.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb, MessageContainer mc) throws ParseException {
        super(tb, mc);
        
		consumeToken(tb, Delimiter.LEFT_BRACE); //Наличие токена должно быть гарантировано вызывающим

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			if(tb.match(TokenType.LABEL)) {
				LabelNode label = new LabelNode(tb, mc);
				labels.put(label.getName(), label);
				declarations.add(label);
			}
			if (tb.match(TokenType.COMMAND)) {
				try {
					declarations.add(parseCommand());
				}
				catch(ParseException e) {
					addMessage(e.getErrorMessage());
					markFirstError(e);
				} // Фиксируем ошибку(Unexpected command token)
				continue;
			}
			if(tb.match(Keyword.FREE)) {
				declarations.add(new FreeNode(tb, mc));
				continue;
			}
			
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
				declarations.add(new ClassNode(tb, mc, modifiers, null, null));
				continue;
			}
			// Обработка интерфейсов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
				declarations.add(new InterfaceNode(tb, mc, modifiers, null));
				continue;
			}

			try {
				// Определение типа (примитив или класс)
				VarType type = checkPrimtiveType();
				if (null == type) type = checkClassType();


				if(null != type) {
					// Получаем имя метода/конструктора
					String name = null;
					try {name = consumeToken(tb, TokenType.ID).getStringValue();}catch(ParseException e) {markFirstError(e);} // Нет имени сущности, пытаемся парсить дальше

					if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
						ArrayDeclarationNode node = new ArrayDeclarationNode(tb, mc, modifiers, type, name);
						if(null != name) declarations.add(node);
					}
					else { // Переменная
						VarNode varNode = new VarNode(tb, mc, modifiers, type, name);
						if(null != name) declarations.add(varNode);
					}
					continue;
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}

			// Обработка остальных statement (if, while, вызовы и т.д.)
			try {
				AstNode statement = parseStatement();
				declarations.add(statement);
				if(statement instanceof BlockNode) {
					blocks.add((BlockNode)statement);
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}
		}
		consumeToken(tb, Delimiter.RIGHT_BRACE);
    }

	public BlockNode(MessageContainer mc) throws ParseException {
        super(null, mc);
	}
	
	public static  boolean hasReturnStatement(AstNode node) {
		if (null == node) return false;

		// Если это return-выражение
		if (node instanceof ReturnNode) return true;

		// Если это блок, проверяем все его декларации
		if (node instanceof BlockNode) {
			BlockNode block = (BlockNode)node;
			for (AstNode declaration : block.getDeclarations()) {
				if (hasReturnStatement(declaration)) {
					return true;
				}
			}
		}
		// Для других узлов проверяем их дочерние блоки
		else {
			for (BlockNode block : node.getBlocks()) {
				if (hasReturnStatement(block)) {
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
			for (Case c : switchNode.getCases()) {
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
	
	public List<AstNode> getDeclarations() {
		return declarations;
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
		for (AstNode declaration : declarations) {
			declaration.preAnalyze(); // Не реагируем на критические ошибки
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Создаем новую область видимости для блока
		blockScope = new BlockScope(scope);

		// Объявляем все элементы в блоке
		for (AstNode declaration : declarations) {
			declaration.declare(blockScope);
		}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		for (int i = 0; i < declarations.size(); i++) {
			AstNode declaration = declarations.get(i);

			// Анализируем текущую ноду
			declaration.postAnalyze(blockScope);

			// Проверяем недостижимый код после прерывающих инструкций
			if (i > 0 && isControlFlowInterrupted(declarations.get(i - 1))) {
				markError("Unreachable code after " + declarations.get(i - 1).getClass().getSimpleName());
				// Можно пропустить анализ остального кода, так как он недостижим
				break;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
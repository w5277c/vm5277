/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class ClassBlockNode extends AstNode {
	protected	List<AstNode>			declarations	= new ArrayList<>();

	public ClassBlockNode(TokenBuffer tb, MessageContainer mc, String className) throws ParseException {
		super(tb, mc);
        
		consumeToken(tb, Delimiter.LEFT_BRACE); // в случае ошибки, останавливаем парсинг файла

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {		
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка классов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.CLASS)) {
				declarations.add(new ClassNode(tb, mc, modifiers, null, null));
				continue;
			}
			// Обработка интерфейсов с модификаторами
			if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
				declarations.add(new InterfaceNode(tb, mc, modifiers, null));
				continue;
			}

			// Определение типа (примитив, класс или конструктор)
			VarType type = checkPrimtiveType();
			boolean isClassName = false;
			if (null == type) {
				isClassName = checkClassName(className);
				if(!isClassName) type = checkClassType();
			}
			
			// Получаем имя метода/конструктора
			String name = null;
			if(tb.match(TokenType.ID)) {
				if(isClassName) {
					isClassName = false;
					type = VarType.fromClassName(className);
				}
				name = consumeToken(tb).getStringValue();
			}

			if(tb.match(Delimiter.LEFT_PAREN)) { //'(' Это метод
				if(isClassName) {
					declarations.add(new MethodNode(tb, mc, modifiers, null, className));
				}
				else {
					declarations.add(new MethodNode(tb, mc, modifiers, type, name));
				}
				continue;
			}

			if(null != type) {
				if (tb.match(Delimiter.LEFT_BRACKET)) { // Это объявление массива
					declarations.add(new ArrayDeclarationNode(tb, mc, modifiers, type, name));
				}
				else { // Поле
					declarations.add(new FieldNode(tb, mc, modifiers, type, name));
				}
			}

			if(tb.match(Delimiter.LEFT_BRACE)) {
				tb.getLoopStack().add(this);
				try {
					BlockNode blockNode = new BlockNode(tb, mc);
					declarations.add(blockNode);
					blocks.add(blockNode);
				}
				catch(ParseException e) {}
				tb.getLoopStack().remove(this);
			}
		}
		
		//Попытка потребить '}'
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
    }

	public ClassBlockNode(MessageContainer mc) throws ParseException {
		super(null, mc);
	}
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
	
	@Override
	public String getNodeType() {
		return "class body";
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
		for (AstNode declaration : declarations) {
			declaration.declare(scope);
		}
		return true;
	}

	
	@Override
	public boolean postAnalyze(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		// Проверка наличия конструктора
		if (checkNonStaticMembers(classScope)) {
			List<MethodSymbol> constructors = classScope.getConstructors();
			if(null == constructors || constructors.isEmpty()) {
				markError("Class must have at least one constructor");
			}
		}
		
		for (AstNode node : declarations) {
			node.postAnalyze(scope);
		}
		return true;
	}
	
	private boolean checkNonStaticMembers(ClassScope classScope) {
		// Проверка полей
		for (Symbol field : classScope.getFields().values()) {
			if (!field.isStatic()) return true;
		}

		// Проверка методов
		for (List<MethodSymbol> methodGroup : classScope.getMethods().values()) {
			for (MethodSymbol method : methodGroup) {
				if (!method.isStatic()) return true;
			}
		}

		return false;
	}
}
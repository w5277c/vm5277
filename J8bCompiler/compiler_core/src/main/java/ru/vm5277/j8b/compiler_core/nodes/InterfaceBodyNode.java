/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import ru.vm5277.j8b.compiler_core.enums.TokenType;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler.common.messages.WarningMessage;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class InterfaceBodyNode extends AstNode {
	protected List<AstNode> declarations = new ArrayList<>();
	
	public InterfaceBodyNode(TokenBuffer tb, MessageContainer mc, String className) throws ParseException {
		super(tb, mc);

		consumeToken(tb, Delimiter.LEFT_BRACE);

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка вложенных интерфейсов
			if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
				declarations.add(new InterfaceNode(tb, mc, modifiers, null));
				continue;
			}

			// Обработка вложенных классов
			if (tb.match(TokenType.OOP, Keyword.CLASS)) {
				declarations.add(new ClassNode(tb, mc, modifiers, null, null));
				continue;
			}

			// Определение типа (примитив или класс)
			VarType type = checkPrimtiveType();
			if (null == type) type = checkClassType();

			// Получаем имя поля/метода
			String name = null;
			if (tb.match(TokenType.ID)) {
				name = consumeToken(tb).getStringValue();
			}

			if (tb.match(Delimiter.LEFT_PAREN)) { // Это метод
				declarations.add(new MethodNode(tb, mc, modifiers, type, name));
				continue;
			}

			if (null != type) { // Это поле
				declarations.add(new FieldNode(tb, mc, modifiers, type, name));
				continue;
			}

			// Если ничего не распознано, пропускаем токен
			markError("Unexpected token in interface body: " + tb.current());
			tb.consume();
		}

		try {
			consumeToken(tb, Delimiter.RIGHT_BRACE);
		} catch (ParseException e) {
			markFirstError(e);
		}
	}
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
	
	@Override
	public String getNodeType() {
		return "interface body";
	}
	
	@Override
	public boolean preAnalyze() {
		// Проверка всех объявлений в блоке
		for (AstNode declaration : declarations) {
			if(declaration instanceof InterfaceNode) {
				declaration.preAnalyze();
			}
			else if(declaration instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)declaration;
				if(!fieldNode.getModifiers().isEmpty()) {
					//TODO не сохраняется позиция для конкретного modifier
					addMessage(new WarningMessage("Modifiers not allowed for interface fields (already public static final)", fieldNode.getSP()));
				}
				declaration.preAnalyze();
			}
			else if(declaration instanceof MethodNode) {
				MethodNode methoddNode = (MethodNode)declaration;
				if(!methoddNode.getModifiers().isEmpty()) {
					//TODO не сохраняется позиция для конкретного modifier
					addMessage(new WarningMessage("Modifiers not allowed for interface methods (already public abstract)", methoddNode.getSP()));
				}
				declaration.preAnalyze();
			}
			else {
				markError("Interface cannot contain " + declaration.getNodeType() + " declarations. Only methods, constants and nested types are allowed");
			}
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

		for (AstNode declaration : declarations) {
			if (declaration instanceof InterfaceNode || declaration instanceof ClassNode) {
				// Обработка вложенных интерфейсов и классов
				declaration.postAnalyze(scope);
			} 
			else if (declaration instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)declaration;

				// Проверка инициализации поля
				if (null == fieldNode.getInitializer()) {
					markError("Interface field '" + fieldNode.getName() + "' must be initialized");
				} else {
					fieldNode.getInitializer().postAnalyze(scope);
				}

				// Проверка что поле static final
				if (!fieldNode.isStatic()) {
					markError("Interface field '" + fieldNode.getName() + "' must be static");
				}
				if (!fieldNode.isFinal()) {
					markError("Interface field '" + fieldNode.getName() + "' must be final");
				}
			} 
			else if (declaration instanceof MethodNode) {
				MethodNode methodNode = (MethodNode)declaration;

				// Запрет конструкторов
				if (methodNode.isConstructor()) {
					markError("Interfaces cannot have constructors");
					continue;
				}

				// Проверка методов
				if (methodNode.isStatic()) {
					// Для статических методов должно быть тело
					if (null == methodNode.getBody()) {
						markError("Static method '" + methodNode.getName() + "' must have a body");
					} else {
						methodNode.getBody().postAnalyze(scope);
					}
				} else {
					// Для нестатических методов не должно быть тела (абстрактные)
					if (null != methodNode.getBody()) {
						markError("Non-static interface method '" + methodNode.getName() + "' cannot have a body");
					}
				}

				// Проверка что метод public
				if (!methodNode.isPublic()) {
					markError("Interface method '" + methodNode.getName() + "' must be public");
				}
			}
		}

		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		for(AstNode decl : declarations) {
			if(decl instanceof MethodNode && ((MethodNode)decl).isStatic()) {
				decl.codeGen(cg);
			}
			else if(decl instanceof FieldNode && ((FieldNode)decl).isStatic()) {
				decl.codeGen(cg);
			}
		}
	}
}
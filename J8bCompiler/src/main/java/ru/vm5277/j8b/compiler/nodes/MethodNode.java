/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.ErrorMessage;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.MethodScope;
import ru.vm5277.j8b.compiler.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	private			MethodScope			methodScope; // Добавляем поле для хранения области видимости
	
	public MethodNode(TokenBuffer tb, Set<Keyword> modifiers, VarType returnType, String name) throws ParseException {
		super(tb);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;

        consumeToken(tb); // Потребляем '('
		this.parameters = parseParameters();
        consumeToken(tb); // Потребляем ')'
		
		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {blocks.add(new BlockNode(tb));}catch(ParseException e) {}
			tb.getLoopStack().remove(this);
		}
		else {
			markFirstError(tb.parseError("Method '" + name + "' must contain a body"));
		}
	}
	
	private List<ParameterNode> parseParameters() {
        List<ParameterNode> params = new ArrayList<>();
        
		while(!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_PAREN)) {
			try {
				params.add(new ParameterNode(tb));
				if (tb.match(Delimiter.COMMA)) {
					consumeToken(tb); // Потребляем ','
					continue;
				}
				else if(tb.match(TokenType.EOF) || tb.match(Delimiter.RIGHT_PAREN)) {
					break;
				}
				ErrorMessage message = new ErrorMessage("Expected " + Delimiter.RIGHT_PAREN + ", but got " + tb.current().getType(), tb.current().getSP());
				tb.addMessage(message);
				markFirstError(message);
				break;
			}
			catch(ParseException e) {
				markFirstError(e);
				tb.skip(Delimiter.RIGHT_PAREN);
				break;
			}
		}

		return params;
    }
	
	public BlockNode getBody() {
		return blocks.isEmpty() ? null : blocks.get(0);
	}
	
	public boolean isConstructor() {
		return null == returnType;
	}

	public List<ParameterNode> getParameters() {
		return parameters;
	}
	
	public VarType getReturnType() {
		return returnType;
	}

	public String getName() {
		return name;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "method";
	}

	@Override
	public boolean preAnalyze() {
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC, Keyword.NATIVE);} catch(SemanticException e) {tb.addMessage(e);}
		
		if(!isConstructor() && Character.isUpperCase(name.charAt(0))) {
			tb.addMessage(new WarningMessage("Method name should start with lowercase letter:" + name, tb.getSP()));
		}

		for (ParameterNode parameter : parameters) {
			parameter.preAnalyze();
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		List<Symbol> paramSymbols = new ArrayList<>();
		for (ParameterNode param : parameters) {
			paramSymbols.add(new Symbol(param.getName(), param.getType(), param.isFinal()));
		}

		// Создаем MethodSymbol
		try {
			methodScope = new MethodScope(null, classScope);
			MethodSymbol methodSymbol = new MethodSymbol(name, returnType, paramSymbols, modifiers.contains(Keyword.FINAL), methodScope);
			// Устанавливаем обратную ссылку
			methodScope.setSymbol(methodSymbol);

			// Добавляем метод или конструктор в область видимости класса
			if (isConstructor()) {
				classScope.addConstructor(methodSymbol);
			}
			else {
				// Для обычных методов
				classScope.addMethod(methodSymbol);
			}

			// Объявляем параметры в области видимости метода
			for (ParameterNode param : parameters) {
				param.declare(methodScope);
			}
		}
		catch(SemanticException e) {markError(e);}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		if (modifiers.contains(Keyword.NATIVE)) {
			if (getBody() != null) {
				markError("Native method cannot have a body");
			}
			return true;
		}
		
		// Анализ тела метода (если есть)
		if (null != getBody() && null != methodScope) {
			for (ParameterNode param : parameters) {
				param.postAnalyze(methodScope);
			}

			// Анализируем тело метода
			getBody().postAnalyze(methodScope);

			// Для не-void методов проверяем наличие return
			// TODO переосмыслить после ConstantFolding
			if (null != returnType && !returnType.equals(VarType.VOID)) {
				if (!BlockNode.hasReturnStatement(getBody())) {
					markError("Method '" + name + "' must return a value");
				}
			}
			
			List<AstNode> declarations = getBody().getDeclarations();
			for (int i = 0; i < declarations.size(); i++) {
				if (i > 0 && isControlFlowInterrupted(declarations.get(i - 1))) {
					markError("Unreachable code in method " + name);
					break;
				}
			}
		}
		return true;
	}
}

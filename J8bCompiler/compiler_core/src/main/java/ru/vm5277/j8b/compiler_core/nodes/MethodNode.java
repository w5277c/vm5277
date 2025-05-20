/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
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
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.common.messages.ErrorMessage;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler.common.messages.WarningMessage;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.MethodScope;
import ru.vm5277.j8b.compiler_core.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.semantic.Symbol;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	private			MethodScope			methodScope; // Добавляем поле для хранения области видимости
	private			boolean				canThrow	= false;
	
	public MethodNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name) throws ParseException {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;

        consumeToken(tb); // Потребляем '('
		this.parameters = parseParameters(mc);
        consumeToken(tb); // Потребляем ')'
		
		// Проверяем наличие throws
		if (tb.match(TokenType.OOP, Keyword.THROWS)) {
			consumeToken(tb);
			this.canThrow = true;
		}

		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {blocks.add(new BlockNode(tb, mc));}catch(ParseException e) {}
			tb.getLoopStack().remove(this);
		}
		else {
			try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
		}
	}
	
	public MethodNode(	MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name, List<ParameterNode> parameters, boolean canThrow,
						BlockNode body) {
		super(null, mc);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		this.parameters = parameters;
		this.canThrow = canThrow;
		
		blocks.add(body);
	}
	
	public boolean canThrow() {
		return canThrow;
	}
	
	private List<ParameterNode> parseParameters(MessageContainer mc) {
        List<ParameterNode> params = new ArrayList<>();
        
		while(!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_PAREN)) {
			try {
				params.add(new ParameterNode(tb, mc));
				if (tb.match(Delimiter.COMMA)) {
					consumeToken(tb); // Потребляем ','
					continue;
				}
				else if(tb.match(TokenType.EOF) || tb.match(Delimiter.RIGHT_PAREN)) {
					break;
				}
				ErrorMessage message = new ErrorMessage("Expected " + Delimiter.RIGHT_PAREN + ", but got " + tb.current().getType(), tb.current().getSP());
				addMessage(message);
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

	public boolean isStatic() {
		return modifiers.contains(Keyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(Keyword.PUBLIC);
	}
	
	@Override
	public boolean preAnalyze() {
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC, Keyword.NATIVE);} catch(SemanticException e) {addMessage(e);}
		
		if(!isConstructor() && Character.isUpperCase(name.charAt(0))) {
			addMessage(new WarningMessage("Method name should start with uppercase letter:" + name, sp));
		}

		for (ParameterNode parameter : parameters) {
			parameter.preAnalyze();
		}
		
		if(null != getBody()) {
			getBody().preAnalyze();
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		List<Symbol> paramSymbols = new ArrayList<>();
		for (ParameterNode param : parameters) {
			paramSymbols.add(new Symbol(param.getName(), param.getType(), param.isFinal(), false));
		}

		// Создаем MethodSymbol
		try {
			methodScope = new MethodScope(null, classScope);
			MethodSymbol methodSymbol = new MethodSymbol(	name, returnType, paramSymbols, modifiers.contains(Keyword.FINAL),
															modifiers.contains(Keyword.STATIC),	canThrow, methodScope);
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
		
		if(null != getBody()) {
			getBody().declare(methodScope);
		}
		
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
	
	@Override
	public void codeGen(CodeGenerator cg) {
		int[] typeIds = null;
		if(!parameters.isEmpty()) {
			typeIds = new int[parameters.size()];
			for(int i=0; i<parameters.size(); i++) {
				typeIds[i] = parameters.get(i).getType().getId();
			}
		}
		
		if(null == returnType) {
			cg.enterConstructor(typeIds);
		}
		else {
			cg.enterMethod(returnType.getId(), typeIds);
		}
		
		try {
			BlockNode body = blocks.get(0);
			if(null != body) body.codeGen(cg);
		}
		finally {
			cg.leave();
		}
	}
}

/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.InterfaceSymbol;
import ru.vm5277.j8b.compiler.semantic.MethodSymbol;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class ClassNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			String			name;
	private			String			parentClassName;
	private			List<String>	interfaces	= new ArrayList<>();
	private			ClassBlockNode	blockNode;
	
	public ClassNode(TokenBuffer tb, Set<Keyword> modifiers, String parentClassName) throws ParseException {
		super(tb);
		
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name);
		}
		catch(ParseException e) {markFirstError(e);} // ошибка в имени, оставляем null
		
        // Парсинг интерфейсов (если есть)
		if (tb.match(Keyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					interfaces.add((String)consumeToken(tb, TokenType.ID).getValue());
				}
				catch(ParseException e) {markFirstError(e);} // встретили не ID интерфейса, пропускаем
				if (!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}
        // Парсинг тела класса
		blockNode = new ClassBlockNode(tb, name);
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}
	
	public ClassBlockNode getBody() {
		return blockNode;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "class";
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + interfaces;
	}

	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(SemanticException e) {tb.addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			tb.addMessage(new WarningMessage("Class name should start with uppercase letter:" + name, tb.getSP()));
		}
		
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC);} catch(SemanticException e) {tb.addMessage(e);}
		
		// Анализ тела класса
		for (BlockNode block : blocks) {
			block.preAnalyze();
		}
		return true;
	}
	
	@Override
	public boolean declare(Scope parentScope) {
		try {
			ClassScope classScope = new ClassScope(name, parentScope);
			if(null != parentScope) ((ClassScope)parentScope).addClass(classScope);
			
			getBody().declare(classScope);
		}
		catch(SemanticException e) {markError(e); return false;}

		return true;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope) {
		ClassScope classScope = (ClassScope)scope;
		
		for (String interfaceName : interfaces) {
			// Проверяем существование интерфейса
			InterfaceSymbol interfaceSymbol = classScope.getInterface(interfaceName);
			if (null == interfaceSymbol) markError("Interface not found: " + interfaceName);

			checkInterfaceImplementation(classScope, interfaceSymbol);
		}
		
		for (BlockNode block : blocks) {
			block.postAnalyze(classScope);
		}
		return true;
	}
	
	
	private boolean checkInterfaceImplementation(ClassScope classScope, InterfaceSymbol interfaceSymbol) {
		boolean allMethodsImplemented = true;
		
		Map<String, List<MethodSymbol>> map = interfaceSymbol.getMethods();
		for(String methodName : map.keySet()) { 
			List<MethodSymbol> entry = map.get(methodName);
			boolean found = false;

			// Получаем методы класса с таким же именем
			List<MethodSymbol> classMethods = classScope.getMethods(methodName);

			// Для каждого метода в интерфейсе
			for (MethodSymbol interfaceMethod : entry) {

				// Проверяем каждый метод класса
				for (MethodSymbol classMethod : classMethods) {
					if (interfaceMethod.getSignature().equals(classMethod.getSignature())) {
						found = true;
						break;
					}
				}

				if (!found) {
					markError(	"Class '" + classScope.getName() + "' must implement method: " + interfaceMethod.getSignature() + 
								" from interface '" + interfaceSymbol.getName() + "'");
					allMethodsImplemented = false;
				}
			}
		}
		return allMethodsImplemented;
	}
}

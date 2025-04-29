/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.BlockNode;
import ru.vm5277.j8b.compiler.nodes.ClassNode;
import ru.vm5277.j8b.compiler.nodes.FieldNode;
import ru.vm5277.j8b.compiler.nodes.MethodNode;
import ru.vm5277.j8b.compiler.nodes.ParameterNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.j8b.compiler.semantic.ClassInfo;
import ru.vm5277.j8b.compiler.semantic.MethodInfo;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class SemanticAnalyzer {
	private final	SymbolTable	symbolTable			= new SymbolTable();
	
	public SemanticAnalyzer(ClassNode clazz) {
		registerNestedClasses(clazz);
		analyzeClass(clazz);
    }
	
    private void registerNestedClasses(ClassNode clazz) throws SemanticError {
		ClassInfo classInfo = new ClassInfo();
		symbolTable.addClass(clazz.getFullName(), classInfo);
		for(AstNode decl : clazz.getBody().getDeclarations()) {
			if(decl instanceof FieldNode) {
				classInfo.addField(((FieldNode)decl).getName(), ((FieldNode)decl).getType());
			}
			else if(decl instanceof MethodNode) {
				MethodNode method = (MethodNode)decl;
                List<VarType> paramTypes = new ArrayList<>();
                for (ParameterNode param : method.getParameters()) {
                    paramTypes.add(param.getType());
                }
                classInfo.addMethod(method.getName(), new MethodInfo(method.getType(), paramTypes));
			}
		}
		
		BlockNode body = clazz.getBody();
		if(null != body) {
			for (AstNode nested : body.getDeclarations()) {
				if(nested instanceof ClassNode) {
					registerNestedClasses((ClassNode)nested);
				}
			}
		}
	}
	
	private void analyzeClass(ClassNode classNode) throws SemanticError {
		ClassInfo classInfo = symbolTable.getClassInfo(classNode.getFullName());
		symbolTable.enterScope();

		try {
			// Анализ содержимого класса
			BlockNode classBody = classNode.getBody();
			for (AstNode decl : classBody.getDeclarations()) {
				if (decl instanceof ClassNode) {
					analyzeClass((ClassNode) decl);
				}
				else if (decl instanceof FieldNode)  {
					analyzeField((FieldNode) decl, classNode.getFullName());
				}
				else if (decl instanceof MethodNode) {
					analyzeMethod((MethodNode) decl, classNode.getFullName());
				}
			}
		} finally {
			symbolTable.exitScope();
		}
	}
	
	// Анализ поля
	private void analyzeField(FieldNode field, String className) throws SemanticError {
		ClassInfo classInfo = symbolTable.getClassInfo(className);
		
		boolean isFinal = field.getModifiers().contains(Keyword.FINAL);
		
		// Проверка типа поля
	    VarType declaredType = field.getType();
		if (VarType.UNKNOWN == field.getType()) {
			throw new SemanticError("Unknown field type: " + declaredType, field.getLine(), field.getColumn());
        }

		// Проверка инициализатора
		if (null != field.getInitializer()) {
			VarType initType = checkExpression(field.getInitializer());
			// Проверяем совместимость типов
			if (!declaredType.isCompatibleWith(initType)) {
				throw new SemanticError(String.format("Type mismatch: cannot assign %s to %s", initType.getName(), declaredType.getName()),
										field.getInitializer().getLine(), field.getInitializer().getColumn());
			}
		}

		// Проверка final поля
		if (isFinal && field.getInitializer() == null) {
			throw new SemanticError("Final field must be initialized", field.getLine(), field.getColumn());
		}
		
		// 4. Добавляем поле в таблицу символов
		try {
			symbolTable.addSymbol(field.getName(), declaredType, !isFinal, field.getLine()); // mutable если не final
		}
		catch (SemanticError e) {
			throw new SemanticError(String.format("Field '%s': %s", field.getName(), e.getMessage()), field.getLine(), field.getColumn());
		}
	}
	
	// Анализ метода экземпляра
	private void analyzeMethod(MethodNode method, String className) throws SemanticError {
		ClassInfo classInfo = symbolTable.getClassInfo(className);
		
		// Проверяем тип возвращаемого значения
		VarType returnType = method.getType();
		if (returnType == VarType.UNKNOWN) {
			throw new SemanticError("Invalid return type: " + method.getType(), method.getLine(), method.getColumn());
		}

		// Входим в новую область видимости для параметров метода
		symbolTable.enterScope();

		try {
			// Добавление this для нестатических методов
            if (!method.getModifiers().contains(Keyword.STATIC)) {
             //TODO   symbolTable.addSymbol("this", VarType.addClass(className), false, method.getLine());
            }
			
			// Добавляем параметры в таблицу символов
			analyzeParameters(method.getParameters());

			// Проверяем тело метода
			analyzeBody(method.getBody(), returnType, className, method.isConstructor());
		}
		finally {
			symbolTable.exitScope();
		}
	}

	// Анализ параметров
	private void analyzeParameters(List<ParameterNode> params) throws SemanticError {
        for (ParameterNode param : params) {
			VarType paramType = param.getType();
			if (paramType == VarType.UNKNOWN) {
				throw new SemanticError("Unknown parameter type: " + param.getType(), param.getLine(), param.getColumn());
			}

			symbolTable.addSymbol(param.getName(), paramType, true, param.getLine());
        }
    }

	// Проверяет блок кода (список statements) с учётом ожидаемого типа возврата.
	private boolean analyzeBody(BlockNode block, VarType expectedReturnType, String className, boolean isConstructor) throws SemanticError {
		boolean hasReturn = false;
		
		for (AstNode stmt : block.getDeclarations()) {
			if (stmt instanceof ReturnNode) {
				// Проверяем соответствие типа возвращаемого значения
				ReturnNode returnNode = (ReturnNode) stmt;
				VarType actualType = returnNode.getExpression() != null ? checkExpression(returnNode.getExpression()) : VarType.VOID;

				if (!actualType.isCompatibleWith(expectedReturnType)) {
					throw new SemanticError(String.format("Return type mismatch: expected %s, got %s", expectedReturnType.getName(),
											actualType.getName()), returnNode.getLine(), returnNode.getColumn());
				}
				hasReturn = true;
			}
			else if (stmt instanceof ExpressionNode) {
				checkExpression((ExpressionNode)stmt);
			}
			
			// Рекурсивная проверка вложенных блоков
			for (BlockNode innerBlock : stmt.getBlocks()) {
				if(analyzeBody(innerBlock, expectedReturnType, className, false)) {
					hasReturn = true;
				}
			}
		}

		// Проверка возвращаемого значения
		if (expectedReturnType != VarType.VOID && !isConstructor && !hasReturn) {
			throw new SemanticError("Missing return statement in non-void method", block.getLine(), block.getColumn());
		}
		
		return hasReturn;
	}
	
	private boolean hasReturnStatement(BlockNode block) {
		for (AstNode stmt : block.getDeclarations()) {
			if (stmt instanceof ReturnNode) return true;
			
			for(BlockNode innerBlock : stmt.getBlocks()) {
				if (hasReturnStatement(innerBlock)) return true;
			}
		}
		return false;
	}
	
	private VarType checkExpression(ExpressionNode expr) throws SemanticError {
		if(expr instanceof MethodCallExpression) {
			return checkMethodCallExpression((MethodCallExpression)expr);
		}
		return expr.semanticAnalyze(symbolTable);
	}
	
	private VarType checkMethodCallExpression(MethodCallExpression expr) throws SemanticError {
		// Проверка целевого объекта
		VarType targetType = checkExpression(expr.getTarget());

		if (null != targetType && !targetType.isClassType()) {
			throw new SemanticError("Method call on non-class type: " + targetType, expr.getLine(), expr.getColumn());
		}

		// Поиск метода
		MethodInfo methodInfo = symbolTable.lookupMethod(targetType.getName(), expr.getMethodName());
		if(null == methodInfo) {
			throw new SemanticError(String.format("Method '%s' not found in class %s", expr.getMethodName(), targetType.getName()), expr.getLine(),
									expr.getColumn());
		}
		
		// Проверка аргументов
		List<ExpressionNode> args = expr.getArguments();
		int size = null == methodInfo.getParameters() ? 0 : methodInfo.getParameters().size();
		if (args.size() != size) {
			throw new SemanticError(String.format("Expected %d arguments, got %d", size, args.size()), expr.getLine(), expr.getColumn());
		}

		for (int i = 0; i < size; i++) {
			VarType argType = checkExpression(args.get(i));
			VarType paramType = methodInfo.getParameters().get(i);

			if (!argType.isCompatibleWith(paramType)) {
				throw new SemanticError(String.format("Argument %d type mismatch: expected %s, got %s", i+1, paramType, argType), args.get(i).getLine(),
										args.get(i).getColumn());
			}
		}

		return methodInfo.getReturnType();
	}
}
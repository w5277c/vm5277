/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.compiler.nodes;

import java.util.Arrays;
import java.util.List;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.FieldAccessExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.UnresolvedReferenceExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class FieldNode extends AstNode implements InitNodeHolder {
	private	final	Set<Keyword>	modifiers;
	private			VarType			type;
	private			String			name;
	private			ExpressionNode	init;
	
	public FieldNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType  type, String name) {
		super(tb, mc);

		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            init = null;
        }
		else {
			consumeToken(tb);
			try {init = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return init;
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
	public boolean isPrivate() {
		return modifiers.contains(Keyword.PRIVATE);
	}

	@Override
	public String getNodeType() {
		return "field";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		try{validateModifiers(modifiers, Keyword.STATIC, Keyword.FINAL, Keyword.PRIVATE, Keyword.PUBLIC);} catch(CompileException e) {addMessage(e);}
		
		if((!isFinal() || !isStatic()) && Character.isUpperCase(name.charAt(0))) {
			addMessage(new WarningMessage("Field name should start with lowercase letter:" + name, sp));
		}

		if(null != init) {
			if(!init.preAnalyze()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(null != init) {
			if(init instanceof NewArrayExpression) {
				try {
					NewArrayExpression nae = (NewArrayExpression)init;
					if(null != nae.getType(scope)) {
						if(type.getArrayDepth() != nae.getType(scope).getArrayDepth()) {
							markError("Array dimension mismatch, expected:" + type.getArrayDepth() + ", found:" + nae.getType(scope).getArrayDepth());
							return false;
						}
						type = nae.getType(scope);
						// Проверяем вложенность массивов
						if (type.getArrayDepth() > 3) {
							markError("Array nesting depth exceeds maximum allowed (3)");
							return false;
						}
					}
				}
				catch(Exception ex){}
			}

			if(!init.declare(scope)) {
				return false;
			}
		}

		if(scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			boolean isFinal = modifiers.contains(Keyword.FINAL);
			symbol = new FieldSymbol(name, type, isFinal || VarType.CSTR == type, isStatic(), isPrivate(), classScope, this);

			try{classScope.addField(symbol);}
			catch(CompileException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in filed:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterField(type, isStatic(), name);
			symbol.setCGScope(cgScope);

			// Проверка инициализации final-полей
			if(isFinal() && init == null) markError("Final field '" + name + "' must be initialized");

			// Анализ инициализатора, если есть
			if(null!=init) {
				if(!init.postAnalyze(scope, cg)) {
					cg.leaveField();
					return false;
				}
				if(init instanceof UnresolvedReferenceExpression) {
					init = ((UnresolvedReferenceExpression)init).getResolvedExpr();
				}

				// Проверка совместимости типов
				VarType initType = init.getType(scope);
				if(!isCompatibleWith(scope, type, initType)) {
					// Дополнительная проверка втоматического привдения целочисленной константы к fixed.
					if(VarType.FIXED == type && init instanceof LiteralExpression && initType.isInteger()) {
						long num = ((LiteralExpression)init).getNumValue();
						if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
							markError("Type mismatch: cannot assign " + initType + " to " + type);
							cg.leaveField();
							return false;
						}
					}
					else {
						markError("Type mismatch: cannot assign " + initType + " to " + type);
						cg.leaveField();
						return false;
					}
				}
				// Дополнительная проверка на сужающее преобразование
				if(type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) {
					markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast");
					cg.leaveField();
					return false;
				}

				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					init = optimizedExpr; //TODO не передаю сохданный Symbol?
				}
				
				if(!symbol.isReassigned()) {
					if(	init instanceof LiteralExpression ||
						(init instanceof VarFieldExpression && null!=((VarFieldExpression)init).getSymbol() &&
						((VarFieldExpression)init).getSymbol().isFinal())) {

						modifiers.add(Keyword.FINAL);
						symbol.setFinal(true);
						if(type.isArray() || VarType.CSTR==type) {
							symbol.setReassigned();
						}
					}
				}
			}
		}
		catch (CompileException e) {
			markError(e);
		}

		cg.leaveField();		
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(cgDone || disabled) return null;
		cgDone = true;

		CGFieldScope fScope = (CGFieldScope)cgScope;		
		CGClassScope cScope = (CGClassScope)fScope.getParent();
//		CGScope oldCGScope = cg.setScope(fScope);
		
		Boolean accUsed = null;
		((CGFieldScope)cgScope).build();

		if(VarType.CSTR == type) {
			if(init instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)init;
				if(VarType.CSTR == le.getType(null)) {
					((CGFieldScope)cgScope).setDataSymbol(cg.defineData(cgScope.getResId(), -1, (String)le.getValue()));
					//TODO рудимент?
					//symbol.getConstantOperand().setValue(cgScope.getResId());
				}
			}
			else throw new Exception("unexpected expression:" + init + " for constant");
		}
		else {
			if(null == init) {
				// Ничего не делаем, инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			}
			else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				LiteralExpression le = (LiteralExpression)init;
				boolean isFixed = le.isFixed() || VarType.FIXED == ((CGFieldScope)cgScope).getType();
				cScope.getFieldsInitCont().append(cg.constToCells(null, isFixed ? le.getFixedValue() : le.getNumValue(), ((CGFieldScope)cgScope).getCells(), isFixed));
			}
			else if(init instanceof FieldAccessExpression) {
				cScope.getFieldsInitCont().append(cg.constToCells(null, 0, ((CGFieldScope)cgScope).getCells(), false));
				accUsed = true;
			}
			else if(init instanceof NewExpression) {
				init.codeGen(cg, null, true);
				accUsed = true;
			}
			else if(init instanceof NewArrayExpression) {
				init.codeGen(cg, null, true);
				if(VarType.CLASS == ((CGFieldScope)cgScope).getType()) {
					cg.updateClassRefCount(cgScope, ((CGFieldScope)cgScope).getCells(), true);
				}
//				((FieldSymbol)symbol).setArrayDimensions(((NewArrayExpression)init).getConsDimensions());
//				cg.accToCells(cgScope, ((CGFieldScope)cgScope));
			}
			else if(init instanceof BinaryExpression) {
				CGScope oldScope = cg.setScope(cgScope);
				CGScope brScope = cg.enterBranch();
				init.codeGen(cg, brScope, true);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType(null);
				if(VarType.CLASS == initType) {
					cg.updateClassRefCount(brScope, fScope.getCells(), true);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(brScope, fScope.getCells(), true, init instanceof ArrayExpression);
				}
				cg.constToAcc(brScope, 1, 1, false);
				CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
				cg.jump(brScope, lbScope);
				brScope.append(((CGBranchScope)brScope).getEnd());
				cg.constToAcc(brScope, 1, 0, false);
				brScope.append(lbScope);
				cg.accToCells(brScope, fScope);
				accUsed = true;
				cg.leaveBranch();
				cg.setScope(oldScope);
			}
			else if(init instanceof EnumExpression) {
				cg.accCast(null, VarType.BYTE);
				cgScope.append(cg.constToCells(cgScope, ((EnumExpression)init).getIndex(), fScope.getCells(), false));
			}
			else {
				init.codeGen(cg, null, true);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType(null);
				if(VarType.CLASS == initType) {
					cg.updateClassRefCount(cgScope, fScope.getCells(), true);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, fScope.getCells(), true, init instanceof ArrayExpression);
				}
				cg.accToCells(cgScope, ((CGFieldScope)cgScope));
				accUsed = true;
			}
		}
		
		return accUsed;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null != init) return Arrays.asList(init);
		return null;
	}
	
	@Override
	public ExpressionNode getInitNode() {
		return init;
	}
}

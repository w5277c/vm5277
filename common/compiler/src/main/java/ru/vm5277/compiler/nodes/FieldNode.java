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
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.VarType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.Main;

public class FieldNode extends AstNode implements InitNodeHolder {
	private	final	ObjectTypeNode	objectTypeNode;
	private	final	Set<Keyword>	modifiers;
	private			VarType			type;
	private			String			name;
	private			ExpressionNode	init;
	
	public FieldNode(Instance inst, TokenBuffer tb, ObjectTypeNode objectTypeNode, Set<Keyword> modifiers, VarType type, String name) {
		super(inst, tb);
		
		this.objectTypeNode = objectTypeNode;
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            init = null;
        }
		else {
			consumeToken(tb);
			try {
				init = new ExpressionNode(inst, tb).parse();
			}
			catch(CompileException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
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
		return modifiers.contains(J8BKeyword.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}

	public boolean isPrivate() {
		return modifiers.contains(J8BKeyword.PRIVATE);
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try{
			validateModifiers(modifiers, J8BKeyword.STATIC, J8BKeyword.FINAL, J8BKeyword.PRIVATE, J8BKeyword.PUBLIC);
		}
		catch(CompileException e) {
			addMessage(e);
			result = false;
		}
		
		if(isFinal()) {
			if(!name.equals(name.toUpperCase())) {
				markWarning("Final field name should in UPPER_CASE: " + name);
			}
		}
		else if(Character.isUpperCase(name.charAt(0))) {
			markWarning("Field name should start with lowercase letter: " + name);
		}

		if(null!=init) {
			result&=init.preAnalyze();
			
			if(init instanceof VarFieldExpression) {
				if(name.equals(((VarFieldExpression)init).getName())) {
					markError("Self-assignment for '" + ((VarFieldExpression)init).getName() + "' has no effect");
					result = false;
				}
			}
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		if(null!=init) {
			if(init instanceof NewArrayExpression) {
				try {
					NewArrayExpression nae = (NewArrayExpression)init;
					if(null != nae.getType()) {
						if(type.getArrayDepth() != nae.getType().getArrayDepth()) {
							markError("Array dimension mismatch, expected:" + type.getArrayDepth() + ", found:" + nae.getType().getArrayDepth());
							result = false;
						}
						else {
							type = nae.getType();
							// Проверяем вложенность массивов
							if(type.getArrayDepth()>3) {
								markError("Array nesting depth exceeds maximum allowed (3)");
								result = false;
							}
						}
					}
				}
				catch(Exception ex){}
			}
			
			result&=init.declare(scope);
		}
		
		if(result) {
			if(scope instanceof ClassScope) {
				ClassScope classScope = (ClassScope)scope;
				boolean isFinal = modifiers.contains(J8BKeyword.FINAL);
				symbol = new FieldSymbol(name, type, isFinal || VarType.CSTR == type, isStatic(), isPrivate(), classScope, this);

				try{classScope.addField(symbol);}
				catch(CompileException e) {markError(e);}
			}
			else {
				markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in filed:" + name);
			}
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);

		try {
			cgScope = cg.enterField(parent, type, isStatic(), name);
			symbol.setCGScope(cgScope);
			
			// Проверка инициализации final-полей
			if(isFinal() && init==null) {
				markError("Final field '" + name + "' must be initialized");
				result = false;
			}

			// Анализ инициализатора, если есть
			if(null!=init) {
				if(!init.postAnalyze(scope, cg, cgScope)) {
					result = false;
				}
				else {
					// Резолвинг QualifiedPathExpression
					ExpressionNode resolved = resolveQualifiedPathExpr(init);
					if(null!=resolved) {
						init = resolved;
					}
					
					((CGCellsScope)cgScope).setArrayView(init instanceof ArrayExpression);
					// Проверка совместимости типов
					VarType initType = init.getType();
					if(initType.isClassType() && type.isClassType()) {
						if(initType!=type) {
							CIScope cis = scope.getThis().resolveCI(null, initType.getClassName(), false);
							if(!cis.isImplements(type)) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
						}
					}
					else if(!isCompatibleWith(scope, type, initType)) {
						// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
						if(VarType.FIXED == type && init instanceof LiteralExpression && initType.isInteger()) {
							long num = ((LiteralExpression)init).getNumValue();
							if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
						}
						else {
							markError("Type mismatch: cannot assign " + initType + " to " + type);
							result = false;
						}
					}

					if(result) {
						// Дополнительная проверка на сужающее преобразование
						if(type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) {
							markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast");
							result = false;
						}
					}
				}
			}
		}
		catch (CompileException e) {
			markError(e);
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		if(null!=init) {
			init.codeOptimization(scope, cg);
		
			try {
				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}

			if(!symbol.isReassigned()) {
				// Возможно упростить?
				if(	init instanceof LiteralExpression || init.getType().isEnum() ||
					(init instanceof VarFieldExpression && null!=((VarFieldExpression)init).getSymbol() &&
					((VarFieldExpression)init).getSymbol().isFinal())) {

					modifiers.add(J8BKeyword.FINAL);
					symbol.setFinal(true);
					if(type.isArray() || VarType.CSTR==type) {
						symbol.setReassigned();
					}
				}
			}
			try {
				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		CGFieldScope fScope = (CGFieldScope)cgScope;		
		CGClassScope cScope = (CGClassScope)fScope.getParent();
		
		if(cg.getDevice().isLowRAM() && StrictLevel.NONE!=Main.getStrictLevel() && !isStatic()) {
			if(StrictLevel.STRONG==Main.getStrictLevel()) {
				markError("HEAP usage on low memory device " + cg.getDevice().getUniqueName());
			}
			else {
				markWarning("HEAP usage on low memory device " + cg.getDevice().getUniqueName());
			}
		}
		
		Boolean accUsed = null;
		((CGFieldScope)cgScope).build();

		if(VarType.CSTR == type) {
			if(init instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)init;
				if(VarType.CSTR == le.getType()) {
					fScope.setCells(new CGCells(cg.defineData(fScope.getResId(), -1, (String)le.getValue()).getLabel()));
				}
			}
			else throw new CompileException("unexpected expression:" + init + " for constant");
		}
		else {
			// Инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			if(null==init) {
				// Ничего не делаем, инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			}
			else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				LiteralExpression le = (LiteralExpression)init;
				boolean isFixed = le.getType().isFixedPoint() || VarType.FIXED == ((CGFieldScope)cgScope).getType();
				cgScope.append(cg.constToCells(null, isFixed ? le.getFixedValue() : le.getNumValue(), ((CGFieldScope)cgScope).getCells(), isFixed));
			}
			else if(init instanceof NewExpression) {
				init.codeGen(cg, true, excs);
				cg.accumLock(VarType.CLASS);
				cg.accToCells(cgScope, type, fScope);
				cg.accumUnlock();
				accUsed = true;
			}
			else if(init instanceof NewArrayExpression) {
				init.codeGen(cg, true, excs);
				//При инициализации счетчику ссылок сразу назначаем 1 (не допускаем создание висящих объектов)
				cg.accumLock(VarType.SHORT);
				cg.accToCells(cgScope, type, fScope);
				cg.accumUnlock();
			}
			else if(init instanceof BinaryExpression) {
//!!!				CGScope oldScope = cg.setScope(cgScope);
				CGBranch branch = new CGBranch();
				cgScope.setBranch(branch);
				cg.accumLock(type);
				init.codeGen(cg, true, excs);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, fScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, fScope.getCells(), true, ((CGCellsScope)cgScope).isArrayView());
				}
				if(branch.isUsed()) {
					cg.constToAcc(cgScope, 1, 1, false);
					CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
					cg.jump(cgScope, lbScope);
					cgScope.append(((CGBranch)branch).getEnd());
					cg.constToAcc(cgScope, 1, 0, false);
					cgScope.append(lbScope);
					cg.accToCells(cgScope, type, fScope);
				}
				cg.accToCells(cgScope, type, fScope);
				cg.accumUnlock();
				accUsed = true;
			}
			else if(init instanceof EnumExpression) {
				cg.accumLock(type);
				cg.constToCells(cgScope, ((EnumExpression)init).getIndex(), fScope.getCells(), false);
				cg.accumUnlock();
			}
			else {
				boolean optimized = false;
				try {
					if(	init instanceof VarFieldExpression && null!=init.getSymbol() && null!=init.getSymbol().getCGScope() &&
						init.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)init.getSymbol().getCGScope()).getCells()) {
					
						init.codeGen(cg, false, excs);
						cg.cellsToCells(cgScope, fScope.getCells(), type, ((CGCellsScope)init.getSymbol().getCGScope()).getCells(), init.getType());
						optimized = true;
					}
				}
				catch(Exception ex) {
					System.out.println("TODO:" + ex.getMessage());
				}

				if(!optimized) {
					cg.accumLock(type);
					init.codeGen(cg, true, excs);
					cg.accToCells(cgScope, type, fScope);
					cg.accumUnlock();
					accUsed = true;
				}

				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgScope);
					cg.updateClassRefCount(cgScope, fScope.getCells(), true);
					cg.popHeapReg(cgScope);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgScope, fScope.getCells(), true, ((CGCellsScope)cgScope).isArrayView());
				}
			}
			
			if(null!=init) {
				cgScope.getParent().remove(cgScope);
				if(isStatic()) {
					cScope.getStatFieldsInitCont().append(cgScope);
				}
				else {
					cScope.getFieldsInitCont().append(cgScope);
				}
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
	
	public ObjectTypeNode getObjectTypeNode() {
		return objectTypeNode;
	}

	@Override
	public String toString() {
		return (StrUtils.toString(modifiers) + " " + type + " " + name).trim();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}

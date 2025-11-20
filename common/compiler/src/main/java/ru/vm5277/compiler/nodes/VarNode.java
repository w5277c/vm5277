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
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.EnumExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.NewArrayExpression;
import ru.vm5277.compiler.nodes.expressions.NewExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class VarNode extends AstNode implements InitNodeHolder {
	private	final	Set<Keyword>	modifiers;
	private			VarType			type;
	private	final	String			name;
	private			ExpressionNode	init;
	
	public VarNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            init = null;
        }
		else {
			consumeToken(tb);
			try {
				init = new ExpressionNode(tb, mc).parse();
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
		return modifiers.contains(Keyword.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		try{
			validateModifiers(modifiers, Keyword.STATIC, Keyword.FINAL);
		}
		catch(CompileException e) {
			addMessage(e);
			result = false;
		}
		
		if(Character.isUpperCase(name.charAt(0))) {
			markWarning("Variable name should start with lowercase letter:" + name);
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
			if(scope instanceof BlockScope) {
				BlockScope blockScope = (BlockScope)scope;
				symbol = new VarSymbol(name, type, isFinal() || VarType.CSTR == type, isStatic(), scope, this);

				try {blockScope.addVariable(symbol);}
				catch(CompileException e) {markError(e);}

	//			if(init instanceof NewArrayExpression) {
	//				((VarSymbol)symbol).setArrayDimensions(((NewArrayExpression)init).getConsDimensions());
	//			}
			}
			else {
				markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in var:" + name);
			}
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);

		try {
			cgScope = cg.enterLocal(type, (-1 == type.getSize() ? cg.getRefSize() : type.getSize()), VarType.CSTR == type, name);
			symbol.setCGScope(cgScope);
			
			// Проверка инициализации final-полей
			if(isFinal() && init==null) {
				markError("Final variable  '" + name + "' must be initialized");
				result = false;
			}

			// Анализ инициализатора, если есть
			if(null!=init) {
				if(!init.postAnalyze(scope, cg)) {
					result = false;
				}

				ExpressionNode optimizedExpr = init.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					init = optimizedExpr;
				}
				
				if(result) {
					((CGCellsScope)cgScope).setArrayView(init instanceof ArrayExpression);
					// Проверка совместимости типов
					VarType initType = init.getType();
					if(initType.isClassType() && type.isClassType()) {
						if(initType!=type) {
							CIScope cis = scope.getThis().resolveCI(initType.getClassName(), false);
							if(!cis.isImplements(type)) {
								markError("Type mismatch: cannot assign " + initType + " to " + type);
								result = false;
							}
						}
					}
					else if(!isCompatibleWith(scope, type, initType)) {
						// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
						if(VarType.FIXED == type && init instanceof LiteralExpression && initType.isIntegral()) {
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
		
		cg.leaveLocal();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
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

					modifiers.add(Keyword.FINAL);
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
		
		cg.setScope(oldScope);
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		CGScope cgs = null == parent ? cgScope : parent;
		CGVarScope vScope = (CGVarScope)cgScope;
		
		Boolean accUsed = null;
		vScope.build();
		if(VarType.CSTR == type) {
			if(init instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)init;
				if(VarType.CSTR == le.getType()) {
					vScope.setCells(new CGCells(cg.defineData(vScope.getResId(), -1, (String)le.getValue()).getLabel()));
				}
			}
			else throw new CompileException("unexpected expression:" + init + " for constant");
		}
		else {
			// Инициализация(заполнение нулями необходима только регистрам, остальные проинициализированы вместе с HEAP/STACK)
			if(null==init) {
				if(CGCells.Type.REG==vScope.getCells().getType()) cgs.append(cg.constToCells(cgs, 0, vScope.getCells(), false));
			}
			else if(init instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				LiteralExpression le = (LiteralExpression)init;
				boolean isFixed = le.isFixed() || VarType.FIXED == vScope.getType();
				cgs.append(cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(), vScope.getCells(), isFixed));
			}
			else if(init instanceof NewExpression) {
				init.codeGen(cg, null, true);
				cg.accCast(null, VarType.SHORT);
				cg.accToCells(cgs, vScope);
			}
			else if(init instanceof NewArrayExpression) {
				init.codeGen(cg, null, true);
				//TODO оптимизация, можно сразу назначать счетчику 1
				//cg.updateRefCount(vScope, vScope.getCells(), true);
				
				//Перенес в declare //((VarSymbol)symbol).setArrayDimensions(((NewArrayExpression)initializer).getConsDimensions());
				cg.accCast(null, VarType.SHORT);
				cg.accToCells(cgs, vScope);
			}
//			else if(init instanceof ArrayExpression) {
//				init.codeGen(cg, null, true);
//				//cg.arrToCells(cgs, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//				//cg.arrToCells(cgs, (CGArrCells)((CGCellsScope)init.getSymbol().getCGScope()).getCells(), vScope.getCells());
//			}
			else if(init instanceof BinaryExpression) {
				CGScope oldScope = cg.setScope(cgs);
				CGBranch branch = new CGBranch();
				cgs.setBranch(branch);
				
				init.codeGen(cg, cgs, true);
				//TODO проверить на объекты и на массивы(передача ref)
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgs);
					cg.updateClassRefCount(cgs, vScope.getCells(), true);
					cg.popHeapReg(cgs);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgs, vScope.getCells(), true, ((CGCellsScope)cgs).isArrayView());
				}
				if(branch.isUsed()) {
					cg.constToAcc(cgs, 1, 1, false);
					CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
					cg.jump(cgs, lbScope);
					cgs.append(((CGBranch)branch).getEnd());
					cg.constToAcc(cgs, 1, 0, false);
					cgs.append(lbScope);
					cg.accToCells(cgs, vScope);
				}
				accUsed = true;
				cg.setScope(oldScope);
			}
			else if(init instanceof ArrayExpression) {
				init.codeGen(cg, cgs, false);
				cg.accToCells(cgs, vScope);
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgs);
					cg.updateClassRefCount(cgs, vScope.getCells(), true);
					cg.popHeapReg(cgs);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgs, vScope.getCells(), true, ((CGCellsScope)vScope).isArrayView());
				}
				accUsed = true;
			}
			else if(init instanceof EnumExpression) {
				cgs.append(cg.constToCells(cgs, ((EnumExpression)init).getIndex(), vScope.getCells(), false));
			}
			else {
				init.codeGen(cg, cgs, true);
				cg.accResize(type);
				cg.accToCells(cgs, vScope);
				VarType initType = init.getType();
				if(VarType.CLASS == initType) {
					cg.pushHeapReg(cgs);
					cg.updateClassRefCount(cgs, vScope.getCells(), true);
					cg.popHeapReg(cgs);
				}
				else if(initType.isArray()) {
					cg.updateArrRefCount(cgs, vScope.getCells(), true, ((CGCellsScope)vScope).isArrayView());
				}

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
	
	@Override
	public String toString() {
		return (StrUtils.toString(modifiers) + " " + type + " " + name).trim();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}

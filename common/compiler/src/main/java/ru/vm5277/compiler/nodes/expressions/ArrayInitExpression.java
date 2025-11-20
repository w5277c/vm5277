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

package ru.vm5277.compiler.nodes.expressions;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.Pair;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.DataSymbol;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.semantic.CIScope;

public class ArrayInitExpression extends ExpressionNode {
	public static class ConstRange {
		private int			start;
		private	int			end;
		private	DataSymbol	symbol;
		
		public ConstRange(int start, int end, DataSymbol symbol) {
			this.start = start;
			this.end = end;
			this.symbol = symbol;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
		
		public DataSymbol getSymbol() {
			return symbol;
		}
	}
	
	private			VarType					basedType;
	private			List<ExpressionNode>	valueExprs	= new ArrayList<>();
	private			int[]					dimensions	= new int[]{0, 0, 0};
	private			List<ExpressionNode>	linearValues= new ArrayList<>();
	private			List<ConstRange>		constRanges	= new ArrayList<>();
	
	public ArrayInitExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, VarType type) {
        super(tb, mc, sp);
	
		try {
			consumeToken(tb, Delimiter.LEFT_BRACE); // Потребляем '{'
			while(!tb.match(TokenType.EOF)) {
				if(tb.match(Delimiter.LEFT_BRACE)) { // Проверяем на '{'
					valueExprs.add(new ArrayInitExpression(tb, mc, sp, type));
				}
				else {
					valueExprs.add(new ExpressionNode(tb, mc).parse());
				}
				
				if(tb.match(Delimiter.RIGHT_BRACE)) {
					break;
				}
				consumeToken(tb, Delimiter.COMMA); // Потребляем ','
			}
			consumeToken(tb, Delimiter.RIGHT_BRACE); // Потребляем '}'
		}
		catch(CompileException e){markFirstError(e);}
		
		this.basedType = type.getElementType();
		while(basedType.isArray()) {
			basedType = basedType.getElementType();
		}

		try {
			checkValues(1, valueExprs);
		}
		catch(CompileException ex) {
			markError(ex);
		}
		
		this.type = type;
    }

	@Override
	public boolean preAnalyze() {
		boolean result = true;

		expandValue(valueExprs, linearValues); 
		
		for(ExpressionNode valueExpr : linearValues) {
			if(!(valueExpr instanceof ArrayInitExpression)) {
				result &= valueExpr.preAnalyze();
			}
		}
		
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		for(ExpressionNode valueExpr : linearValues) {
			if(!(valueExpr instanceof ArrayInitExpression)) {
				result &= valueExpr.declare(scope);
			}
		}
		
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;

		try {
			List<Object> consts = null;
			int pos=-1;
			for(int i=0; i<linearValues.size(); i++) {
				ExpressionNode valueExpr = linearValues.get(i);
				if(!(valueExpr instanceof ArrayInitExpression)) {
					result&=valueExpr.postAnalyze(scope, cg);
				}
				
				if(result) {
					ExpressionNode optimizedExpr = valueExpr.optimizeWithScope(scope, cg);
					if(null != optimizedExpr) {
						optimizedExpr.postAnalyze(scope, cg);
						// ASTPrinter работает с valueExprs
						updateNestedElement(valueExprs, valueExpr, optimizedExpr);
						
						valueExpr = optimizedExpr;
						linearValues.set(i, valueExpr);
					}

					// Проверка совместимости типов (аналогично VarNode.postAnalyze)
					VarType valueType = valueExpr.getType();
					if(valueType.isClassType() && basedType.isClassType()) {
						if(valueType != basedType) {
							CIScope cis = scope.getThis().resolveCI(valueType.getClassName(), false);
							if(!cis.isImplements(basedType)) {
								markError("Type mismatch: cannot assign " + valueType + " to " + basedType);
								result = false;
							}
						}
					}
					else if (!isCompatibleWith(scope, basedType, valueType)) {
						// Дополнительная проверка автоматического привдения целочисленной константы к fixed.
						if(VarType.FIXED == basedType && valueExpr instanceof LiteralExpression && valueType.isIntegral()) {
							long num = ((LiteralExpression)valueExpr).getNumValue();
							if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
								markError("Type mismatch: cannot assign " + valueType + " to " + basedType);
								result = false;
							}
						}
						else {
							markError("Type mismatch: cannot assign " + valueType + " to " + basedType);
							result = false;
						}
					}

					// Дополнительная проверка на сужающее преобразование
					if (basedType.isNumeric() && valueType.isNumeric() && basedType.getSize() < valueType.getSize()) {
						markError("Narrowing conversion from " + valueType + " to " + basedType + " requires explicit cast"); 
						result = false;
					}

					if(result) {
						if(valueExpr instanceof LiteralExpression) {
							if(null==consts) {
								consts = new ArrayList<>();
								pos = i;
							}
						}
						else {
							if(null!=consts) {
								int size = (-1==basedType.getSize() ? cg.getRefSize() : basedType.getSize());
								//TODO 0x02 константа зависящая от библиотеки кодогенератора(для AVR переносить во FLASH блоки <3 байт не выгодно)
								if(0x02<consts.size()*size) {
									constRanges.add(new ConstRange(pos, i-1, cg.defineData(	cg.genId(), consts.size()*size,
																							new Pair<VarType,List<Object>>(basedType, consts))));
								}
								consts = null;
							}
						}
						if(null!=consts) {
							if(basedType.isFixedPoint()) {
								consts.add(((LiteralExpression)valueExpr).getFixedValue());
							}
							else {
								consts.add(((LiteralExpression)valueExpr).getNumValue());
							}
						}
					}
				}
			}
			if(null!=consts) {
				int size = (-1==basedType.getSize() ? cg.getRefSize() : basedType.getSize());
				//TODO 0x02 константа зависящая от библиотеки кодогенератора(для AVR переносить во FLASH блоки <3 байт не выгодно)
				//TODO переносоить копированием блоки мнее блока передачи параметров функции копирования(минимум 3 инструкции) тоже не выгодно
				if(0x02<consts.size()*size) {
					constRanges.add(new ConstRange(pos, linearValues.size()-1, cg.defineData(	cg.genId(), consts.size()*size,
																								new Pair<VarType,List<Object>>(basedType, consts))));
				}
			}
		}
		catch(CompileException ex) {
			markError(ex);
			result = false;
		}
		
		return result;
	}

	private boolean updateNestedElement(List<ExpressionNode> valueExprs, ExpressionNode oldExpr, ExpressionNode newExpr) {
		for(int i=0; i<valueExprs.size(); i++) {
			ExpressionNode expr = valueExprs.get(i);
			
			if(expr instanceof ArrayInitExpression) {
				if(updateNestedElement(((ArrayInitExpression)expr).getValueExprs(), oldExpr, newExpr)) {
					return true;
				}
			}
			else if(expr==oldExpr) {
				valueExprs.set(i, newExpr);
				return true;
			}
		}
		return false;
	}
	
	private void checkValues(int depth, List<ExpressionNode> list) throws CompileException {
		if(0x03<depth) {
			throw new CompileException("Array nesting depth exceeds maximum allowed (3)");
		}
		if(0==dimensions[depth-1]) {
			dimensions[depth-1] = list.size();
		}
		else if(dimensions[depth-1] != list.size()) {
			throw new CompileException("Inconsistent subarray lengths in multidimensional array");
		}

		Boolean isValue = null;
		for(ExpressionNode expr : list) {
			if(null==isValue) {
				if(expr instanceof ArrayInitExpression) isValue = false;
				else isValue = true;
			}

			if((isValue && expr instanceof ArrayExpression) || (!isValue && !(expr instanceof ArrayInitExpression))) {
				throw new CompileException("Invalid mixed array initializer");
			}

			if(!isValue) {
				checkValues(depth+1, ((ArrayInitExpression)expr).getValueExprs());
			}
		}
	}
	
	private void expandValue(List<ExpressionNode> list, List<ExpressionNode> target) {
		for(ExpressionNode expr : list) {
			if(expr instanceof ArrayInitExpression) {
				expandValue(((ArrayInitExpression)expr).getValueExprs(), target);
			}
			else {
				target.add(expr);
			}
		}
	}
	
	
	public List<ExpressionNode> getValueExprs() {
		return valueExprs;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		int size = (-1==basedType.getSize() ? cg.getRefSize() : basedType.getSize());
		int rangeId=0;
		for(int i=0; i<linearValues.size(); i++) {
			
			if(constRanges.size()>rangeId) {
				ConstRange range = constRanges.get(rangeId);
				if(range.getStart()==i) {
					//todo выполняем копирование блока по 
					cg.flashDataToArr(parent, range.getSymbol(), i*size);
					i=range.getEnd();
					rangeId++;
					continue;
				}
			}
			
			ExpressionNode expr = linearValues.get(i);
			//TODO проверить на VIEW(обяателне вызов runtime)
			//CGArrCells arrCells = new CGArrCells(i*size, size);
			CGArrCells arrCells = new CGArrCells(valueExprs.size());
			arrCells.setSize(0x02);
			
			if(expr instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr;
				boolean isFixed = le.isFixed() || VarType.FIXED == basedType;
				parent.append(cg.constToCells(parent, isFixed ? le.getFixedValue() : le.getNumValue(), arrCells, isFixed));
			}
			else {
				expr.codeGen(cg, parent, true);
				cg.accToArr(parent, arrCells);
			}
		}

		return null;
	}
	
	public int[] getDimensions() {
		return dimensions;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
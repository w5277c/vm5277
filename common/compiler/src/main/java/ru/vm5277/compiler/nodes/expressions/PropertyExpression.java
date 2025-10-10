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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.Property;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.Scope;

public class PropertyExpression extends ExpressionNode {
	private			ExpressionNode			nameExpr;
	private			Property				property;
	private			NewArrayExpression		nae;
	private			List<ExpressionNode>	args;
	private			VarType					exprType;
	private			VarType					type;
	
	public PropertyExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode nameExpr, Property property) throws CompileException {
		super(tb, mc);
		
		this.nameExpr = nameExpr;
		this.property = property;
		
		if(Property.ITEM==property || Property.INDEX==property || Property.SIZE==property) {
			args = parseArguments(tb);
		}
	}

	public PropertyExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode nameExpr, Property property, List<ExpressionNode> args)
																																	throws CompileException {
		super(tb, mc);
		
		this.nameExpr = nameExpr;
		this.property = property;
		this.args = args;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		if(null!=type) return type;
		
		if(nameExpr instanceof LiteralExpression) {
			if(Property.SIZE==property || Property.INDEX==property) {
				return VarType.BYTE;
			}
			else if(Property.ITEM==property) {
				String enumName = ((LiteralExpression)nameExpr).getStringValue();
				return VarType.fromEnumName(enumName);
			}
			else {
				throw new CompileException("Unsupported enum property: " + property);
			}
		}
		else {
			VarType varType = nameExpr.getType(scope);
			if(varType.isArray()) {
				if(Property.LENGTH == property) {
					return VarType.SHORT;
				}
				throw new CompileException("Unsupported array property: " + property);
			}
			else if(varType.isEnum()) {
				if(Property.INDEX == property || Property.SIZE == property) {
					return VarType.BYTE;
				}
				else if(Property.ITEM == property) {
					return varType;
				}
				throw new CompileException("Unsupported enum property: " + property);
			}
			throw new CompileException("Unknown property: " + property + " for expr: " + nameExpr);
		}
	}
	
	public ExpressionNode getNameExpr() {
		return nameExpr;
	}
	
	public Property getProperty() {
		return property;
	}
	
	public List<ExpressionNode> getArguments() {
		return args;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		result &= nameExpr.preAnalyze();
		
		if(result) {
			result &= nameExpr.preAnalyze();
			if(null!=args) {
				if(!args.isEmpty() && (Property.INDEX==property || Property.SIZE==property)) {
					markError("Property '" + property + "' does not accept arguments");
					result = false;
				}
				if(1!=args.size() && Property.ITEM==property) {
					markError("Property 'item' requires index argument");
					result = false;
				}
			}
			
			// EnumExpression - это выражение вида EStatus.OK, LiteralExpression - EStatus
			if(nameExpr instanceof EnumExpression && Property.SIZE==property) {
				markError("Cannot get size from enum value '" + ((EnumExpression)nameExpr).getName() + "' - use enum type instead");
				result = false;
			}
			if(nameExpr instanceof EnumExpression && Property.ITEM==property) {
				markError("Cannot get item from enum value '" + ((EnumExpression)nameExpr).getName() + "' - use enum type instead");
				result = false;
			}
			if(nameExpr instanceof LiteralExpression && Property.INDEX==property) {
				markError("Cannot get index from enum type '" + ((LiteralExpression)nameExpr).getStringValue() + "' - use enum value instead");
				result = false;
			}
			
			if(null!=args) {
				for(int i=0; i<args.size(); i++) {
					result &= args.get(i).preAnalyze();
				}
			}
		}
		
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		result &= nameExpr.declare(scope);

		if(null!=args) {
			for(int i=0; i<args.size(); i++) {
				result&=args.get(i).declare(scope);
			}
		}

		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());

		try {
			type = getType(scope);
			
			ExpressionNode optimizedParentScope = nameExpr.optimizeWithScope(scope, cg);
			if(null != optimizedParentScope) {
				nameExpr = optimizedParentScope;
			}
			
			result &= nameExpr.postAnalyze(scope, cg);
			
			if(nameExpr instanceof LiteralExpression) {
				String enumName = ((LiteralExpression)nameExpr).getStringValue();
				exprType = VarType.fromEnumName(enumName);
			}
			else {
				exprType = nameExpr.getType(scope);
			}
			if(!exprType.isArray() && !exprType.isEnum()) {
				markError("Unsupported expr: " + nameExpr);
				result = false;
			}

			if(exprType.isArray() && Property.LENGTH != property) {
				markError("Unsupported array property: " + property);
				result = false;
			}
			else if(exprType.isEnum() && Property.INDEX!=property && Property.SIZE!=property && Property.ITEM!=property) {
				markError("Unsupported enum property: " + property);
				result = false;
			}

			if(Property.ITEM==property && exprType.isEnum()) {
				if(VarType.BYTE!=args.get(0).getType(scope)) {
					markError("Enum index must be byte value, but found: " + args.get(0).getType(scope));
					result = false;
				}
			}

			
			if(nameExpr.getSymbol() instanceof AstHolder) {
				AstHolder ah = (AstHolder)nameExpr.getSymbol();
				if(ah.getNode() instanceof InitNodeHolder) {
					InitNodeHolder inh = (InitNodeHolder)ah.getNode();
					if(inh.getInitNode() instanceof NewArrayExpression) {
						nae = (NewArrayExpression)inh.getInitNode();
					}
				}
			}
		}
		catch (CompileException e) {
            markError(e.getMessage());
            result = false;
        }
		
		if(null==nae && Property.LENGTH == property) {
			markError("Cannot determine array length: array initialization not found");
			result = false;
		}
		
		if(null!=args) {
			for(int i=0; i<args.size(); i++) {
				result &= args.get(i).postAnalyze(scope, cg);
			}
			
			if(Property.ITEM==property && args.get(0) instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)args.get(0);
				if(!le.isInteger()) {
					markError("Enum index must be integer value");
					result = false;
				}
				else {
					long index = le.getNumValue();
					if(0>index || index>=exprType.getEnumValues().size()) {
						markError("Enum index out of bounds: " + index + ", valid range: 0-" + (exprType.getEnumValues().size()-1));
						result = false;
					}
				}
			}
		}
		
		cg.leaveExpression();
		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		// У этого выражения нет дочерних узлов в AST
		return Arrays.asList();
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		CodegenResult result = null;
		
		CGScope cgs = (null==parent ? cgScope : parent);
		
		nameExpr.codeGen(cg, null, false);
		
		if(exprType.isArray()) {
			//TODO нужно проверить!
			boolean isView = false;
			AstHolder ah = (AstHolder)nameExpr.getSymbol();
			if(ah.getNode() instanceof InitNodeHolder) {
				InitNodeHolder inh = (InitNodeHolder)ah.getNode();
				if(inh.getInitNode() instanceof ArrayExpression) {
					isView = true;
				}
			}

			if(toAccum) {
				if(Property.LENGTH == property) {
					if(!isView && null!=nae.getConstDimensions()) {
						cg.constToAcc(cgs, 0x02, nae.getConstDimensions()[0x00], false);
					}
					else {
						CGCellsScope cScope = (CGCellsScope)nameExpr.getSymbol().getCGScope();
						cg.cellsToArrReg(cgs, cScope.getCells());
						cg.arrSizetoAcc(cgs, isView);
					}
				}
				return CodegenResult.RESULT_IN_ACCUM;
			}
		}
		else if(exprType.isEnum()) {
			if(toAccum) {
				if(Property.SIZE == property) {
					cg.constToAcc(cgs, 0x01, exprType.getEnumValues().size(), false);
				}
				else if(Property.INDEX == property) {
					if(nameExpr instanceof EnumExpression) {
						cg.constToAcc(cgs, 0x01, ((EnumExpression)nameExpr).getIndex(), false);
					}
					else {
						nameExpr.codeGen(cg, cgs, true);
					}
				}
				else { //ITEM
					args.get(0).codeGen(cg, cgs, true);
				}
				return CodegenResult.RESULT_IN_ACCUM;
			}
		}

		return result;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + nameExpr.toString() + "." + property;
	}
}

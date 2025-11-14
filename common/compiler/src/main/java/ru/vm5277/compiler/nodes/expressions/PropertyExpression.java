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
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.EnumScope;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.Scope;

public class PropertyExpression extends ExpressionNode {
	private			ExpressionNode			targetExpr;
	private			Property				property;
	private			NewArrayExpression		nae;
	private			List<ExpressionNode>	args;
	private			EnumScope				enumScope;
	
	public PropertyExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode targetExpr, Property property, List<ExpressionNode> args) {
		super(tb, mc, sp);
		
		this.targetExpr = targetExpr;
		this.property = property;
		
		this.args = args;
	}

	public ExpressionNode getTargetExpr() {
		return targetExpr;
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
		debugAST(this, PRE, true, getFullInfo());
		
		result&=targetExpr.preAnalyze();
		
		if(result) {
			result&=targetExpr.preAnalyze();
			if(null!=args) {
				if(!args.isEmpty() && (Property.index==property || Property.size==property)) {
					markError("Property '" + property + "' does not accept arguments");
					result = false;
				}
				if(1!=args.size() && Property.item==property) {
					markError("Property 'item' requires index argument");
					result = false;
				}
			}
			
			// EnumExpression - это выражение вида EStatus.OK, LiteralExpression - EStatus
			if(targetExpr instanceof EnumExpression && Property.size==property) {
				markError("Cannot get size from enum value '" + ((EnumExpression)targetExpr).toString() + "' - use enum type instead");
				result = false;
			}
			if(targetExpr instanceof EnumExpression && Property.item==property) {
				markError("Cannot get item from enum value '" + ((EnumExpression)targetExpr).toString() + "' - use enum type instead");
				result = false;
			}
			if(targetExpr instanceof LiteralExpression && Property.index==property) {
				markError("Cannot get index from enum type '" + ((LiteralExpression)targetExpr).getStringValue() + "' - use enum value instead");
				result = false;
			}
			
			if(null!=args) {
				for(int i=0; i<args.size(); i++) {
					result&=args.get(i).preAnalyze();
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
		
		result&=targetExpr.declare(scope);
		
		if(null!=args) {
			for(int i=0; i<args.size(); i++) {
				result&=args.get(i).declare(scope);
			}
		}
		
//		type = targetExpr.getType();
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());

		try {
			ExpressionNode optimizedParentScope = targetExpr.optimizeWithScope(scope, cg);
			if(null!=optimizedParentScope) {
				targetExpr = optimizedParentScope;
			}
			result&=targetExpr.postAnalyze(scope, cg);

			for(int i=0; i<args.size(); i++) {
				ExpressionNode arg = args.get(i);
				result&=arg.postAnalyze(scope, cg);
				if(result) {
					ExpressionNode optimizedExpr = arg.optimizeWithScope(scope, cg);
					if(null!=optimizedExpr) {
						args.set(i, optimizedExpr);
						arg = optimizedExpr;
					}
				}
			}
			
			if(targetExpr instanceof TypeReferenceExpression) {
				TypeReferenceExpression tre = (TypeReferenceExpression)targetExpr;
				enumScope = (EnumScope)((TypeReferenceExpression)targetExpr).getScope();
				if(args.isEmpty() && Property.size==property) {
					type = VarType.BYTE;
				}
				else if(0x01==args.size() && Property.item==property) {
					type = tre.getType();
					ExpressionNode argExpr = args.get(0);
					if(VarType.BYTE!=argExpr.getType()) {
						markError("Enum index must be byte value, but found: " + argExpr.getType());
						result = false;
					}
					
					if(argExpr instanceof LiteralExpression) {
						LiteralExpression le = (LiteralExpression)argExpr;
						long index = le.getNumValue();
						int size = enumScope.getSize();
						if(0>index || index>=size) {
							markError("Enum index out of bounds: " + index + ", valid range: 0-" + (size-1));
							result = false;
						}
					}
				}
				else {
					if(args.isEmpty()) {
						markError("Unsupported enum property:" + property.name() + "()");
					}
					else {
						markError("Unsupported enum property:" + property.name() + "(" + StrUtils.toString(args) + ")");
					}
					result = false;
				}
			}
			else if(targetExpr instanceof EnumExpression) {
				EnumExpression ee = (EnumExpression)targetExpr;
				enumScope = (EnumScope)ee.getTargetScope();
				if(args.isEmpty() && Property.index==property) {
					type = VarType.BYTE;
				}
				else {
					if(args.isEmpty()) {
						markError("Unsupported enum property:" + property.name() + "()");
					}
					else {
						markError("Unsupported enum property:" + property.name() + "(" + StrUtils.toString(args) + ")");
					}
					result = false;
				}
			}
			else if(targetExpr instanceof ArrayExpression) {
				type = targetExpr.getType();
				if(args.isEmpty() && Property.length==property) {
					if(targetExpr.getSymbol() instanceof AstHolder) {
						AstHolder ah = (AstHolder)targetExpr.getSymbol();
						if(ah.getNode() instanceof InitNodeHolder) {
							InitNodeHolder inh = (InitNodeHolder)ah.getNode();
							if(inh.getInitNode() instanceof NewArrayExpression) {
								nae = (NewArrayExpression)inh.getInitNode();
							}
						}
					}
					
					if(null==nae && Property.length == property) {
						markError("Cannot determine array length: array initialization not found");
						result = false;
					}
				}
				else {
					if(args.isEmpty()) {
						markError("Unsupported array property:" + property.name() + "()");
					}
					else {
						markError("Unsupported array property:" + property.name() + "(" + StrUtils.toString(args) + ")");
					}
					result = false;
				}
			}
			else if(targetExpr instanceof VarFieldExpression) {
				if(targetExpr.getType().isEnum()) {
					if(args.isEmpty() && Property.index==property) {
						type = VarType.BYTE;
					}
					else {
						if(args.isEmpty()) {
							markError("Unsupported enum property:" + property.name() + "()");
						}
						else {
							markError("Unsupported enum property:" + property.name() + "(" + StrUtils.toString(args) + ")");
						}
						result = false;
					}
				}
				else if(targetExpr.getType().isClassType()) {
					if(args.isEmpty()) {
						if(Property.instanceId==property) {
							type = VarType.SHORT;
						}
						else if(Property.typeId==property) {
							//TODO для refsize = 1
							type = VarType.BYTE;
						}
						else {
							markError("Unsupported class property:" + property.name() + "()");
							result = false;
						}
					}
					else {
						markError("Unsupported class property:" + property.name() + "(" + StrUtils.toString(args) + ")");
						result = false;
					}
				}
				else if(targetExpr.getType().isArray()) {
					if(args.isEmpty()) {
						if(Property.length==property) {
							type = VarType.SHORT;

							if(targetExpr.getSymbol() instanceof AstHolder) {
								AstHolder ah = (AstHolder)targetExpr.getSymbol();
								if(ah.getNode() instanceof InitNodeHolder) {
									InitNodeHolder inh = (InitNodeHolder)ah.getNode();
									if(inh.getInitNode() instanceof NewArrayExpression) {
										nae = (NewArrayExpression)inh.getInitNode();
									}
								}
							}
						}
						else {
							markError("Unsupported array property:" + property.name() + "()");
							result = false;
						}
					}
					else {
						markError("Unsupported array property:" + property.name() + "(" + StrUtils.toString(args) + ")");
						result = false;
					}
				}
				else {
					markError("Unsupported init expr in var/field: " + targetExpr);
					result = false;
				}
			}
			else if(targetExpr instanceof PropertyExpression) {
				if(targetExpr.getType().isEnum()) {
					if(args.isEmpty() && Property.index==property) {
						type = VarType.BYTE;
					}
					else {
						if(args.isEmpty()) {
							markError("Unsupported enum property:" + property.name() + "()");
						}
						else {
							markError("Unsupported enum property:" + property.name() + "(" + StrUtils.toString(args) + ")");
						}
						result = false;
					}
				}
				else {
					markError("Unsupported init expr in var/field: " + targetExpr);
				}
			}
			else {
				markError("Unsupported property target expr: " + targetExpr);
				result = false;
			}
		}
		catch (CompileException e) {
            markError(e.getMessage());
            result = false;
        }
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		// У этого выражения нет дочерних узлов в AST
		return Arrays.asList();
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		CodegenResult result = null;
		
		CGScope cgs = (null==parent ? cgScope : parent);
		
		targetExpr.codeGen(cg, null, false);
		
		if(null==enumScope && !targetExpr.getType().isEnum() && !targetExpr.getType().isClassType()) {
			//TODO нужно проверить!
			boolean isView = false;
			AstHolder ah = (AstHolder)targetExpr.getSymbol();
			if(ah.getNode() instanceof InitNodeHolder) {
				InitNodeHolder inh = (InitNodeHolder)ah.getNode();
				if(inh.getInitNode() instanceof ArrayExpression) {
					isView = true;
				}
			}

			if(toAccum) {
				if(Property.length == property) {
					if(!isView && null!=nae.getConstDimensions()) {
						cg.constToAcc(cgs, 0x02, nae.getConstDimensions()[0x00], false);
					}
					else {
						CGCellsScope cScope = (CGCellsScope)targetExpr.getSymbol().getCGScope();
						cg.cellsToArrReg(cgs, cScope.getCells());
						cg.arrSizetoAcc(cgs, isView);
					}
				}
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}
		else if(targetExpr.getType().isClassType() && !targetExpr.getType().isEnum()) {
			if(Property.instanceId==property) {
				cg.cellsToAcc(cgs, (CGCellsScope)targetExpr.getSymbol().getCGScope());
				result = CodegenResult.RESULT_IN_ACCUM;
			}
			else if(Property.typeId==property) {
				cg.constToAcc(cgs, 0x01, targetExpr.getType().getId(), false);
				result = CodegenResult.RESULT_IN_ACCUM;
			}
			else {
				throw new CompileException("Unsupported propert:'" + property + "' for '" + targetExpr + "'");
			}
		}
		else {
			if(toAccum) {
				if(Property.size==property) {
					cg.constToAcc(cgs, 0x01, enumScope.getSize(), false);
				}
				else if(Property.index == property) {
					if(targetExpr instanceof EnumExpression) {
						cg.constToAcc(cgs, 0x01, ((EnumExpression)targetExpr).getIndex(), false);
					}
					else {
						targetExpr.codeGen(cg, cgs, true);
					}
				}
				else { //ITEM
					args.get(0).codeGen(cg, cgs, true);
				}
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}

		return result;
	}
	
	@Override
	public String toString() {
		return targetExpr + "." + property;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}

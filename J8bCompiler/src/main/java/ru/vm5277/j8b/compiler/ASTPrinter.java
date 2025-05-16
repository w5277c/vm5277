/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.nodes.ArrayDeclarationNode;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.BlockNode;
import ru.vm5277.j8b.compiler.nodes.ClassBlockNode;
import ru.vm5277.j8b.compiler.nodes.ClassNode;
import ru.vm5277.j8b.compiler.nodes.FieldNode;
import ru.vm5277.j8b.compiler.nodes.InterfaceBodyNode;
import ru.vm5277.j8b.compiler.nodes.InterfaceNode;
import ru.vm5277.j8b.compiler.nodes.LabelNode;
import ru.vm5277.j8b.compiler.nodes.MethodNode;
import ru.vm5277.j8b.compiler.nodes.ParameterNode;
import ru.vm5277.j8b.compiler.nodes.VarNode;
import ru.vm5277.j8b.compiler.nodes.commands.BreakNode;
import ru.vm5277.j8b.compiler.nodes.commands.CommandNode.Case;
import ru.vm5277.j8b.compiler.nodes.commands.ContinueNode;
import ru.vm5277.j8b.compiler.nodes.commands.DoWhileNode;
import ru.vm5277.j8b.compiler.nodes.commands.ForNode;
import ru.vm5277.j8b.compiler.nodes.commands.IfNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.commands.SwitchNode;
import ru.vm5277.j8b.compiler.nodes.commands.ThrowNode;
import ru.vm5277.j8b.compiler.nodes.commands.TryNode;
import ru.vm5277.j8b.compiler.nodes.commands.WhileNode;
import ru.vm5277.j8b.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.NewExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.TernaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.TypeReferenceExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.VariableExpression;
import ru.vm5277.j8b.compiler.tokens.Token;

public class ASTPrinter {
	private class Printer {
		private	StringBuilder	sb		= new StringBuilder();
		private	StringBuilder	spacer	= new StringBuilder();
		
		public void put(String str) {
			sb.append(str);
		}
		
		public void extend() {
			spacer.append("    ");
		}
		public void reduce() {
			spacer.delete(spacer.length()-4, spacer.length());
		}
		public void removeLast(int qnt) {
			sb.delete(sb.length()-qnt, sb.length());
		}
		public void print() {
			System.out.print(spacer.toString());
			printLeft();
		}
		public void printLeft() {
			System.out.println(sb.toString());
			sb = new StringBuilder();
		}
	}
	
	private	Printer	out	= new Printer();
	
	public ASTPrinter(ClassNode clazz) {
		printClass(clazz);
	}

	void printClass(ClassNode clazz) {
		printModifiers(clazz.getModifiers());
		out.put("class " + clazz.getName() + " ");
		ClassBlockNode cbn = clazz.getBody();
		if(null != cbn) {
			out.put("{"); out.print(); out.extend();
			for(AstNode node : cbn.getDeclarations()) {
				if(node instanceof ClassNode) {
					printClass((ClassNode)node);
				}
				else if(node instanceof InterfaceNode) {
					printInterface((InterfaceNode)node);
				}
				else if(node instanceof MethodNode) {
					printMethod((MethodNode)node);
				}
//				else if(node instanceof ArrayDeclarationNode) {
//					//printArray((ArrayDeclarationNode)node);
//				}
				else if(node instanceof FieldNode) {
					printField((FieldNode)node); out.put(";");out.print();
				}
				else if(node instanceof VarNode) {
					printVar((VarNode)node); out.put(";");out.print();
				}
				else if(node instanceof BlockNode) {
					printBody((BlockNode)node); out.print();
				}
				else {
					out.put("!unknown node:" + node); out.print();
				}

			}
			out.reduce();
			out.put("}");
		}
		out.print();
	}
	
	void printInterface(InterfaceNode iface) {
		printModifiers(iface.getModifiers());
		out.put("interface " + iface.getName() + " ");
		InterfaceBodyNode ibn = iface.getBody();
		if(null != ibn) {
			out.put("{"); out.print(); out.extend();
			for(AstNode node : ibn.getDeclarations()) {
				if(node instanceof MethodNode) {
					printMethod((MethodNode)node);
				}
//				else if(node instanceof ArrayDeclarationNode) {
//					//printArray((ArrayDeclarationNode)node);
//				}
				else if(node instanceof FieldNode) {
					printField((FieldNode)node); out.put(";");out.print();
				}
				else if(node instanceof BlockNode) {
					printBody((BlockNode)node); out.print();
				}
				else {
					out.put("!unknown node:" + node); out.print();
				}
			}
			out.reduce();
			out.put("}");
		}
		out.print();
	}

	void printModifiers(Set<Keyword> modifiers) {
		for(Keyword kw : modifiers) {
			out.put(kw.toString().toLowerCase() + " ");
		}
	}
	
	void printMethod(MethodNode method) {
		printModifiers(method.getModifiers());
		if(!method.isConstructor()) {
			out.put(method.getReturnType() + " ");
		}
		out.put(method.getName() + "(");
		printParameters(method.getParameters());
		out.put(") ");
		if(method.canThrow()) {
			out.put("throws ");
		}
		BlockNode body = method.getBody();
		if(null == body) {
			out.put(";");
			out.print();
		}
		else {
			printBody(body);
			out.print();
		}
	}
	
	void printParameters(List<ParameterNode> parameters) {
		if(null != parameters && !parameters.isEmpty()) {
			for(ParameterNode parameter : parameters) {
				out.put(parameter.getType() + " ");
				out.put(parameter.getName() + ", ");
			}
			out.removeLast(2);
		}
	}
	
	void printArguments(List<ExpressionNode> arguments) {
		if(null != arguments && !arguments.isEmpty()) {
			for(ExpressionNode arg : arguments) {
				printExpr(arg); out.put(", ");
			}
			out.removeLast(2);
		}
	}

	
	void printBody(BlockNode body)  {
		out.put("{"); out.print(); out.extend();
		for(AstNode node : body.getDeclarations()) {
			if(node instanceof LabelNode) {
				out.put(((LabelNode)node).getName() + ":");
				continue;
			}
			else if(node instanceof BreakNode) {
				BreakNode bn = (BreakNode)node;
				out.put("break");
				if(null != bn.getLabel()) out.put(" " + bn.getLabel());
				out.put(";");
			}
			else if(node instanceof ContinueNode) {
				out.put("continue;");
			}
			else if(node instanceof DoWhileNode) {
				DoWhileNode dwn = (DoWhileNode)node;
				out.put("do ");
				printBody(dwn.getBody());
				out.put(" while(");
				printExpr(dwn.getCondition());
				out.put(");"); out.print();
			}
			else if(node instanceof ForNode) {
				ForNode fn = (ForNode)node;
				out.put("for (");
				if(fn.getInitialization() instanceof FieldNode) {
					printField((FieldNode)fn.getInitialization());
				}
				else {
					printExpr((ExpressionNode)fn.getInitialization());
				}
			}
			else if(node instanceof IfNode) {
				IfNode in = (IfNode)node;
				out.put("if (");
				printExpr(in.getCondition());
				if(null != in.getVarName()) {
					out.put(" as " + in.getVarName());
				}
				out.put(") ");
				printBody(in.getThenBlock());
				if(null != in.getElseBlock()) {
					out.print();
					out.put("else ");
					printBody(in.getElseBlock());
				}
			}
			else if(node instanceof ReturnNode) {
				ReturnNode rn = (ReturnNode)node;
				out.put("return");
				if(null != rn.getExpression()) {
					out.put(" ");
					printExpr(rn.getExpression());
				}
				out.put(";");
			}
			else if(node instanceof SwitchNode) {
				SwitchNode sn = (SwitchNode)node;
				out.put("switch (");
				printExpr(sn.getExpression());
				out.put(") {"); out.extend(); out.print();
				for(SwitchNode.Case c : sn.getCases()) {
					out.put("case " + c.getFrom());
					if(-1 != c.getTo()) {
						out.put(".." + c.getTo());
					}
					out.put(": ");
					out.extend(); out.print();
					printBody(c.getBlock());
					out.reduce(); out.print();
				}
				out.reduce(); out.put("}");
			}
			else if(node instanceof WhileNode) {
				WhileNode wn = (WhileNode)node;
				out.put("while (");
				printExpr(wn.getCondition());
				out.put(") ");
				printBody(wn.getBody());
				out.print();
			}
			else if(node instanceof ClassNode) {
				printClass((ClassNode)node);
			}
			else if(node instanceof ArrayDeclarationNode) {
				//TODO
			}
			else if(node instanceof FieldNode) {
				printField((FieldNode)node);
				out.put(";");
			}
			else if(node instanceof VarNode) {
				printVar((VarNode)node);
				out.put(";");
			}
			else if(node instanceof ExpressionNode) {
				printExpr((ExpressionNode)node);
				out.put(";");
			}
			else if(node instanceof BlockNode) {
				printBody((BlockNode)node);
				out.print();
			}
			else if(node instanceof InterfaceNode) {
				printInterface((InterfaceNode)node);
			}
			else if(node instanceof TryNode) {
				TryNode tryNode = (TryNode)node;
				out.put("try ");
				printBody(tryNode.getTryBlock());
				if(!tryNode.getCatchCases().isEmpty() || null != tryNode.getCatchDefault()) {
					out.print();
					out.put("catch (byte ");
					out.put(tryNode.getVarName());
					out.put(") ");
					if(tryNode.getCatchCases().isEmpty()) {
						printBody(tryNode.getCatchDefault());
					}
					else {
						out.put("{");
						out.print();
						out.extend();
						for(Case c : tryNode.getCatchCases()) {
							out.put("case " + c.getFrom());
							if(-1 != c.getTo()) {
								out.put(".." + c.getTo());
							}
							out.put(": ");
							printBody(c.getBlock());
							out.print();
						}
						if(null != tryNode.getCatchDefault()) {
							out.put("default: ");
							printBody(tryNode.getCatchDefault());
						}
						out.print();
						out.reduce();
						out.put("}");
					}
				}
				
			}
			else if(node instanceof ThrowNode) {
				out.put("throw ");
				printExpr(((ThrowNode)node).getExceptionExpr());
				out.put(";");
			}
			else {
				out.put("!unknown node:" + node); out.print();
			}
			out.print();
		}
		out.reduce(); out.put("}");
	}
	
	void printField(FieldNode node) {
		if(null != node.getType()) {
			out.put(node.getType() + " ");
		}
		out.put(node.getName());
		if(null != node.getInitializer()) {
			out.put(" = ");
			printExpr(node.getInitializer());
		}
	}
	
	void printVar(VarNode node) {
		if(null != node.getType()) {
			out.put(node.getType() + " ");
		}
		out.put(node.getName());
		if(null != node.getInitializer()) {
			out.put(" = ");
			printExpr(node.getInitializer());
		}
	}

	void printExpr(ExpressionNode expr) {
		if(expr instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression)expr;
			printOperand(be.getLeft(), be.getOperator(), true);
			out.put(be.getOperator().getSymbol());
			printOperand(be.getRight(), be.getOperator(), false);
		}
		else if (expr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr;
			out.put(le.isCstr() ? "\"" + Token.toStringValue(le.getValue()) + "\"" : Token.toStringValue(le.getValue()));
		}
		else if(expr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)expr;
			if(null != mce.getParent()) {
				printExpr(mce.getParent());
				out.put(".");
			}
			out.put(mce.getMethodName() + "(");
			printArguments(mce.getArguments());
			out.put(")");
		}
		else if(expr instanceof VariableExpression) {
			out.put(((VariableExpression)expr).getValue());
		}
		else if(expr instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression)expr;
			Operator op = ue.getOperator();
			if(op.isPostfix()) {
				if(null != ue.getOperand()) {
					printExpr(ue.getOperand());
				}
				out.put(ue.getOperator().getSymbol());
			}
			else {
				out.put(ue.getOperator().getSymbol());
				if(null != ue.getOperand()) {
					printExpr(ue.getOperand());
				}
			}
		}
		else if(expr instanceof TernaryExpression) {
			TernaryExpression te = (TernaryExpression)expr;
			printExpr(te.getCondition());
			out.put(" ? ");
			printExpr(te.getTrueExpr());
			out.put(" : ");
			printExpr(te.getFalseExpr());
		}
		else if(expr instanceof NewExpression) {
			NewExpression ne = (NewExpression)expr;
			out.put("new ");
			out.put(ne.getName());
			out.put("(");
			printArguments(ne.getArgs());
			out.put(")");
		}
		else if(expr instanceof InstanceOfExpression) {
			InstanceOfExpression ie = (InstanceOfExpression)expr;
			printExpr(ie.getLeft());
			out.put(" is ");
			printExpr(ie.getTypeExpr());
		}
		else if(expr instanceof TypeReferenceExpression) {
			TypeReferenceExpression te = (TypeReferenceExpression)expr;
			out.put(te.getClassName());
		}
		else {
			out.put("!unknown expr:" + expr); out.print();
		}
	}
	
	void printOperand(ExpressionNode operand, Operator parentOp, boolean isLeft) {
		boolean needParentheses = false;

		if (operand instanceof BinaryExpression) {
			Operator childOp = ((BinaryExpression) operand).getOperator();
			int parentPriority = Operator.PRECEDENCE.get(parentOp);
			int childPriority = Operator.PRECEDENCE.get(childOp);

			needParentheses = childPriority < parentPriority || (childPriority == parentPriority && !isLeft);
		}

		if (needParentheses) out.put("(");
		printExpr(operand);
		if (needParentheses) out.put(")");
	}
}

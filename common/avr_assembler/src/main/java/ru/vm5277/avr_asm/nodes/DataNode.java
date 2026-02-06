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
package ru.vm5277.avr_asm.nodes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.Assembler;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.avr_asm.semantic.IdExpression;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class DataNode extends Node {
	private int						valueSize;
	private	Map<Integer, String>	unresolvedLabels	= new HashMap<>();
	
	public DataNode(TokenBuffer tb, Scope scope, MessageContainer mc, int valueSize) throws CompileException {
		super(tb, scope, mc);
		
		this.valueSize = valueSize;
		this.scope = scope;
		this.mc = mc;
		this.sp = tb.getSP();

		StringBuilder listSB = null;
		if(scope.isListEnabled()) {
			listSB = new StringBuilder();
			switch(valueSize) {
				case 1: listSB.append(".DB "); break;
				case 2: listSB.append(".DW "); break;
				case 4: listSB.append(".DD "); break;
				case 8: listSB.append(".DQ "); break;
			}
		}
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			while(true) {
				SourcePosition _sp = tb.getSP();
				byte[] tmp = null;
				Expression expr = Expression.parse(tb, scope, mc);
				if(expr instanceof LiteralExpression && ((LiteralExpression)expr).getValue() instanceof String) {
					tmp = ((String)((LiteralExpression)expr).getValue()).getBytes(StandardCharsets.US_ASCII);
					if(scope.isListEnabled()) {
						for(int offset=0; offset<tmp.length;) {
							for(int i=0; i<valueSize && offset+i<tmp.length; i++) {
								listSB.append(tmp[offset+i]&0xff);
							}
							offset += valueSize;
							listSB.append(",");
						}
					}
				}
				else {
					Long value = Expression.getLong(expr, tb.getSP());
					if(null!=value) {
						if(scope.isListEnabled()) listSB.append(value).append(",");
					}
					else {
						// Похоже в выражении метка
						if(expr instanceof IdExpression) {
							if(scope.isListEnabled()) listSB.append(((IdExpression)expr).getId()).append(",");
							
							Integer _value = scope.resolveLabel(((IdExpression)expr).getId());
							if(null != _value) {
								value = _value.longValue();
							}
							else {
								value = 0x00l;
								// Адрес метки еще не известен, оставляем для second pass
								unresolvedLabels.put(scope.getCSeg().getPC()+(baos.size()/2), ((IdExpression)expr).getId());
							}
						}
						else {
							tb.skipLine();
							throw new CompileException("Cannot resolve expression: '" + expr + "'", _sp);
						}
					}
					if(0x08!=valueSize && value>=(1L<<(valueSize*8))) {
						tb.skipLine();
						throw new CompileException("Value " + value + " exceeds " + valueSize*8 + "-bit range", _sp);
					}
					tmp = new byte[valueSize];
					for(int i=0; i<valueSize; i++) {
						tmp[i] = (byte)(value & 0xff);
						value >>>= 8;
					}
				}

				baos.write(tmp);
				
				if(tb.match(TokenType.NEWLINE) || tb.match(TokenType.EOF)) break;
				Node.consumeToken(tb, Delimiter.COMMA);
			}
			
			if(0 != baos.size()) {
				if(0 != (baos.size()&0x01)) {
					if(Assembler.STRICT_STRONG == Scope.getStrincLevel()) {
						throw new CompileException("Odd-sized FLASH data", sp);
					}
					else if(Assembler.STRICT_LIGHT == Scope.getStrincLevel()) {
						mc.add(new WarningMessage("Odd-sized FLASH data", sp));
					}
					baos.write(0x00);
				}
				int wSize = baos.size()/2;
				scope.getCSeg().getCurrentBlock().write(baos.toByteArray(), wSize);
				scope.getCSeg().movePC(wSize);
			
				if(scope.isListEnabled()) {
					listSB.deleteCharAt(listSB.length()-1);
					scope.list(listSB.toString());
				}
			}
			Node.consumeToken(tb, TokenType.NEWLINE);
		}
		catch(Exception e) {
			throw new CompileException(e.getMessage(), sp);
		}
	}

	public void secondPass()  {
		if(!unresolvedLabels.isEmpty()) {
			for(Integer wAddr : unresolvedLabels.keySet()) {
				String labelName = unresolvedLabels.get(wAddr);
				Integer value = scope.resolveLabel(labelName);
				if(null == value) {
					markError(new CompileException("Cannot resolve label: '" + labelName + "'", sp));
				}
				else {
					byte[] tmp = new byte[valueSize];
					for(int i=0; i<valueSize; i++) {
						tmp[i] = (byte)(value & 0xff);
						value >>>= 8;
					}
					try {
						scope.getCSeg().setPC(wAddr);
						scope.getCSeg().getCurrentBlock().write(tmp, valueSize/2);
					}
					catch(Exception e) {
						markError(new CompileException(e.getMessage(), sp));
					}
				}
			}
		}
	}
}

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
package ru.vm5277.avr_asm.scope;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.avr_asm.Assembler;
import ru.vm5277.avr_asm.InstrReader;
import static ru.vm5277.avr_asm.Assembler.tabSize;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.ExternalTokenProvider;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class Scope {
	protected	final	static	Map<String, Byte>			registers		= new HashMap<>();
	static {
		for(byte i=0; i<32; i++) {
			if(i<10) registers.put("r0"+i, i);
			registers.put("r"+i, i);
		}
	}

	private			static	String									mcu				= null;
	private			static	InstrReader								instrReader;
	private	final			BufferedWriter							listWriter;
	private					boolean									listEnabled		= true;
	private					boolean									listMacEnabled	= true;
	private					boolean									overlapAllowed	= true;
	private			static	int										strictLevel		= Assembler.STRICT_LIGHT;
	private	final			Map<String, Byte>						regAliases		= new HashMap<>();	// Алиасы регистров
	private	final			Map<String, VariableSymbol>				variables		= new HashMap<>();
	private	final			Map<String, Integer>					labels			= new HashMap<>();
	private	final			Map<String, MacroDefSymbol>				macros			= new HashMap<>();
	private final			Deque<MacroCallSymbol>					macroDeploys	= new ArrayDeque<>();
	private final			Deque<MacroCallSymbol>					macroSecondPass	= new ArrayDeque<>();
	
	private					CodeSegment								cSeg;
	private					List<Node>								dSeg;
	private					List<Node>								eSeg;
	private					MacroDefSymbol							currentMacroDef	= null;
	private final			Set<String>								includePaths	= new HashSet<>();
	private					IncludeSymbol							currentInclude	= null;
	private					ExternalTokenProvider					tokenProvider;
	
	public Scope(File sourceFile, InstrReader instrReader, BufferedWriter listWriter) throws CompileException {
		this.instrReader = instrReader;
		addImport(sourceFile.getAbsolutePath());
		this.listWriter = listWriter;
		
		
		tokenProvider = new ExternalTokenProvider() {
			@Override
			public Token getExternalToken(SourceBuffer sb, SourcePosition sp, String str) {
				if(null!=instrReader.getInstrById().get(str) || null!=instrReader.getInstrByMn().get(str)) {
					return new Token(sb, sp, TokenType.MNEMONIC, str.toLowerCase());
				}
				// Проверка на индексные регистры X+,Y+,Z+
				else if(str.equals("x") || str.equals("y") || str.equals("z")) {
					if(sb.available() && '+'==sb.peek()) {
						sb.next();
						return new Token(sb, sp, TokenType.INDEX_REG, str+"+");
					}
					else {
						return new Token(sb, sp, TokenType.INDEX_REG, str);
					}
				}
				return null;
			}
		};
	}
	
	public void setDevice(String device) throws CompileException {
		if(null!=this.mcu) {
			throw new CompileException("Device is already set", null);
		}
		this.mcu = device.toLowerCase();
		instrReader.setMCU(this.mcu);
	}
	
	public boolean addImport(String fullPath) throws CompileException {
		if(null!=currentMacroDef || isMacroDeploy()) {
			throw new CompileException("Imports not allowed inside macro definitions", null);
		}
		
		if(!includePaths.add(fullPath)) return false;
		
		currentInclude = new IncludeSymbol(fullPath, currentInclude);
		return true;
	}
	public void leaveImport(SourcePosition sp) throws CriticalParseException, CompileException {
		IncludeSymbol cur = currentInclude;
		currentInclude = currentInclude.getParent();
		if(0 != cur.getBlockCntr()) {
			throw new CompileException("Unbalanced conditional blocks (expected " + cur.getBlockCntr() + " more END directives)", sp);
		}
	}
	
	public int addLabel(String name, SourcePosition sp) throws CompileException {
		String lcName = name.toLowerCase();
		if(null==cSeg) throw new CompileException("FLASH access detected but no .ORG directive was encountered for label '" + name);
		if(labels.keySet().contains(lcName)) throw new CompileException("Label '" + name + "' already defined", sp);
		if(null != registers.get(lcName)) throw new CompileException("Label name conflicts with a register:" + name, sp);
		if(regAliases.keySet().contains(lcName)) {
			throw new CompileException("Label name conflicts with a register alias:" + name, sp);
		}
		if(lcName.equals("pc") || variables.keySet().contains(lcName)) throw new CompileException("Symbol '" + name + "' already defined as variable", sp);
		
		int addr = cSeg.getPC();
		if(isMacroDeploy()) {
			MacroCallSymbol symbol = macroDeploys.getLast();
			symbol.addLabel(lcName, sp, addr);
		}
		else {
			labels.put(lcName, addr);
		}
		return addr;
	}
	
	public void setVariable(VariableSymbol variableSymbol, SourcePosition sp) throws CompileException {
		String varName = variableSymbol.getName();
		String lcName = varName.toLowerCase();
		if(lcName.equals("pc")) throw new CompileException("Register PC cannot be used as a variable name", sp);
		if(null != registers.get(lcName)) throw new CompileException("Variable name conflicts with a register:" + varName, sp);
		if(regAliases.keySet().contains(lcName)) throw new CompileException("Variable name conflicts with a register alias:" + varName, sp);
		VariableSymbol vs = variables.get(lcName);
		if(null!=vs && vs.isConstant()) {
			throw new CompileException("Cannot overwrite the value of a constant:" + varName, sp);
		}
		if(isMacroDeploy()) {
			MacroCallSymbol symbol = macroDeploys.getLast();
			symbol.addVariable(variableSymbol, sp);
		}
		else {
			variables.put(lcName, variableSymbol);
		}
	}

	public VariableSymbol resolveVariable(String name) throws CompileException {
		String lcName = name.toLowerCase();
		if(lcName.equals("pc")) {
			if(null==cSeg) throw new CompileException("Cannot reference PC register: no active code segment defined");
			return new VariableSymbol(lcName, cSeg.getPC()-1, true); //TODO Костыль?
		}
		if(isMacroSecondPass()) {
			Iterator<MacroCallSymbol> it = macroDeploys.descendingIterator();
			while(it.hasNext()) {
				VariableSymbol result = it.next().resolveVariable(lcName);
				if(null!=result) return result;
			}
			it = macroSecondPass.descendingIterator();
			while(it.hasNext()) {
				VariableSymbol result = it.next().resolveVariable(lcName);
				if(null!=result) return result;
			}
		}
		return variables.get(lcName);
	}

	public Byte resolveReg(String name) {
		String lcName = name.toLowerCase();
		
		Byte result = registers.get(lcName);
		if(null == result) {
			result = regAliases.get(lcName);
		}
		return result;
	}
	
	public MacroDefSymbol resolveMacro(String name) {
		return macros.get(name.toLowerCase());
	}
	
	public Integer resolveLabel(String name) {
		String lcName = name.toLowerCase();
		if(isMacroSecondPass()) {
			Iterator<MacroCallSymbol> it = macroDeploys.descendingIterator();
			while (it.hasNext()) {
				Integer result = it.next().resolveLabel(lcName);
				if(null != result) return result;
			}
			it = macroSecondPass.descendingIterator();
			while (it.hasNext()) {
				Integer result = it.next().resolveLabel(lcName);
				if(null != result) return result;
			}
		}
		return labels.get(lcName);
	}
	
	public void addRegAlias(String alias, byte regId, SourcePosition sp) throws CompileException {
		String lcAlias = alias.toLowerCase();
		if(null != currentMacroDef) throw new CompileException("Regster aliases not allowed inside macro definitions", sp);
		if(regAliases.keySet().contains(lcAlias)) throw new CompileException("Alias is already in use:" + alias, sp);
		regAliases.put(lcAlias, regId);
	}
	public void removeRegAlias(String alias) {
		regAliases.remove(alias.toLowerCase());
	}

	public void beginMacro(MacroDefSymbol macro, SourcePosition sp) throws CompileException {
		if(null != currentMacroDef || isMacroDeploy()) throw new CompileException("Nested macros are not supported", sp);
		currentMacroDef = macro;
		if(macros.keySet().contains(macro.getName().toLowerCase())) {
			throw new CompileException("A macro with this name already exists:" + macro.getName(), sp);
		}
		macros.put(macro.getName().toLowerCase(), macro);
	}
	public void endMacro(SourcePosition sp) throws CompileException {
		if(null == currentMacroDef) throw new CompileException("Macro end encountered without a matching begin", sp);
		currentMacroDef = null;
	}
	
	public void setTabSize(int size) {
		tabSize = size;
	}
	public static int getTabSize() {
		return tabSize;
	}
	
	public IncludeSymbol getIncludeSymbol() throws CriticalParseException {
		return currentInclude;
	}

	public void beginMacroDeploy(MacroCallSymbol symbol) {
		macroDeploys.add(symbol);
	}
	public boolean isMacroDeploy() {
		return !macroDeploys.isEmpty();
	}
	public void endMacroDeploy() {
		macroDeploys.removeLast();
	}
	public MacroCallSymbol getMarcoCall() {
		return macroDeploys.getLast();
	}
	
	public boolean isMacroDef() {
		return null != currentMacroDef;
	}
	public MacroDefSymbol getMacroDef() {
		return currentMacroDef;
	}

	public void beginMacroSecondPass(MacroCallSymbol symbol) {
		macroSecondPass.add(symbol);
	}
	public boolean isMacroSecondPass() {
		return !macroSecondPass.isEmpty();
	}
	public void endMacroSecondPass() {
		macroSecondPass.removeLast();
	}
	
	public InstrReader getInstrReader() {
		return instrReader;
	}
	
	public CodeSegment getCSeg() throws CompileException {
		if(null==cSeg) {
			VariableSymbol vs = resolveVariable("flash_size");
			if(null!=vs) {
				cSeg = new CodeSegment(((int)vs.getValue())/2);
			}
			else throw new CompileException("FLASH_SIZE constant not found", null);
		}
		return cSeg;
	}
	
	public static void setStrictLevel(int _strinctLevel) {
		strictLevel = _strinctLevel;
	}
	public static int getStrincLevel() {
		return strictLevel;
	}

	public void makeMap(File mapFile) throws FileNotFoundException, IOException {
		try (BufferedWriter bf = new BufferedWriter(new FileWriter(mapFile))) {
			bf.write("\n# ==== Register aliases ====\n");
			List<String> list = new ArrayList<>(regAliases.keySet());
			Collections.sort(list);
			for(String name : list) {
				bf.write("R\tR" + regAliases.get(name) + "\t" + name + "\n");
			}
			
			bf.write("\n# ==== Constants ====\n");
			list = new ArrayList<>(variables.keySet());
			Collections.sort(list);
			for(String name : list) {
				VariableSymbol symbol = variables.get(name);
				if(symbol.isConstant()) {
					bf.write("C\t" + symbol.getName() + " = " + symbol.getValue() + "\n");
				}
			}
			
			bf.write("\n# ==== Variables ====\n");
			for(String name : list) {
				VariableSymbol symbol = variables.get(name);
				if(!symbol.isConstant()) {
					bf.write("V\t" + symbol.getName() + " = " + symbol.getValue() + "\n");
				}
			}

			bf.write("\n# ==== Labels ====\n");
			list = new ArrayList<>(labels.keySet());
			Collections.sort(list);
			for(String name : list) {
				bf.write("L\t" + name + " = " + labels.get(name) + "\n");
			}
		}
	}
	
	public void list(String str) {
		if(null!=listWriter) {
			try {
				listWriter.write(str + "\n");
			}
			catch(Exception e) {}
		}
	}
	public void setListEnabled(boolean enabled) {
		this.listEnabled = enabled;
	}
	public boolean isListEnabled() {
		return null != listWriter && listEnabled;
	}

	public void setListMacEnabled(boolean enabled) {
		this.listMacEnabled = enabled;
	}
	public boolean isListMacEnabled() {
		return null != listWriter && listMacEnabled;
	}
	
	// TODO необходимо реализовать проверку перекрытия кода с учетом этого признака
	public void setOverlapAllowed(boolean allowed) {
		this.overlapAllowed = allowed;
	}
	
	public ExternalTokenProvider getTokenProvider() {
		return tokenProvider;
	}
}

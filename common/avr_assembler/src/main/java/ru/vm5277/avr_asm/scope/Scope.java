/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import ru.vm5277.avr_asm.InstrReader;
import static ru.vm5277.avr_asm.Main.tabSize;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;

public class Scope {
	protected	final	static	Map<String, Byte>			registers		= new HashMap<>();
	static {
		for(byte i=0; i<32; i++) {
			if(i<10) registers.put("r0"+i, i);
			registers.put("r"+i, i);
		}
	}
	public	final	static	int										STRICT_STRONG	= 1;
	public	final	static	int										STRICT_LIGHT	= 2;
	public	final	static	int										STRICT_NONE		= 3;
	private					String									name;
	private			static	String									mcu				= null;
	private			static	InstrReader								instrReader;
	private	final			BufferedWriter							listWriter;
	private					boolean									listEnabled		= true;
	private					boolean									listMacEnabled	= true;
	private					boolean									overlapAllowed	= true;
	private			static	int										strictLevel		= STRICT_LIGHT;
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
	private final			List<IncludeSymbol>						includeSymbols	= new ArrayList<>();
	private					IncludeSymbol							currentInclude	= null;

	public Scope(File sourceFile, InstrReader instrReader, BufferedWriter listWriter) throws ParseException {
		this.instrReader = instrReader;
		addImport(sourceFile.getAbsolutePath());
		this.listWriter = listWriter;
	}
	
	public void setDevice(String device) throws ParseException {
		if(null != this.mcu) throw new ParseException("TODO устройство уже задано", null);
		this.mcu = device;
		instrReader.setMCU(device);
	}
	
	public boolean addImport(String fullPath) throws ParseException {
		if(null != currentMacroDef || isMacroDeploy()) throw new ParseException("Imports not allowed inside macro definitions", null);
		
		for(IncludeSymbol symbol : includeSymbols) {
			if(symbol.getName().equals(fullPath)) return false;
		}
		currentInclude = new IncludeSymbol(fullPath);
		includeSymbols.add(currentInclude);
		return true;
	}
	public void leaveImport(SourcePosition sp) throws CriticalParseException, ParseException {
		if(0 >= includeSymbols.size()) throw new CriticalParseException("TODO список import файлов пуст", sp);
		IncludeSymbol cur = currentInclude;
		currentInclude = includeSymbols.get(includeSymbols.indexOf(currentInclude)-1);
		if(0 != cur.getBlockCntr()) {
			throw new ParseException("Unbalanced conditional blocks (expected" + cur.getBlockCntr() + "more END directives)", sp);
		}
	}
	
	public int addLabel(String name, SourcePosition sp) throws ParseException {
		if(labels.keySet().contains(name)) throw new ParseException("Label '" + name + "' already defined", sp); //TODO
		if(null != registers.get(name)) throw new ParseException("TODO имя метки совпадает с регистром:" + name, sp);
		if(regAliases.keySet().contains(name)) {
			throw new ParseException("TODO имя метки совпадает с алиасом регистра:" + name, sp);
		}
		if(name.equals("pc") || variables.keySet().contains(name)) throw new ParseException("Symbol '" + name + "' already defined as variable", sp);
		
		int addr = cSeg.getPC();
		if(isMacroDeploy()) {
			MacroCallSymbol symbol = macroDeploys.getLast();
			symbol.addLabel(name, sp, addr);
		}
		else {
			labels.put(name, addr);
		}
		return addr;
	}
	
	public void setVariable(VariableSymbol variableSymbol, SourcePosition sp) throws ParseException {
		String varName = variableSymbol.getName();
		if(varName.equals("pc")) throw new ParseException("TODO нельзя использовать регистр PC в качестве переменной", sp);
		if(null != registers.get(varName)) throw new ParseException("TODO имя переменной совпадает с регистром:" + varName, sp);
		if(regAliases.keySet().contains(varName)) throw new ParseException("TODO имя переменной совпадает с алиасом регистра:" + varName, sp);
		VariableSymbol vs = variables.get(varName);
		if(null != vs && vs.isConstant()) throw new ParseException("TODO Нельзя переписать значение константы:" + varName, sp);
		if(isMacroDeploy()) {
			MacroCallSymbol symbol = macroDeploys.getLast();
			symbol.addVariable(variableSymbol, sp, cSeg.getPC());
		}
		else {
			variables.put(varName, variableSymbol);
		}
	}

	public VariableSymbol resolveVariable(String name) throws ParseException {
		if(name.equals("pc")) {
			return new VariableSymbol(name, cSeg.getPC()-1, true); //TODO Костыль?
		}
		if(isMacroSecondPass()) {
			Iterator<MacroCallSymbol> it = macroDeploys.descendingIterator();
			while (it.hasNext()) {
				VariableSymbol result = it.next().resolveVariable(name);
				if(null != result) return result;
			}
			it = macroSecondPass.descendingIterator();
			while (it.hasNext()) {
				VariableSymbol result = it.next().resolveVariable(name);
				if(null != result) return result;
			}
		}
		return variables.get(name);
	}

	public Byte resolveReg(String name) {
		Byte result = registers.get(name);
		if(null == result) 	result = regAliases.get(name);
		return result;
	}
	
	public MacroDefSymbol resolveMacro(String name) {
		return macros.get(name);
	}
	
	public Integer resolveLabel(String name) {
		if(isMacroSecondPass()) {
			Iterator<MacroCallSymbol> it = macroDeploys.descendingIterator();
			while (it.hasNext()) {
				Integer result = it.next().resolveLabel(name);
				if(null != result) return result;
			}
			it = macroSecondPass.descendingIterator();
			while (it.hasNext()) {
				Integer result = it.next().resolveLabel(name);
				if(null != result) return result;
			}
		}
		return labels.get(name);
	}
	
	public void addRegAlias(String alias, byte regId, SourcePosition sp) throws ParseException {
		if(null != currentMacroDef) throw new ParseException("Regster aliases not allowed inside macro definitions", sp);
		if(regAliases.keySet().contains(alias)) throw new ParseException("TODO алиас уже занят:" + alias, sp);
		regAliases.put(alias, regId);
	}
	public void removeRegAlias(String alias) {
		regAliases.remove(alias);
	}

	public void beginMacro(MacroDefSymbol macro, SourcePosition sp) throws ParseException {
		if(null != currentMacroDef || isMacroDeploy()) throw new ParseException("TODO Вложенные макросы не поддерживаются", sp);
		currentMacroDef = macro;
		if(macros.keySet().contains(macro.getName())) {
			throw new ParseException("TODO максрос с таким именем уже существует:" + macro.getName(), sp);
		}
		macros.put(macro.getName(), macro);
	}
	public void endMacro(SourcePosition sp) throws ParseException {
		if(null == currentMacroDef) throw new ParseException("TODO Вложенные конца макроа без его начала", sp);
		currentMacroDef = null;
	}
	
	public String getName() {
		return name;
	}
	
	public void setTabSize(int size) {
		tabSize = size;
	}
	public static int getTabSize() {
		return tabSize;
	}
	
	public IncludeSymbol getIncludeSymbol() throws CriticalParseException {
		if(0 >= includeSymbols.size()) throw new CriticalParseException("TODO список import файлов пуст", null);
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
	
	public CodeSegment getCSeg() throws ParseException {
		if(null == cSeg) {
			VariableSymbol vs = resolveVariable("flash_size");
			if(null != vs) {
				cSeg = new CodeSegment(((int)vs.getValue())/2);
			}
			else throw new ParseException("TODO не найдена константа FLASH_SIZE", null);
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
		if(null != listWriter) try {listWriter.write(str + "\n");} catch(Exception e) {}
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
}

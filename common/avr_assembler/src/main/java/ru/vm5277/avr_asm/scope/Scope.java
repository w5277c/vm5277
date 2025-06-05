/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.avr_asm.InstrReader;
import static ru.vm5277.avr_asm.Main.tabSize;
import ru.vm5277.avr_asm.nodes.MnemNode;
import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.exceptions.ParseException;

public class Scope {
	protected	final	static	Map<String, Byte>			registers		= new HashMap<>();
	static {
		for(byte i=0; i<32; i++) {
			if(i<10) registers.put("r0"+i, i);
			registers.put("r"+i, i);
		}
	}
	private					String									name;
	private			static	int										TABSIZE			= 4;
	private			static	String									device			= null;
	private			static	InstrReader								instrReader;
	private					Set<String>								supInstIds		= new HashSet<>();	// Поддерживаемые инструкции
	private	final			List<String>							importedFiles	= new ArrayList<>();
	private	final			Map<String, Byte>						regAliases		= new HashMap<>();	// Алиасы регистров
	private	final			Map<String, VariableSymbol>				variables		= new HashMap<>();
	private	final			Map<String, Integer>					labels			= new HashMap<>();
	private	final			Map<String, MacroSymbol>				macros			= new HashMap<>();
	private					CodeSegment								cSeg;
	private					List<Node>								dSeg;
	private					List<Node>								eSeg;
	private					Segment									currentSegment;
	private					MacroSymbol								currentMacro	= null;
	private					boolean									isMacroMode		= false;
	private					List<Expression>						macroParams		= null;
	private					int										blockCntr		= 0;
	private					boolean									isBlockSkip		= false;
	private					List<MnemNode>							mnemNodes		= new ArrayList<>();

	public Scope(InstrReader instrReader) {
		this.instrReader = instrReader;
	}
	
	public void setDevice(String device) throws ParseException {
		if(null != this.device) throw new ParseException("TODO устройство уже задано", null);
		this.device = device;
	}
	
	public boolean addImport(String basePath, String path) throws ParseException {
		if(null != currentMacro) throw new ParseException("TODO Import не поддерживается в макросе", null);
		String fullPath = basePath + File.separator + path;
		if(importedFiles.contains(fullPath)) return false;
		importedFiles.add(fullPath);
		return true;
	}
	
	public void addSecondPassNode(MnemNode mnemNode) {
		if(null != mnemNode) {
			mnemNodes.add(mnemNode);
		}
	}
	public List<MnemNode> getMnemNodes() {
		return mnemNodes;
	}
	
	public void addLabel(String name) throws ParseException {
		if(null != currentMacro) throw new ParseException("TODO Метки не поддерживается в макросе", null);
		if(labels.keySet().contains(name)) throw new ParseException("Label already defined:" + name, null); //TODO
		if(null != registers.get(name)) throw new ParseException("TODO имя метки совпадает с регистром:" + name, null);
		if(regAliases.keySet().contains(name)) {
			throw new ParseException("TODO имя метки совпадает с алиасом регистра:" + name, null);
		}
		if(variables.keySet().contains(name)) throw new ParseException("TODO имя метки совпадает с переменной:" + name, null);
		labels.put(name, getCSeg().getCurrentBlock().getAddress());
	}
	
	public void setVariable(VariableSymbol variableSymbol) throws ParseException {
		if(null != currentMacro) throw new ParseException("TODO переменные не поддерживаются в макросе", null);
		String name = variableSymbol.getName();
		if(null != registers.get(name)) throw new ParseException("TODO имя переменной совпадает с регистром:" + name, null);
		if(regAliases.keySet().contains(name)) throw new ParseException("TODO имя переменной совпадает с алиасом регистра:" + name, null);
		VariableSymbol vs = variables.get(name);
		if(null != vs && vs.isConstant()) throw new ParseException("TODO Нельзя переписать значение константы:" + name, null);
		
		variables.put(name, variableSymbol);
	}

	public VariableSymbol resolveVariable(String name) {
		return variables.get(name);
	}

	public Byte resolveReg(String name) {
		Byte result = registers.get(name);
		if(null == result) 	result = regAliases.get(name);
		return result;
	}
	
	public MacroSymbol resolveMacro(String name) {
		return macros.get(name);
	}
	
	public Integer resolveLabel(String name) {
		return labels.get(name);
	}
	
	public void addRegAlias(String alias, byte regId) throws ParseException {
		if(null != currentMacro) throw new ParseException("TODO Алиасы не поддерживаются в макросе", null);
		if(regAliases.keySet().contains(alias)) throw new ParseException("TODO алиас уже занят:" + alias, null);
		regAliases.put(alias, regId);
	}
	
	public void startMacro(MacroSymbol macro) throws ParseException {
		if(null != currentMacro) throw new ParseException("TODO Вложенные макросы не поддерживаются", null);
		currentMacro = macro;
		if(macros.keySet().contains(macro.getName())) {
			throw new ParseException("TODO максрос с таким именем уже существует:" + macro.getName(), null);
		}
		macros.put(macro.getName(), macro);
	}
	public void endMacro() throws ParseException {
		if(null == currentMacro) throw new ParseException("TODO Вложенные конца макроа без его начала", null);
		currentMacro = null;
	}
	public boolean isMacro() {
		return null != currentMacro;
	}
	public MacroSymbol getCurrentMacro() {
		return currentMacro;
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
	
	public void blockStart(boolean skip) {
		blockCntr++;
		isBlockSkip |= skip;
	}
	
	public void blockEnd() throws ParseException {
		if(blockCntr > 0) {
			blockCntr--;
		}
		else {
			throw new ParseException("TODO конец блока без его начала", null);
		}
	}
	
	public boolean isBlockSkip() {
		return 0!=blockCntr && isBlockSkip;
	}

	public void startMacroImpl(List<Expression> macroParams) {
		this.isMacroMode = true;
		this.macroParams = macroParams;
	}
	public boolean isMacroMode() {
		return isMacroMode;
	}
	public void stopMacroImpl() {
		this.isMacroMode = false;
		this.macroParams = null;
	}
	public Expression getMacroParam(int index) {
		return index>= macroParams.size() ? null : macroParams.get(index);
	}
	
	public InstrReader getInstrReader() {
		return instrReader;
	}
	
	public void addMnemonics(Collection<String> instructionIds) {
		this.supInstIds.addAll(instructionIds);
	}
	public boolean isSupported(String instructionicId) {
		return this.supInstIds.contains(instructionicId);
	}
	
	public CodeSegment getCSeg() throws ParseException {
		if(null == cSeg) {
			VariableSymbol vs = resolveVariable("flash_size");
			if(null != vs) {
				cSeg = new CodeSegment((int)vs.getValue());
				currentSegment = cSeg;
			}
			else throw new ParseException("TODO не найдена константа FLASH_SIZE", null);
		}
		return cSeg;
	}
	
	public Segment getCurrentSegment() {
		return currentSegment;
	}
}

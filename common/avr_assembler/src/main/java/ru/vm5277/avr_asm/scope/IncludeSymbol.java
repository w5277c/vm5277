/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.util.Stack;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class IncludeSymbol extends Symbol {
	private					int										blockCntr		= 0;
	private					boolean									blockSuccess	= false;
	private					boolean									elseIfSkip		= false;
	private					Stack<Boolean>							blockSkip		= new Stack<>();
	private					IncludeSymbol							parent;
	
	public IncludeSymbol(String name, IncludeSymbol parent) {
		super(name);
		
		this.parent = parent;
	}

	public void blockStart(boolean skip, SourcePosition sp) {
		blockSuccess |= !skip;
		
		blockSkip.add(skip);
		blockCntr++;
	}

	public void blockSkipInvert(SourcePosition sp) {
		if(!blockSkip.isEmpty()) {
			blockSkip.add(!blockSkip.pop());
		}
	}

	public void blockElseIf(boolean skip, SourcePosition sp) {
		if(!blockSuccess) {
			elseIfSkip = skip;
			blockSuccess |= !skip;
		}
	}

	public int getBlockCntr() {
		return  blockCntr;
	}

	public void blockEnd(SourcePosition sp) throws ParseException {
		elseIfSkip = false;
		blockSuccess = false;
		
		blockCntr--;
		if(!blockSkip.isEmpty()) {
			blockSkip.pop();
		}
		else {
			throw new ParseException("END directive without matching block (no IF/IFDEF/etc opened)", sp);
		}
	}
	
	public boolean isBlockSkip() {
		boolean result = elseIfSkip;
		for(Boolean skip : blockSkip) {
			result |=skip;
		}
		return result;
	}
	
	public IncludeSymbol getParent() {
		return parent;
	}
}

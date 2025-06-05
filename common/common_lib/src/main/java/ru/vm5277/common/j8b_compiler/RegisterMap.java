/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
26.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common.j8b_compiler;

import ru.vm5277.common.exceptions.ParseException;

public class RegisterMap {
	private	String		methodQName;
	private	String		rtosFunction;
	private	byte[][]	regIds;
	private	boolean		supported	= true;
	
	public RegisterMap(String str) throws ParseException {
		try {
			String[] parts = str.split("\\s+");
			if(3 == parts.length || 4 == parts.length) {
				methodQName = parts[0];
				rtosFunction = parts[1];
				String[] oParts = parts[2].trim().split("\\.");
				if(0 != oParts.length) {
					regIds = new byte[oParts.length][];
					for(int i=0; i<oParts.length; i++) {
						regIds[i] = getSeparated(oParts[i].trim());
					}
				}

				if(0x04 == parts.length) {
					if("unsupported".equals(parts[3])) {
						supported = false;
					}
				}
			}
		}
		catch(Exception ex) {
			throw new ParseException("Invalid record \"" + str + "\" in register map", null);
		}
	}
	
	private byte[] getSeparated(String str) {
		byte[] result = null;
		if(!str.isEmpty()) {
			String[] parts = str.split(",");
			if(0 != parts.length) {
				result = new byte[parts.length];
				for(int i=0; i<parts.length; i++) {
					result[i] = Byte.parseByte(parts[i]);
				}
			}
		}
		return result;
	}
	
	public String getMethodQName() {
		return methodQName;
	}
	
	public String getRtosFunction() {
		return rtosFunction;
	}
	
	public byte[][] getRegIds() {
		return regIds;
	}

	public boolean isSupported() {
		return supported;
	}
}

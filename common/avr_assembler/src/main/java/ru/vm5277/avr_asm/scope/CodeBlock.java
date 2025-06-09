/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

public class CodeBlock extends Block {
	public CodeBlock(int startAddr) {
		this.startAddr = startAddr;
	}

	public void writeOpcode(int opcode) {
		byte[] bdata = new byte[0x02];
		bdata[0x00] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x01] = (byte)(opcode & 0xff);
		write(bdata, 0x02);
	}
	public void writeDoubleOpcode(long opcode) {
		byte[] bdata = new byte[0x04];
		bdata[0x00] = (byte)((opcode >> 0x18) & 0xff);
		bdata[0x01] = (byte)((opcode >> 0x10) & 0xff);
		bdata[0x02] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x03] = (byte)(opcode & 0xff);
		write(bdata, 0x04);
	}
	
	public int getPC() {
		return (startAddr+offset)/2;
	}
}

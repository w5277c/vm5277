/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import ru.vm5277.common.exceptions.ParseException;


public class CodeBlock {
	private	final	static	int					PART_SIZE	= 0x80;
	private final			int					startWAddress;
	private					int					offset		= 0;
	private					int					length;
	private					byte[]				data		= new byte[PART_SIZE];

	public CodeBlock(int startWAddress) {
		this.startWAddress = startWAddress;
	}

	public void writeOpcode(int opcode) throws ParseException {
		byte[] bdata = new byte[0x02];
		bdata[0x00] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x01] = (byte)(opcode & 0xff);
		write(bdata, 0x01);
	}
	public void writeDoubleOpcode(long opcode) throws ParseException {
		byte[] bdata = new byte[0x04];
		bdata[0x00] = (byte)((opcode >> 0x18) & 0xff);
		bdata[0x01] = (byte)((opcode >> 0x10) & 0xff);
		bdata[0x02] = (byte)((opcode >> 0x08) & 0xff);
		bdata[0x03] = (byte)(opcode & 0xff);
		write(bdata, 0x02);
	}
	
	public void write(byte[] bdata, int wSize) {
		try {
			int _bytesSize = wSize*2;
			if(data.length-offset < _bytesSize) {
				byte[] _data = new byte[offset + _bytesSize + PART_SIZE];
				System.arraycopy(data, 0x00, _data, 0x00, offset);
				data = _data;
			}
			System.arraycopy(bdata, 0x00, data, offset, _bytesSize);
			offset += _bytesSize;
			if(offset>length) length=offset;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

/*	public void append(CodeBlock block) {
		byte _data[] = new byte[offset + block.getLength()];
		System.arraycopy(data, 0x00, _data, 0x00, offset);
		System.arraycopy(block.getData(), 0x00, _data, offset, block.getLength());
		data = _data;
		offset = data.length;
		if(offset>length) length=offset;
	}*/

	public int getStartWAddress() {
		return startWAddress;
	}
	public void setOffset(int wAddr) {
		offset = (wAddr-startWAddress)*2;	
	}
	
	public int getLength() {
		return length;
	}
	
	public byte[] getData() {
		return data;
	}
}

/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
12.06.2025	konstantin@5277.ru		Взято из проекта javra
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.output;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.bind.DatatypeConverter;

public class IntelHexBuilder implements Builder, Closeable {
	public static class Row {
		public	static	final	int	DATA			= 0x00;
		public	static	final	int	EOF				= 0x01;
		public	static	final	int	EXT_SEGMENT		= 0x02;
		public	static	final	int	EXT_LINEAR		= 0x04;
		public	static	final	int	START_LINEAR	= 0x05;

		public static void write(OutputStream os, int type, int address, byte[] data, int offset, int length) throws IOException {
			byte[] tmp = new byte[0x01 + 0x02 + 0x01 + length + 0x01];
			tmp[0x00] = (byte)length;
			tmp[0x01] = (byte)((address / 0x100) & 0x0ff);
			tmp[0x02] = (byte)(address & 0xff);
			tmp[0x03] = (byte)type;
			if(null != data && 0 != length) {
				System.arraycopy(data, offset, tmp, 0x04, length);
			}
			int checksum = 0x00;
			for(int pos = 0x00; pos < (tmp.length-0x01); pos++) {
				checksum += tmp[pos];
			}
			tmp[tmp.length-0x01] = (byte)((0x01 + ~(checksum )) & 0xff);
			os.write((":" + DatatypeConverter.printHexBinary(tmp).toUpperCase()+"\r\n").getBytes());
		}
	}

	private	final	FileOutputStream	fos;
	
	public IntelHexBuilder(File file) throws Exception {
		fos = new FileOutputStream(file);
		Row.write(fos, Row.EXT_SEGMENT, 0x0000, new byte[]{0x00, 0x00}, 0x0000, 0x02);
	}
	
	@Override
	public void push(byte[] data, int address, int length) throws IOException {
		for(int offset=0x00; offset<length; offset+=0x10) {
			Row.write(fos, Row.DATA, address+offset, data, offset, (length-offset)>=0x10 ? 0x10 : (length-offset));
		}
	}
	
	@Override
	public void close() throws IOException {
		Row.write(fos, Row.EOF, 0x0000, null, 0x0000, 0x00);
		
		if(null != fos) try {fos.close();} catch(Exception ex) {ex.printStackTrace();}
	}
}

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
package ru.vm5277.avr_asm;

import java.text.ParseException;

public class Instruction {
	public	final	static	String	FLAGS_NONE	= "none";
	public	final	static	String	FLAGS_SREGS	= "sregs";
	public	final	static	String	OPERAND_R	= "r";		// Register 0<=R<=31
	public	final	static	String	OPERAND_RH	= "rh";		// High register 16<=R<=31
	public	final	static	String	OPERAND_RA	= "ra";		// arithmetic register R24,R26,R28,R30
	public	final	static	String	OPERAND_RE	= "re";		// Even registers R0,R2,R4,...,R30
	public	final	static	String	OPERAND_A	= "a";		// I/O register address
	public	final	static	String	OPERAND_RR	= "rr";		// Register for both operands
	public	final	static	String	OPERAND_K3	= "k3";		// Constant 0<=k<=7
	public	final	static	String	OPERAND_K6	= "k6";		// Constant 0<=k<=63
	public	final	static	String	OPERAND_K7S	= "k7s";	// Constant -64<=k<=63
	public	final	static	String	OPERAND_K8	= "k8";		// Constant 0<=k<=255
	public	final	static	String	OPERAND_K12S= "k12s";	// Constant -2048<=k<=2047
	public	final	static	String	OPERAND_K16	= "k16";	// Constant 0<=k<=65535
	public	final	static	String	OPERAND_K22	= "k22";	// Constant 0<=k<=4M
	public	final	static	String	OPERAND_NONE= "none";	// No operand
	public	final	static	String	OPERAND_X	= "x";
	public	final	static	String	OPERAND_XP	= "x+";
	public	final	static	String	OPERAND_MX	= "-x";
	public	final	static	String	OPERAND_Y	= "y";
	public	final	static	String	OPERAND_YP	= "y+";
	public	final	static	String	OPERAND_MY	= "-y";
	public	final	static	String	OPERAND_Z	= "z";
	public	final	static	String	OPERAND_ZP	= "z+";
	public	final	static	String	OPERAND_MZ	= "-z";
	
	
	private	String		id;
	private	String		mnemonic;
	private	String		flags;
	private	byte[]		clocks;
	private	byte		wsize;
	private	int			opcode;
	private	String[]	operands;
	
	public Instruction(String line) throws ParseException {
		String parts[] = line.trim().toLowerCase().split("\\s+");
		if(0x07!=parts.length) {
			throw new ParseException("Expected 7 parameters for instruction, got [" + parts.length + "] in: " + line, 0);
		}
		
		id = parts[0x00];
		
		mnemonic = parts[0x01];
		
		flags = parts[0x02];
		if(!flags.equals(FLAGS_NONE) && !flags.equals(FLAGS_SREGS) && !flags.replaceAll("[zcnvtish]", "").isEmpty()) {
			throw new ParseException("Unknown flag[s] in:" + line, 0);
		}
		
		if(!parts[0x03].equals("-")) {
			String[] clocksParts = parts[0x03].split("/");
			clocks = new byte[clocksParts.length];
			for(int i=0; i< clocksParts.length; i++) {
				clocks[i] = Byte.parseByte(clocksParts[i]);
			}
		}
		
		wsize = Byte.parseByte(parts[0x04]);
		
		opcode = Integer.parseInt(parts[0x05].replaceFirst("0x", ""), 0x10);
		
		operands = parts[0x06].trim().split(",");
	}
	
	public String getId() {
		return id;
	}
	
	public String getMnemonic() {
		return mnemonic;
	}
	
	public String getFlags() {
		return flags;
	}
	
	public byte[] getClocks() {
		return clocks;
	}
	
	public byte getWSize() {
		return wsize;
	}
	
	public int getOpcode() {
		return opcode;
	}
	
	public String[] getOperands() {
		return operands;
	}
	
	@Override
	public String toString() {
		return id;
	}
}

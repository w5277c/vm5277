/*
 * Copyright 2026 konstantin@5277.ru
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

.IF OS_FT_PRND == 0x01
.IFNDEF OS_PRND

.IF OS_FT_BLDR_API_REUSE == 0x00
_OS_PRND__RANDDATA:
	.RNDB 0x08
.ENDIF
;-----------------------------------------------------------
OS_PRND:
;-----------------------------------------------------------
;Геренирует псевдослучайное число
;-----------------------------------------------------------
;OUT: ACCUM_L-псевдослучайное число
;-----------------------------------------------------------
	PUSH_T16

	LDS ACCUM_L,_OS_PRND_CNTR+0x00
	LDS TEMP_H,_OS_PRND_CNTR+0x01
	INC ACCUM_L
	ANDI ACCUM_L,0x07
	STS _OS_PRND_CNTR+0x00,ACCUM_L
	BRNE _OS_PRND__HCNTR_SKIP
	INC TEMP_H

.IF OS_FT_TIMER == 0x01
	LDS TEMP_L,_OS_UPTIME
	ADD TEMP_H,TEMP_L
.ENDIF
	LDS TEMP_L,TCNT0
	ADD TEMP_H,TEMP_L

	SEC
	ROR TEMP_H
	BRCC _OS_PRND__NO_XOR
	LDI TEMP_L,0x9C
	EOR TEMP_H,TEMP_L
_OS_PRND__NO_XOR:
	STS _OS_PRND_CNTR+0x01,TEMP_H
_OS_PRND__HCNTR_SKIP:
.IF OS_FT_BLDR_API_REUSE == 0x01
	LDI_Z _OS_BLDR_UID_DATA*2
.ELSE
	LDI_Z _OS_PRND__RANDDATA*2
.ENDIF
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z
	SUB ACCUM_L,TEMP_H
	SWAP ACCUM_L

	POP_T16
	RET
.ENDIF
.ENDIF

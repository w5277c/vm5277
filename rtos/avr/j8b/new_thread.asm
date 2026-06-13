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

.IFNDEF J8BPROC_NEW_THREAD
.include "dmem/dram.asm"
;-----------------------------------------------------------
J8BPROC_NEW_THREAD:
;-----------------------------------------------------------
;Вынесен блок кода для оптимизации - выделяет и
;инициализирует HEAP для потока
;IN: ACCUM_L/H-размер блока
;MOD: ACCUM_L/H
;OUT: Z-адрес на выделенный блок, флаг С-OUT OF RAM
;-----------------------------------------------------------
	SET
	MCALL OS_DRAM_ALLOC
	BRCS _J8BPROC_NEW_THREAD__OVF
	STD Z+0x00,ACCUM_L										;Длина
	STD Z+0x01,ACCUM_H
	STD Z+0x02,C0x01										;RefCntr
	;META будет заполнена верхним уровнем
	;STATUS BITS будет заполнен верхним уровнем
	STD Z+0x06,C0x00										;Адрес следующей нити
	STD Z+0x07,C0x00
	LDI ACCUM_L,0x80
	STD Z+0x1c,ACCUM_L										;SREG
	PUSH_Y
	LDS YL,OS_FIRSTTHREAD_ADDR+0x00							;Адрес HEAP
	LDS YH,OS_FIRSTTHREAD_ADDR+0x01
	CP YL,C0x00
	CPC YH,C0x00
	BRNE _J8BPROC_NEW_THREAD__LOOP
	STS OS_FIRSTTHREAD_ADDR+0x00,ZL
	STS OS_FIRSTTHREAD_ADDR+0x01,ZH
	RJMP _J8BPROC_NEW_THREAD__END
_J8BPROC_NEW_THREAD__LOOP:
	LDD ACCUM_L,Y+0x06										;В аккумуляторе адрес HEAP следующей нити
	LDD ACCUM_H,Y+0x07
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ J8BPROC_NEW_THREAD__GOT_LAST
	MOVW YL,ACCUM_L
	RJMP _J8BPROC_NEW_THREAD__LOOP
J8BPROC_NEW_THREAD__GOT_LAST:
	STD Y+0x06,ZL
	STD Y+0x07,ZH
_J8BPROC_NEW_THREAD__END:
	;TASK ENDPOINT будет заполнен верхним уровнем
	POP_Y
	CLC
_J8BPROC_NEW_THREAD__OVF:
	CLT
	RET
.ENDIF
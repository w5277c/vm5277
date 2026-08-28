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

;VERSION 0.1 - черновая версия, требует тщательного тестирования
;Круговой 16 битный буфер

.IFNDEF OS_DRV_CBUFF16__INIT

;Константы смещений в HEAP
.EQU _OS_DRV_CBUFF16__HEAP_HEAP_SIZE				= 0x00	;2B-HEAP SIZE
.EQU _OS_DRV_CBUFF16__HEAP_LINK_CNTR				= 0x02	;1B-Link counter
.EQU _OS_DRV_CBUFF16__HEAP_METADATA_ADDR			= 0x03	;2B-HEAP SIZE
.EQU _OS_DRV_CBUFF16__HEAP_ARRAY_ADDR				= 0x05	;2B
.EQU _OS_DRV_CBUFF16__HEAP_BUFF_SIZE				= 0x07	;2B
.EQU _OS_DRV_CBUFF16__HEAP_ROFFSET					= 0x09	;2B
.EQU _OS_DRV_CBUFF16__HEAP_WOFFSET					= 0x0b	;2B
.EQU _OS_DRV_CBUFF16__HEAP_TOTAL_SIZE				= 0x0d

;-----------------------------------------------------------
OS_DRV_CBUFF16__INIT:
;-----------------------------------------------------------
;Инициализация буфера
;IN:Y-адрес HEAP
;-----------------------------------------------------------
	MCALL OS_DISPATCHER_LOCK
	STD Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x00,C0x00
	STD Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x01,C0x00
	STD Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x00,C0x00
	STD Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x01,C0x00
	MCALL OS_DISPATCHER_UNLOCK
	RET

;-----------------------------------------------------------
OS_DRV_CBUFF16__GET_AVAILABLE:
;-----------------------------------------------------------
;Возвращаем размер занятой части буфера
;IN:Y-адрес HEAP
;OUT-ACCUM_L/H-занято
;MOD: ACCUM_EL/EH
;-----------------------------------------------------------
	MCALL OS_DISPATCHER_LOCK
	LDD ACCUM_L,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x00
	LDD ACCUM_H,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x01
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x01
	MCALL OS_DISPATCHER_UNLOCK
	SUB ACCUM_L,ACCUM_EL
	SBC ACCUM_H,ACCUM_EH
	BRCC OS_DRV_CBUFF16__GET_AVAILABLE__END
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x01
	ADD ACCUM_L,ACCUM_EL
	ADC ACCUM_H,ACCUM_EH
OS_DRV_CBUFF16__GET_AVAILABLE__END:
	RET

;-----------------------------------------------------------
OS_DRV_CBUFF16__GET_FREE:
;-----------------------------------------------------------
;Возвращаем размер свободной части буфера
;IN:Y-адрес HEAP
;OUT-ACCUM_L/H-свободно
;MOD: ACCUM_EL/EH
;-----------------------------------------------------------
	MCALL OS_DISPATCHER_LOCK
	LDD ACCUM_L,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x00
	LDD ACCUM_H,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x01
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x01
	MCALL OS_DISPATCHER_UNLOCK
	SUB ACCUM_L,ACCUM_EL
	SBC ACCUM_H,ACCUM_EH
	BREQ PC+0x02
	BRCC OS_DRV_CBUFF16__GET_FREE__END
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x01
	ADD ACCUM_L,ACCUM_EL
	ADC ACCUM_H,ACCUM_EH
OS_DRV_CBUFF16__GET_FREE__END:
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	RET

;-----------------------------------------------------------
OS_DRV_CBUFF16__WRITE:
;-----------------------------------------------------------
;Записываем байт в буфер
;IN:Y-адрес HEAP, ACCUM_L-значение
;MOD: ACCUM_L/H/EL/EH,X
;-----------------------------------------------------------
	MCALL OS_DISPATCHER_LOCK
	LDD XL,Y+_OS_DRV_CBUFF16__HEAP_ARRAY_ADDR+0x00
	LDD XH,Y+_OS_DRV_CBUFF16__HEAP_ARRAY_ADDR+0x01
	ADIW XL,OS_J8B_1D_BYTE_ARRAY_HEADER_SIZE
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x01
	ADD XL,ACCUM_EL
	ADC XH,ACCUM_EH
	ST X,ACCUM_L
	LDD ACCUM_L,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x00
	LDD ACCUM_H,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x01
	ADD ACCUM_EL,C0x01
	ADC ACCUM_EH,C0x00
	CP ACCUM_EL,ACCUM_L
	CPC ACCUM_EH,ACCUM_H
	BRNE OS_DRV_CBUFF16__WRITE__CLEAR_OFFSET_SKIP
	LDI ACCUM_EL,0x00
	LDI ACCUM_EH,0x00
OS_DRV_CBUFF16__WRITE__CLEAR_OFFSET_SKIP:
	STD Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x00,ACCUM_EL
	STD Y+_OS_DRV_CBUFF16__HEAP_WOFFSET+0x01,ACCUM_EH
	MCALL OS_DISPATCHER_UNLOCK
	RET

;-----------------------------------------------------------
OS_DRV_CBUFF16__READ:
;-----------------------------------------------------------
;Считываем байт из буфера
;IN:Y-адрес HEAP
;OUT: ACCUM_L-значение
;MOD: ACCUM_H/EL/EH,X
;-----------------------------------------------------------
	MCALL OS_DISPATCHER_LOCK
	LDD XL,Y+_OS_DRV_CBUFF16__HEAP_ARRAY_ADDR+0x00
	LDD XH,Y+_OS_DRV_CBUFF16__HEAP_ARRAY_ADDR+0x01
	ADIW XL,OS_J8B_1D_BYTE_ARRAY_HEADER_SIZE
	LDD ACCUM_EL,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x00
	LDD ACCUM_EH,Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x01
	ADD XL,ACCUM_EL
	ADC XH,ACCUM_EH
	LDD ACCUM_L,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x00
	LDD ACCUM_H,Y+_OS_DRV_CBUFF16__HEAP_BUFF_SIZE+0x01
	ADD ACCUM_EL,C0x01
	ADC ACCUM_EH,C0x00
	CP ACCUM_EL,ACCUM_L
	CPC ACCUM_EH,ACCUM_H
	BRNE OS_DRV_CBUFF16__READ__CLEAR_OFFSET_SKIP
	LDI ACCUM_EL,0x00
	LDI ACCUM_EH,0x00
OS_DRV_CBUFF16__READ__CLEAR_OFFSET_SKIP:
	STD Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x00,ACCUM_EL
	STD Y+_OS_DRV_CBUFF16__HEAP_ROFFSET+0x01,ACCUM_EH
	LD ACCUM_L,X
	MCALL OS_DISPATCHER_UNLOCK
	RET
.ENDIF
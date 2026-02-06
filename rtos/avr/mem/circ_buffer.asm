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

.IFNDEF OS_CIRC_BUFFER_INIT_NR

;Байт со смещением 0x00-позиция для записи
;Байт со смещением 0x01-позиция для чтения

;-----------------------------------------------------------
OS_CIRC_BUFFER_INIT_NR:
;-----------------------------------------------------------
;Создание кольцевого буфера
;IN:Y-CIRC_BUFF ADDR, TEMP_L-размер буфера
;-----------------------------------------------------------
	PUSH_Y
	PUSH_T16
	PUSH ACCUM_L
	
	LDI ACCUM_L,0x02
	ST Y+,ACCUM_L										;Запись
	ST Y+,ACCUM_L										;Чтение
	LDI ACCUM_L,0x00
	LDI TEMP_H,0x00
	SUBI TEMP_L,0x02
	MCALL OS_RAM_FILL16_NR

	POP ACCUM_L
	POP_T16
	POP_Y
	RET
	
;-----------------------------------------------------------
OS_CIRC_BUFFER_IS_EMPTY:
;-----------------------------------------------------------
;Буфер пуст?
;IN:Y-CIRC_BUFF ADDR
;OUT: флаг Z-true - пуст
;-----------------------------------------------------------
	PUSH_T16

	LDD TEMP_L,Y+0x00										;Запись
	LDD TEMP_H,Y+0x01										;Чтение
	CP TEMP_L,TEMP_H

	POP_T16
	RET
;-----------------------------------------------------------
OS_CIRC_BUFFER_WRITE:
;-----------------------------------------------------------
;Запись байта
;IN:Y-CIRC_BUFF ADDR, ACCUM_L-значение для записи,
;TEMP_L-размер буфера
;OUT: флаг C-true - переполнение
;-----------------------------------------------------------
	PUSH TEMP_H
	PUSH ACCUM_H

	LDD TEMP_H,Y+0x00										;Запись
	LDD ACCUM_H,Y+0x01										;Чтение
	PUSH_Y
	ADD YL,TEMP_H
	ADC YH,C0x00
	ST Y,ACCUM_L
	INC TEMP_H
	CP TEMP_L,TEMP_H
	BRNE PC+0x02
	LDI TEMP_H,0x02
	POP_Y
	STD Y+0x00,TEMP_H
	CP TEMP_H,ACCUM_H
	BRNE PC+0x02
	SEC

	POP ACCUM_H
	POP TEMP_H
	RET
	
;-----------------------------------------------------------
OS_CIRC_BUFFER_READ:
;-----------------------------------------------------------
;Извлечение байта
;IN:Y-CIRC_BUFF ADDR, TEMP_L-размер буфера
;OUT: ACCUM_L-значение, флаг Z-true - буфер пуст
;-----------------------------------------------------------
	PUSH ACCUM_H

	LDD ACCUM_L,Y+0x00										;Запись
	LDD ACCUM_H,Y+0x01										;Чтение

	CP ACCUM_L,ACCUM_H
	BREQ OS_CIRC_BUFFER_READ__END

	PUSH_Y
	ADD YL,ACCUM_H
	ADC YH,C0x00
	LD ACCUM_L,Y
	
	INC ACCUM_H
	CP TEMP_L,ACCUM_H
	BRNE PC+0x02
	LDI ACCUM_H,0x02
	POP_Y
	STD Y+0x01,ACCUM_H
	CLZ
	
OS_CIRC_BUFFER_READ__END:
	POP ACCUM_H
	RET

;-----------------------------------------------------------
OS_CIRC_BUFFER_GET_LAST:
;-----------------------------------------------------------
;Возвращает послений записанный байт
;IN:Y-CIRC_BUFF ADDR, TEMP_L-размер буфера
;OUT: ACCUM_L-значение, флаг Z-true - буфер пуст
;-----------------------------------------------------------
	LDD ACCUM_L,Y+0x00										;Запись
	PUSH ACCUM_H
	LDD ACCUM_H,Y+0x01										;Чтение

	CP ACCUM_L,ACCUM_H
	POP ACCUM_H
	BREQ OS_CIRC_BUFFER_GET_LAST__END

	CPI ACCUM_L,0x02
	BRNE PC+0x03
	MOV ACCUM_L,TEMP_L
	DEC ACCUM_L

	PUSH_Y
	ADD YL,ACCUM_L
	ADC YH,C0x00
	LD ACCUM_L,Y
	POP_Y
	CLZ
	
OS_CIRC_BUFFER_GET_LAST__END:
	RET
.ENDIF

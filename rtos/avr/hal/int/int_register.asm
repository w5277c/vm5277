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

.IFNDEF OS_INT_REGISTER
.include "j8b/callback_addr.asm"
.include "core/irq_set.asm"
;-----------------------------------------------------------
OS_INT_REGISTER:
;-----------------------------------------------------------
;Регистрируем внешнее прерывание для указанного пина
;IN: ACCUM_L-пин, ACCUM_H-_OS_PIN_TRIGGER_MODE_...
;ACCUM_EL/EH-callback адрес, Y-адрес HEAP
;OUT: ACCUM_L-ID регистрации обработачика событий пина
;(уникально для всех типов) 0x00,0x80-0x87,
;0x00-ошибка регистрации
;MOD: X,ACCUM_H/EL/EH
;-----------------------------------------------------------
	PUSH_T32
	PUSH_Z

	LDI_Z INT_TABLE*2
	LDI TEMP_L,0b00000011									;MASK
	LDI TEMP_H,0b00000000
	LDI TEMP_EL,0b00000001									;EIMSK BIT
	LDI XL,(0<<ISC01)|(1<<ISC00)							;ANY
	CPI ACCUM_H,_OS_PIN_TRIGGER_MODE_FALLING
	BRNE PC+0x02
	LDI XL,(1<<ISC01)|(0<<ISC00)							;FALLING
	CPI ACCUM_H,_OS_PIN_TRIGGER_MODE_RISING
	BRNE PC+0x02
	LDI XL,(1<<ISC01)|(1<<ISC00)							;RISING
.IFDEF EICRB
	LDI XH,0x00
.ENDIF
	LDI ACCUM_H,0x00
_OS_INT_REGISTER__LOOP:
	LPM TEMP_EH,Z+
	CP TEMP_EH,ACCUM_L
	BREQ _OS_INT_REGISTER__GOT
	LSL TEMP_EL
	LSL XL
.IFDEF EICRB
	ROL XH
.ENDIF
	LSL XL
.IFDEF EICRB
	ROL XH
.ENDIF
	LSL TEMP_L
.IFDEF EICRB
	ROL TEMP_H
.ENDIF
	LSL TEMP_L
.IFDEF EICRB
	ROL TEMP_H
.ENDIF
	INC ACCUM_H
	CPI ACCUM_H,OS_INT_PINS_QNT
	BRNE _OS_INT_REGISTER__LOOP
	LDI ACCUM_L,0x00
	RJMP _OS_INT_REGISTER__END
_OS_INT_REGISTER__GOT:
	CLI
	LDS ACCUM_L,EICRA
	COM TEMP_L
	AND ACCUM_L,TEMP_L
	OR ACCUM_L,XL
	STS EICRA,ACCUM_L
.IFDEF EICRB
	LDS ACCUM_L,EICRB
	COM TEMP_H
	AND ACCUM_L,TEMP_H
	OR ACCUM_L,XH
	STS EICRB,ACCUM_L
.ENDIF
	SEI

	MOV ACCUM_L,ACCUM_H										;Порядковый номер номер INTx умножаю на 5
	LSL ACCUM_L												;для вычисления номера прерывания OS_IRQ_* где ячейка = 5 байт
	LSL ACCUM_L
	ADD ACCUM_L,ACCUM_L
	ORI ACCUM_H,0b01000000									;Старшие два бита результата = тип обработчика
	LDI_Z _OS_IRQ_RAM_TABLE+OS_IRQ_INT0
	MCALL J8BPROC_CALLBACK_ADDR
	STS EIFR,TEMP_EL
	MCALL OS_IRQ_SET
	CLI
	LDS ACCUM_L,EIMSK
	OR ACCUM_L,TEMP_EL
	STS EIMSK,ACCUM_L
	SEI
	MOV ACCUM_L,ACCUM_H
_OS_INT_REGISTER__END:
	POP_Z
	POP_T32
	RET
.ENDIF

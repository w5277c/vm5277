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

.IFNDEF _OS_HAL_TIMER_COMPUTE_IOREGS
.include "hal/timer/_meta.asm"
.include "conv/us_to_ticks.asm"
;-----------------------------------------------------------
_OS_HAL_TIMER_COMPUTE_IOREGS:
;-----------------------------------------------------------
;Устанавливаем IO регистры аппаратного таймера
;IN: ACCUM_H/L-период,ACCUM_EL-номер таймера
;OUT: ACCUM_EL/H/L-тики таймера, RESULT-CS значение
;MOD: ACCUM_EH
;-----------------------------------------------------------
	PUSH_T16

	MCALL OS_HAL_TIMER_META									;|bqqqqppp|pppppppp| b=8/16бит, q-максимальное кол-во бит делителя, p-битмаска prescaler
	MCALL OS_US_TO_TICKS

	LDI RESULT,0x00											;Используем для подсчета значения CS регистра TCCRxB (0x00-no clock source - отсчет с 1)
	BST TEMP_H,0x07											;Сохраняем b в флаге T (true=16бит)
	MOV ACCUM_EH,TEMP_H										;Получаем размер маски в битах
	LSL ACCUM_EH
	SWAP ACCUM_EH
	ANDI ACCUM_EH,0x0f
_OS_HAL_TIMER_COMPUTE_IOREGS__PRESCALER_LOOP:
	SBRS TEMP_L,0x00
	RJMP _OS_HAL_TIMER_COMPUTE_IOREGS__PRESCALER_SHIFT
	INC RESULT
	CP ACCUM_EL,C0x00										;Если кол-во тиков помещается в 16 бит и таймер 16 бит - цикл завершен
	BRTC PC+0x03
	BREQ _OS_HAL_TIMER_COMPUTE_IOREGS__LOOP_DONE
	RJMP _OS_HAL_TIMER_COMPUTE_IOREGS__PRESCALER_SHIFT
	CP ACCUM_H,C0x00
	CPC ACCUM_EL,C0x00
	BREQ _OS_HAL_TIMER_COMPUTE_IOREGS__LOOP_DONE					;Если кол-во тиков помещается в 8 бит - цикл завершен
_OS_HAL_TIMER_COMPUTE_IOREGS__PRESCALER_SHIFT:
	;Сдвигаем биты периода
	LSR ACCUM_EL
	ROR ACCUM_H
	ROR ACCUM_L
	;Сдвигаем биты делителя
	LSR TEMP_H
	ROR TEMP_L
	;Декремент счетчика бит делителя
	DEC ACCUM_EH
	BRNE _OS_HAL_TIMER_COMPUTE_IOREGS__PRESCALER_LOOP
_OS_HAL_TIMER_COMPUTE_IOREGS__LOOP_DONE:
	POP_T16
	CLT														;Восстанавливаем флаг T
	RET
.ENDIF
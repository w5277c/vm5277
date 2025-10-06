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

.include "dmem/dram.asm"
.include "math/mul16.asm"

.IFNDEF J8BPROC_ARR_REFCOUNT_INC
;-----------------------------------------------------------
J8BPROC_ARR_REFCOUNT_INC:
;-----------------------------------------------------------
;Инкремент количества ссылок массива
;IN: X-адрес заголовка массива
;-----------------------------------------------------------
	PUSH ACCUM_L
	ADIW XL,0x01
	LD ACCUM_L,X
	CPI ACCUM_L,0xFF
	BRNE PC+0x02
	RJMP _J8BPROC_ARR_REFCOUNT__END
	INC ACCUM_L
	ST X,ACCUM_L
	SBIW XL,0x01
	RJMP _J8BPROC_ARR_REFCOUNT__END
;-----------------------------------------------------------
J8BPROC_ARR_REFCOUNT_DEC_CONST:
;-----------------------------------------------------------
;Декремент количества ссылок массива
;IN: X-адрес заголовка массива, ACCUM_L/H-размер
;-----------------------------------------------------------
	PUSH ACCUM_EL
	ADIW XL,0x01
	LD ACCUM_EL,X
	CPI ACCUM_EL,0xFF
	BRNE PC+0x02
	RJMP _J8BPROC_ARR_REFCOUNT__END
	DEC ACCUM_EL
	ST X,ACCUM_EL
	BREQ PC+0x02
	RJMP _J8BPROC_ARR_REFCOUNT__END
	PUSH ZL
	PUSH ZH
	MOVW ZL,XL
	MCALL OS_DRAM_FREE
	SBIW XL,0x01
	POP ZL
	POP ZH
	POP ACCUM_EL
	RET
;-----------------------------------------------------------
J8BPROC_ARR_REFCOUNT_DEC:
;-----------------------------------------------------------
;Декремент количества ссылок массива
;IN: X-адрес заголовка массива
;-----------------------------------------------------------
	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH XL
	PUSH XH
	ADIW XL,0x01
	LD ACCUM_L,X
	CPI ACCUM_L,0xFF
	BREQ _J8BPROC_ARR_REFCOUNT__END
	DEC ACCUM_L
	ST X,ACCUM_L
	BRNE _J8BPROC_ARR_REFCOUNT__END
	;Освобождение ресурса
	PUSH RESULT
	PUSH FLAGS
	SBIW XL,0x01
	LD RESULT,X												;Загружаю глубину массива
	MOV FLAGS,RESULT										;Загружаю размер типа
	ANDI RESULT,0x03
	PUSH RESULT
	SWAP FLAGS
	ANDI FLAGS,0x03
	ADIW XL,0x02
	ADIW XL,0x03
	LD ACCUM_L,X+											;Загружаю первую размерность
	LD ACCUM_H,X+
	TST RESULT
	BREQ _J8BPROC_ARR_REFCOUNT__SKIP_LOOP
	PUSH ACCUM_EL
	PUSH ACCUM_EH
_J8BPROC_ARR_REFCOUNT__LOOP:								;Умножаю на последующие размерности(если есть)
	LD ACCUM_EL, X+
	LD ACCUM_EH, X+
	MCALL OS_MUL16
	DEC RESULT
	BRNE _J8BPROC_ARR_REFCOUNT__LOOP
	POP ACCUM_EH
	POP ACCUM_EL
_J8BPROC_ARR_REFCOUNT__SKIP_LOOP:

	TST FLAGS
	BREQ _J8BPROC_ARR_REFCOUNT__COMPTOTAL_END
_J8BPROC_ARR_REFCOUNT__COMPTOTAL_LOOP:						;Умножаю на размер ячейки
	LSL ACCUM_L
	ROL ACCUM_H
	DEC FLAGS
	BRNE _J8BPROC_ARR_REFCOUNT__COMPTOTAL_LOOP
_J8BPROC_ARR_REFCOUNT__COMPTOTAL_END:
	POP RESULT												;Добавляем размер заголовка
	LSL RESULT
	SUBI RESULT,low(0x100-0x05)
	ADD ACCUM_L,RESULT
	ADC ACCUM_H,C0x00

	PUSH ZL
	PUSH ZH
	MOVW ZL,XL
	MCALL OS_DRAM_FREE
	POP ZH
	POP ZL

	POP FLAGS
	POP RESULT
	POP XH
	POP XL
	POP ACCUM_H
_J8BPROC_ARR_REFCOUNT__END:
	POP ACCUM_L
	RET
.ENDIF

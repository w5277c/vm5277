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

//TODO при работе с массивами большая часть кода дублируется, нужно вынести в отдельную функцию

.include "math/mul16.asm"

.IFNDEF J8BPROC_ARR_CELLADDR
;-----------------------------------------------------------
J8BPROC_ARR_CELLADDR:
;-----------------------------------------------------------
;Вычисляет адрес ячейки массива
;IN: X-адрес заголовка массива, индексы расположены в стеке
;OUT: X-адрес на ячейку
;-----------------------------------------------------------
	PUSH YL
	PUSH YH
	LDS YL,SPL
	LDS YH,SPH

	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH ACCUM_EL
	PUSH ACCUM_EH
	PUSH ZL
	PUSH ZH
	PUSH FLAGS
	PUSH RESULT

	LD RESULT,X												;Глубина и флаги массива
	MOV FLAGS,RESULT										;Для определения размера ячейки
	ANDI RESULT,0x03

	MOVW ZL,XL
	ADIW ZL,0x03											;dim0(из 0-2)

	MOV ACCUM_EL,RESULT										;Учитываем динамический размер заголовка(dim0:5байт,dim1:7байт,dim2:9байт)
	LSL ACCUM_EL
	ADD XL,ACCUM_EL
	ADC XH,C0x00

	;Вычисляем адрес ячейки с учетом глубины, размерностей и индексов
	ADIW YL,0x02+0x02										;index0(из 0-2, 0x02-количество PUSH)

	LDD ACCUM_L,Y+0x00										;index0
	LDD ACCUM_H,Y+0x01
	CPI RESULT,0x00
	BREQ _J8BPROC_ARR_CELLADDR__COMPINDEX_END

	LDD ACCUM_EL,Z+0x02										;dim1
	LDD ACCUM_EH,Z+0x03
	RCALL OS_MUL16
	CPI RESULT,0x01
	BRNE _J8BPROC_ARR_CELLADDR__COMPINDEX_3D
	LDD ACCUM_EL,Y+0x02										;index1
	ADD ACCUM_L,ACCUM_EL
	LDD ACCUM_EH,Y+0x03
	ADC ACCUM_H,ACCUM_EH
	RJMP _J8BPROC_ARR_CELLADDR__COMPINDEX_END
_J8BPROC_ARR_CELLADDR__COMPINDEX_3D:
	LDD ACCUM_EL,Z+0x04										;dim2
	LDD ACCUM_EH,Z+0x05
	RCALL OS_MUL16
	PUSH ACCUM_H
	PUSH ACCUM_L
	LDD ACCUM_L,Y+0x02										;index1
	LDD ACCUM_H,Y+0x03
	LDD ACCUM_EL,Z+0x04										;dim2
	LDD ACCUM_EH,Z+0x05
	RCALL OS_MUL16
	POP ACCUM_EL
	ADD ACCUM_L,ACCUM_EL
	POP ACCUM_EH
	ADC ACCUM_H,ACCUM_EH
	LDD ACCUM_EL,Y+0x04										;index2
	ADD ACCUM_L,ACCUM_EL
	LDD ACCUM_EH,Y+0x05
	ADC ACCUM_H,ACCUM_EH
_J8BPROC_ARR_CELLADDR__COMPINDEX_END:

	SWAP FLAGS												;Умножаю на размер ячейки(если не byte)
	ANDI FLAGS,0x03
	BREQ _J8BPROC_ARR_CELLADDR__COMPTOTAL_END
_J8BPROC_ARR_CELLADDR__COMPTOTAL_LOOP:
	LSL ACCUM_L
	ROL ACCUM_H
	DEC FLAGS
	BRNE _J8BPROC_ARR_CELLADDR__COMPTOTAL_LOOP
_J8BPROC_ARR_CELLADDR__COMPTOTAL_END:

	SUBI ACCUM_L,low(-0x03)
	SBCI ACCUM_H,high(-0x03)
	ADD XL,ACCUM_L
	ADC XH,ACCUM_H

	POP RESULT
	POP FLAGS
	POP ZH
	POP ZL
	POP ACCUM_EH
	POP ACCUM_EL
	POP ACCUM_H
	POP ACCUM_L
	POP YH
	POP YL
	RET
.ENDIF

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
;Включаем бит по в ячейке расположенной по адресу 0-255
;-----------------------------------------------------------------------------------------------------------------------
.IFNDEF REG_BIT_HI
;-----------------------------------------------------------
REG_BIT_HI:
;-----------------------------------------------------------
;Включаем бит регистра
;IN: Z-адрес,ACCUM_L-число бита
;-----------------------------------------------------------
	PUSH_T16

	LDS TEMP_H,SREG
	CLI
	LD TEMP_L,Z
	OR TEMP_L,ACCUM_L
	ST Z,TEMP_L
	STS SREG,TEMP_H

	POP_T16
	RET
.ENDIF

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

;-----------------------------------------------------------------------------------------------------------------------
.IFNDEF REG_BIT_GET
;-----------------------------------------------------------
REG_BIT_GET:
;-----------------------------------------------------------
;Получаем бит регистра
;IN: Z-адрес,ACCUM_L-число бита
;OUT: Флаг C-состояние пина (C-true=HI)
;-----------------------------------------------------------
	PUSH TEMP_L

	LD TEMP_L,Z
	AND TEMP_L,ACCUM_L
	ADD TEMP_L,C0xff

	POP TEMP_L
	RET
.ENDIF

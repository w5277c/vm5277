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

;Структура данных в стеке
;2Б	-регистровая пара Z
;2Б	-адрес возврата											;SP установлен после адреса

.IFNDEF J8BPROC_MFIN
;-----------------------------------------------------------
J8BPROC_MFIN:
;-----------------------------------------------------------
;Завершает работу метода
;-----------------------------------------------------------
	CLI
	POP _SPH												;Восстанавливаем точку возврата
	POP _SPL
	POP ZH													;Восстанавливаем Z
	POP ZL
	PUSH _SPL												;Помещаем адрес возврата
	PUSH _SPH
	SEI
	RET														;Возврат
.ENDIF

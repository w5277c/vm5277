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

/* Если МК не поддерживает GPIO на RST пине,
   то перед сборкой верхний уровень должен задать константы PORT_BLDR, DDR_BLDR, PIN_BLDR, MASK_BLDR
   При поддержке GPIO константы должны быть заданы в заголовочном фале чипа
 */

.ORG BOOT_256W_ADDR
	.EQU	BOOTLOADER__BUFFER_SIZE					= 0x01+0x02+FLASH_PAGESIZE+0x01	;CMD+ADDR+DATA+XORSUM
;-----------------------------------------------------------
BOOTLOADER_START:											;Необходим включеный fuse:BOOTRST(запуск будет с этой точки)
;-----------------------------------------------------------
;Точка входа в загрузчик
;-----------------------------------------------------------
	CLI
	RJMP _BOOTLOADER__CONST_SKIP							;Пропускаем блок текстовых констант
	.dq 0x7752ffffffffffff
_BOOTLOADER__CONST_WELCOME_STR:
	.db "BR00>",'\n'										;Приветствие
_BOOTLOADER__CONST_ERR_STR:
	.db "E",'\n'											;Ошибка - ошибка в обработке команды (сбой по UART, не корректные данные или не сошлась XOR сумма)
_BOOTLOADER__CONST_OK_STR:
	.db "O",'\n'											;ОК - команда успешно выполнена
_BOOTLOADER__CONST_EQ_STR:
	.db "S",'\n'											;SKIP - данные в запросе на запись страницы идентичны данным в странице

_BOOTLOADER__CONST_SKIP:
.IFDEF SPH
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-0x01)				;Инициализация стека
	STS SPH,ACCUM_L
.ENDIF
	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-0x01)
	STS SPL,ACCUM_L

	LDI_Z 0x0000											;Проверка наличия прошивки программы, если 0xffff то прошивка не обнаружена
	LPM TEMP_L,Z+
	CPI TEMP_L,0xff
	BRNE _BOOTLOADER__PROGRAMM_DETECTED
	LPM TEMP_L,Z
	CPI TEMP_L,0xff
	BREQ _BOOTLOADER__WELCOME								;Если прошивки нет, то переходим сразу в режим бутлоадера
_BOOTLOADER__PROGRAMM_DETECTED:
															
	CBI DDR_BLDR,PINNUM_BLDR								;Гарантируем режим входа на пине
															;Выполняем проверку низкого уровня на пине (должен быть подтянут к VCC через внешний резистор)
	LDI TEMP_EL,12											;Где-то 1с для 4МГц и 200мс для 20Мгц (необходимо проверить)
	LDI TEMP_H,53											;200мс нужно чтобы не пропустить код нажатой кнопки к склавиатуры хоста
	LDI TEMP_L,0
_BOOTLOADER__LOWLEVELDETECTOR_LOOP:
	SBIS PIN_BLDR,PINNUM_BLDR
	RJMP _BOOTLOADER__WELCOME								;Переход на бутлоадер, если обнаружен низкий уровень
	SUBI TEMP_L,0x01
	SBCI TEMP_H,0x00
	SBCI TEMP_EL,0x00
	BRCC _BOOTLOADER__LOWLEVELDETECTOR_LOOP
	JMP 0x0000												;Переход на программу, если низкий уровень не был обнаружен

_BOOTLOADER__WELCOME:
	RCALL BOOTLOADER__OUT_MODE								;Режим вывода на пине UART
	LDI_Z _BOOTLOADER__CONST_WELCOME_STR*2					;Выводим welcome сообщение
	SET
	RCALL BOOTLOADER_UART_SEND

_BOOTLOADER__LOOP:											;Основной цикл бутлоадера (обработчик команд)
	RCALL BOOTLOADER_LISTEN									;Вызываем процедуру чтения с UART (выйдет только с полученными данными)
	RCALL BOOTLOADER__OUT_MODE								;Режим вывода на пине UART
	
	LDS ACCUM_L,SRAM_START									;Считываем первый байт - команду
	
	CPI XH,0x00												;Проверка на размер полученных данных 
	BRNE _BOOTLOADER__CMD_NOT_1BYTECMD						;Минимальный размер 1 байт - CMD (XORSUM проверили и отбросили в BOOTLOADER_LISTEN)
	CPI XL,0x01
	BRNE _BOOTLOADER__CMD_NOT_1BYTECMD

	CPI ACCUM_L,'i'											;Показываем версию еще раз выводя приветствие
	BRNE _BOOTLOADER__CMD_NOT_INFO
	LDI_Z 0x0000
	LDI ACCUM_L,(1<<SIGRD)|(1<<SPMEN)						;Читаем 3 байта сигнатуры MCU
	STS SPMCSR,ACCUM_L
	LPM TEMP_L,Z+
	STS SPMCSR,ACCUM_L
	LPM TEMP_H,Z+
	STS SPMCSR,ACCUM_L
	LPM TEMP_EL,Z+
	LDI_Z (BOOT_256W_ADDR+0x02)*2
	LDI_Y SRAM_START
	LDI ACCUM_L,0x08										;В цикле помещаем в ответ 8 байт UID
_BOOTLOADER__CMD_INFO__LOOP:
	LPM TEMP_EH,Z+
	ST Y+,TEMP_EH
	DEC ACCUM_L
	BRNE _BOOTLOADER__CMD_INFO__LOOP
	ST Y+,c0x00												;Тип платформы - 0x00=AVR
	ST Y+,TEMP_L											;Три байта сигнатуры чипа
	ST Y+,TEMP_H
	ST Y+,TEMP_EL
	LDI ACCUM_L,'\n'										;Перевод строки - конец ответа
	ST Y+,ACCUM_L
	CLT														;Передаем данные с RAM
	RCALL BOOTLOADER_UART_SEND
	RJMP _BOOTLOADER__LOOP
	
_BOOTLOADER__CMD_NOT_INFO:
	CPI ACCUM_L,'g'											;Переходим на код программы
	BRNE _BOOTLOADER__CMD_ERROR
	CBI PORT_BLDR,PINNUM_BLDR								;Из измененного критична только конфигурация пина, возвращаем назад
	CBI DDR_BLDR,PINNUM_BLDR
	JMP 0x0000												;Выполняем переход

_BOOTLOADER__CMD_ERROR:										;Общая точка вывода ошибки
	LDI_Z _BOOTLOADER__CONST_ERR_STR*2						;Выводим ошибку
	SET
	RCALL BOOTLOADER_UART_SEND
	RJMP _BOOTLOADER__LOOP									;Повторяем цикл заново

_BOOTLOADER__CMD_NOT_1BYTECMD:								;Обработка команд размером более 1 байта
	CPI ACCUM_L,'w'											;Пока только команда записи страницы
	BRNE _BOOTLOADER__CMD_ERROR								;Переходим на вывод ошибки если команда отличается

	CPI XH,high(0x01+0x02+FLASH_PAGESIZE)					;Проверка корректной длины принятых данных (без учета XORSUM)
	BRNE _BOOTLOADER__CMD_ERROR
	CPI XL,low(0x01+0x02+FLASH_PAGESIZE)
	BRNE _BOOTLOADER__CMD_ERROR

	LDD XL,Y+0x01											;Более не нужна информация о длине из приянтых данных, считываем адрес страницы 
	LDD XH,Y+0x02
	MOV ACCUM_L,XL
	ANDI ACCUM_L,low(~FLASH_PAGESIZE)						;Проверяем что адрес выровнен по странице
	BRNE _BOOTLOADER__CMD_ERROR

	CPI XH,high(FLASH_SIZE-FLASH_PAGESIZE-512)				;Проверяем что запись не затронет бутлоадер
	BRCS PC+0x04
	BRNE _BOOTLOADER__CMD_ERROR
	CPI XL,low(FLASH_SIZE-FLASH_PAGESIZE-512)
	BRCC _BOOTLOADER__CMD_ERROR
															;Проверяем идентичность данных страницы с принятыми данными
	LDI_Y SRAM_START+0x03									;Смещаемся на данные
	LDI TEMP_EL,FLASH_PAGESIZE								;Максимум страница 256 байт, умещаемся в 8 бит регистр
_BOOTLOADER_CHECK_FLASHPAGE__LOOP:
	LPM TEMP_L,Z+											;Считываем байт с FLASH, инкрементируем индесный регистр
	LD TEMP_H,Y+											;Считываем байт с принятого блока, инкрементируем индесный регистр
	CP TEMP_L,TEMP_H										;Сверяем
	BRNE _BOOTLOADER__WRITECMD_WRITE_PAGE					;Данные отличаются, необходима запись
	DEC TEMP_EL												;Декрементируем счетчик
	BRNE _BOOTLOADER_CHECK_FLASHPAGE__LOOP					;Повторяем цикл для всех байт
	LDI_Z _BOOTLOADER__CONST_EQ_STR*2						;Данные идентичны - выводим сообщение
	SET
	RCALL BOOTLOADER_UART_SEND
	RJMP _BOOTLOADER__LOOP									;Повторяем основной цикл заново

_BOOTLOADER__WRITECMD_WRITE_PAGE:							;Выполняем запись страницы
	LDI_Y SRAM_START+0x03									;Еще раз смещаемся на данные
	MOVW ZL,XL												;Помещаем в Z адрес страницы
	RCALL BOOTLOADER__FLASHPAGE_WRITE						;Основная процедура записи
	LDI_Z _BOOTLOADER__CONST_OK_STR*2						;Выводим сообщение об успешной записи
	SET
	RCALL BOOTLOADER_UART_SEND
	RJMP _BOOTLOADER__LOOP									;Повторяем основной цикл заново
	
;-----------------------------------------------------------
BOOTLOADER_LISTEN:
;-----------------------------------------------------------
;Слушаем UART - ожидаем данные, принимаем и обрабатываем
;OUT: X-длина данных
;-----------------------------------------------------------
	SBI PORT_BLDR,PINNUM_BLDR								;Режим приема пина UART
	CBI DDR_BLDR,PINNUM_BLDR

	CLT														;Флаг t=false, если включен - ошибка переполнения буфера
	LDI_Y SRAM_START										;Устанавливаем адрес буфера
	LDI_X 0x0000											;Длина принятых данных
	LDI ACCUM_H,0x00										;Счетчик XOR суммы
	LDI TEMP_L,0x2a											;Таймаут ожидания START в ~2 бита

_BOOTLOADER_LISTEN__START_LOOP:								;Ждем START
	SBIS PIN_BLDR,PINNUM_BLDR								;Если низкий уровень на пине, значит обнаружен START
	RJMP BOOTLOADER_LISTEN__GOT_START
	CPI XH,0x00												;Проверка на нулевую длину
	BRNE PC+0x03
	CPI XL,0x00
	BREQ _BOOTLOADER_LISTEN__START_LOOP						;При длине=0 бесконечно ждем старт
	DEC TEMP_L												;Уменьшаем таймаут
	BRNE _BOOTLOADER_LISTEN__START_LOOP						;Повторяем цикл
	BRTS BOOTLOADER_LISTEN									;Таймаут, но если была ошибка - слушаем заново
	
															;Длина не нулевая, получен таймаут и не было ошибки, завершаем прослушивание
	CPI XH,0x00												;Длина вместе с XORSUM не может быть меньше 2 байт
	BRNE PC+0x02
	CPI XL,0x01
	BREQ BOOTLOADER_LISTEN									;Слушаем заново, если длина оказалась в 1 байт
	CPI ACCUM_H,0x00										;Сумма XOR вместе с самим XORSUM на конце при корректных данных всегда будет 0x00
	BRNE BOOTLOADER_LISTEN									;Иначе ошибка в данных, слушаем заново
	SBIW XL,0x01											;Вычитаем из длины байт (XORSUM)
	RET														;Данные получены, проверки упешно пройдены, выходим
	
BOOTLOADER_LISTEN__GOT_START:								;Получен START
	LDI TEMP_EH,0x06										;Пауза ~1/3 бита
	DEC TEMP_EH
	BRNE PC-0x01

	LDI TEMP_L,0x08											;Цикл в 8 бит (8n0)
BOOTLOADER_LISTEN__BITLOOP:
	RCALL BOOTLOADER_UART_WAIT								;Пауза в 1 бит, также сбрасывает флаг C
	SBIC PIN_BLDR,PINNUM_BLDR								;Передаем бит с UART в флаг C
	SEC
	ROR ACCUM_L												;Записываем флаг в аккумулятор используя битовый сдвиг
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE BOOTLOADER_LISTEN__BITLOOP							;Повторяем для 8 итераций

	RCALL BOOTLOADER_UART_WAIT								;Пауза в 1 бит
	SBIS PIN_BLDR,PINNUM_BLDR								;Проверяем STOP
	RJMP BOOTLOADER_LISTEN									;STOP не получен, начинаем слушать заново

	SET														;Заранее включаем флаг T(выключим при успешной проверке)
	CPI XH,high(BOOTLOADER__BUFFER_SIZE)					;Проверка на достижение максимальной длины - игнорируем последующие данные
	BRCS _BOOTLOADER_LISTEN__NEXT							;Переполнения нет - переходим к блоку записи байта и переходе к следующей итерации
	BRNE _BOOTLOADER_LISTEN__START_LOOP						;Есть переполнение - переходим к следующей итерации оставив флаг T включенным и игнорируем запись байта
	CPI XL,low(BOOTLOADER__BUFFER_SIZE)
	BRCS _BOOTLOADER_LISTEN__NEXT
	RJMP _BOOTLOADER_LISTEN__START_LOOP

_BOOTLOADER_LISTEN__NEXT:									;Блок записи байта
	CLT														;Сбрасываем флаг T
	ST Y+,ACCUM_L											;Записываем байт
	EOR ACCUM_H,ACCUM_L										;Учитываем XORSUM
	ADIW XL,0x01											;Инкрементируем счетчик длины данных
	RJMP _BOOTLOADER_LISTEN__START_LOOP						;Переходим к следующей итерации

;-----------------------------------------------------------
BOOTLOADER__OUT_MODE:										;Процедура режима вывода пина UART
;-----------------------------------------------------------
;Режим вывода UART
;-----------------------------------------------------------
	SBI PORT_BLDR,PINNUM_BLDR
	SBI DDR_BLDR,PINNUM_BLDR
	RJMP BOOTLOADER_UART_WAIT

;-----------------------------------------------------------
BOOTLOADER_UART_SEND:										;Процедура вывода в UART (XORSUM в ответах не применяется)
;-----------------------------------------------------------;
;Передача FLASH данных по UART
;IN: Z-адрес текстовой константы (T=1 из FLASH, T=0 из SRAM)
;Конец данных определяется как '\n'
;-----------------------------------------------------------
	BRTS PC+0x02
	LPM ACCUM_L,Z+											;Считываем байт в аккумулятор и инкрементируем индексный регистр
	BRTC PC+0x02
	LD ACCUM_L,Z+											;Считываем байт в аккумулятор и инкрементируем индексный регистр

	MOV ACCUM_H,ACCUM_L										;Копируем для проверки на выход
	
	CBI PORT_BLDR,PINNUM_BLDR								;Выставляем START
	RCALL BOOTLOADER_UART_WAIT

	LDI TEMP_L,0x08											;Цикл передачи байта побитно
_BOOTLOADER_UART_SEND__BITLOOP:
	SBRC ACCUM_L,0x00										;В зависимости от текущего бита задаем на пине высокий или низкий уровень
	SBI PORT_BLDR,PINNUM_BLDR
	SBRS ACCUM_L,0x00
	CBI PORT_BLDR,PINNUM_BLDR
	LSR ACCUM_L												;Сдвигаем биты в аккумуляторе
	
	NOP														;Строго выдерживаем паузу
	RCALL BOOTLOADER_UART_WAIT
	DEC TEMP_L												;Декркментируем счетчик бит
	BRNE _BOOTLOADER_UART_SEND__BITLOOP						;Выполняем все 8 итераций

	SBI PORT_BLDR,PINNUM_BLDR								;Выставляем STOP
	RCALL BOOTLOADER_UART_WAIT

	CPI ACCUM_H,'\n'										;Проверка на конец данных
	BRNE BOOTLOADER_UART_SEND								;Повторяем основную итерацию если не обнаружен '\n'
	RET														;Все передали - выходим

;-----------------------------------------------------------
BOOTLOADER_UART_WAIT:										;Процедура выдержки паузы для программного UART
;-----------------------------------------------------------
;Пауза между битами
;-----------------------------------------------------------
	LDI TEMP_EH,0x13
	SUBI TEMP_EH,0x01
	BRNE PC-0x01
	RET

;-----------------------------------------------------------
BOOTLOADER__FLASHPAGE_WRITE:
;-----------------------------------------------------------
;Блок процедуры записи страницы FLASH
;IN: Y-адрес данных в RAM, Z-адрес страницы в FLASH
;-----------------------------------------------------------
	LDI ACCUM_L,(1<<PGERS)|(1<<SPMEN)
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP
	LDI ACCUM_L,(1<<RWWSRE)|(1<<SPMEN)
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP

	PUSH_Z
	LDI TEMP_L,FLASH_PAGESIZE/2
	LDI ACCUM_L,(1<<SPMEN)
_BOOTLOADER__FLASHPAGE_WRITE__WLOOP:
	LD r0,Y+
	LD r1,Y+
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP
	ADIW ZL,0x02
	DEC TEMP_L
	BRNE _BOOTLOADER__FLASHPAGE_WRITE__WLOOP
	POP_Z
	
	LDI ACCUM_L,(1<<PGWRT)|(1<<SPMEN)
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP
	LDI ACCUM_L,(1<<RWWSRE)|(1<<SPMEN)
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP

_BOOTLOADER__FLASHPAGE_WRITE__RWWLOOP:
	LDS ACCUM_L,SPMCSR
	SBRS ACCUM_L,RWWSB
	RET
	LDI ACCUM_L,(1<<RWWSRE)|(1<<SPMEN)
	RCALL _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP
	RJMP _BOOTLOADER__FLASHPAGE_WRITE__RWWLOOP

;-----------------------------------------------------------
_BOOTLOADER__FLASHPAGE_WRITE__DO_SMP:
;-----------------------------------------------------------
;Выполняю команду SPM
;IN: Z-адрес FLASH,r1/r0-слово, ACCUM_L-значение SPMCSR
;-----------------------------------------------------------
_BOOTLOADER__FLASHPAGE_WRITE__DO_SMP_WAIT_SPM:
	LDS TEMP_L,SPMCSR
	SBRC TEMP_L,SPMEN
	RJMP _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP_WAIT_SPM

_BOOTLOADER__FLASHPAGE_WRITE__DO_SMP_WAIT_EE:
	LDS TEMP_L,EECR
	SBRC TEMP_L,EEPE
	RJMP _BOOTLOADER__FLASHPAGE_WRITE__DO_SMP_WAIT_EE

	STS SPMCSR,ACCUM_L
	SPM
	RET


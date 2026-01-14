## Создание первого проекта

>Для разработчиков, знакомых с Arduino:  
VM5277 позволяет писать код на Java-подобном языке (J8B) и компилировать его под микроконтроллеры (например, AVR ATmega328) без Arduino IDE.  
Ниже — два способа создания проекта: через Maven (автоматически) или вручную (для лучшего понимания).  
Позже вы сможете работать в NetBeans IDE — это упростит процесс сборки и отладки.


### 1. Автоматическое создание через Maven-плагин
* Выполните стандартный скрипт создания maven проекта:
	```bash
	mvn archetype:generate -DarchetypeGroupId=ru.vm5277 -DarchetypeArtifactId=j8b-archetype -DinteractiveMode=false -DgroupId=your_group_id -DartifactId=new_project
	```
* Перейдите в директорию созданного проекта:
	```bash
	cd new_project
	```
* Отредактируйте файл `pom.xml` указав необходимые параметры:
   - `<j8b.target>` — платформа:чип, пример `avr:atmega328p`
   - `<j8b.targetFreq>` — тактовая частота чипа в МГц, пример `16` (также можно указать нецелочисленные значения - `9.6`)

	```bash
	nano pom.xml
	```

* Посмотрите исходный файл проекта и внесите изменения по желанию:
	```bash
	nano src/main/j8b/Main.j8b
	```
* Выполните очистку и затем сборку проекта
	```bash
	mvn clean
	mvn compile
	```

### 2. Создание вручную (рекомендуется для детального ознакомления)
* Создаем каталог нового проекта и перейдем в него
	```bash
	mkdir new_project
	cd new_project
	```
* Создаем основной файл и напишем первую программу
	```bash
	nano Main.j8b
	```
	Пример программы:
	```java
	class Main {
		public static void main() {
			System.setParam(RTOSParam.STDOUT_PORT, GPIO.PC2);
			cstr text="READY!\n";
			System.out(text);
		}
	}
	```
	
* Пробуем ее скомпилировать
	```bash
	j8bc.sh avr:atmega328p Main.j8b -F 16
	```
	> Опции утилиты и подробную информацию см. в документе [Компилятор J8B](compiler.md)

	Пример вывода:
	```bash
	;j8b compiler: Java-like source code compiler for vm5277 Embedded Toolkit
	;Version: 0.5.0 | License: Apache-2.0
	;================================================================
	;WARNING: This project is under active development.
	;The primary focus is on functionality; testing is currently limited.
	;It is strongly recommended to carefully review the generated
	;assembler code before flashing the device.
	;Please report any bugs found to: konstantin@5277.ru
	;================================================================
	Toolkit path: /home/kostas/vm5277
	Parsing /home/kostas/1/new_project/Main.j8b ...
	Parsing done, time:0.067 s
	Semantic...
	Semantic done, time:0.014 s
	Codegen...
	Codegen done, time:0.020 s
	Total time:0.155 s
	```
	
	Если ошибок не обнаружено, то в директории нашего проекта должна была появиться директория `target`:
	```bash
	target/
	├── Main.asm
	└── Main.dbg
	```

	Мы успешно получили файл ассемблера для avr - `Main.asm` и файл с информацией для отладки (пока он нам не нужен)  
	Мы можем посмотреть файл ассемблера и внести при желании правки
	```bash
	nano target/Main.asm
	```
	Как мы видим в файле ассемблера уже указан наш микроконтроллер и его тактовая частота:
	* `.include "devices/atmega328p.def"`
	* `.equ CORE_FREQ = 16`
 
	Теперь наша задача собрать файл ассемблера в hex прошивку для микроконтроллера
* Используем AVR ассемблер
	```bash
	avrasm.sh target/Main.asm -I rtos/avr/
	```
	>Необходимо указывать директорию RTOS тулкита, чтобы исключить конфликты при использовании avr ассемблера для проектов не использующих RTOS  
	>Опции утилиты и подробную информацию см. в документе [Ассемблер AVR](avrasm.md)  
	
	Пример вывода:
	```bash
	;j8b AVR Assembler for vm5277 Embedded Toolkit
	;Version: 0.3.0 | License: Apache-2.0
	;================================================================
	;WARNING: This project is under active development.
	;The primary focus is on functionality; testing is currently limited.
	;Please report any bugs found to: konstantin@5277.ru
	;================================================================
	Toolkit path: /home/kostas/vm5277
	Parsing /home/kostas/1/new_project/target/Main.asm ...
	INF|38:14 /home/kostas/vm5277/rtos/avr/core/core.asm		######## IO BAUDRATE:230400

	--CODE-----------------------------------
	Start	= 0000, End = 0094, Length = 0095
	-----
	Total	:  149 words (298 bytes) 0.9%
	
	parsed: 0 lines
	Build SUCCESS, warnings:0
	Total time: 0.130 s
	```

	Если ошибок не обнаружено, то в директории `targe`t появится новый файл `Main_cseg.hex`

	**Не важно какой способ мы выберем, при успешной сборке мы получим в каталоге `target` основные файлы**:
	```bash
	target/
	├── Main.asm
	├── Main_cseg.hex
	└── Main.dbg
	```

	На этом этапе можно смело утверждать, что наш проект не только создан но и отстроен - получен файл прошивки, который можно прошить стандартным инструментарием или инструментарием VM5277

Следующий шаг: [Прошивка микроконтроллера](flashing.md)
	
[← Назад к содержанию](index.md)

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


// !Сгенерировано нейронкой!

package ru.vm5277.tools.fontRotate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	//public	final	static	String					VERSION				= StrUtils.readVersion(Main.class);
	public			static	boolean					isWindows;
	
	public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Использование: java FontRotator <входной_файл> <выходной_файл>");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try {
            rotateFontFile(inputFile, outputFile);
            System.out.println("Конвертация успешно завершена! Файл сохранен в: " + outputFile);
        } catch (Exception e) {
            System.err.println("Ошибка при конвертации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void rotateFontFile(String inputPath, String outputPath) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        }

        String content = fileContent.toString();

        // Ищем блок внутри @FlashData({...})
        Pattern flashDataPattern = Pattern.compile("(@FlashData\\s*\\(\\s*\\{)(.*?)(\\}\\s*\\))", Pattern.DOTALL);
        Matcher matcher = flashDataPattern.matcher(content);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Не найден блок @FlashData({...}) во входном файле.");
        }

        String prefix = matcher.group(1);
        String dataBody = matcher.group(2);
        String suffix = matcher.group(3);

        // Обрабатываем внутренности @FlashData
        String rotatedDataBody = processDataBody(dataBody);

        // Собираем итоговый файл, заменяя только тело данных
        String finalContent = content.substring(0, matcher.start()) 
                + prefix + rotatedDataBody + suffix 
                + content.substring(matcher.end());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(finalContent);
        }
    }

    private static String processDataBody(String dataBody) {
        String[] lines = dataBody.split("\n");
        StringBuilder result = new StringBuilder();
        
        List<String> currentBlockComments = new ArrayList<>();
        List<Integer> currentBlockBytes = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                result.append(line).append("\n");
                continue;
            }

            // Если строка содержит комментарий, запоминаем его
            if (trimmed.startsWith("//")) {
                currentBlockComments.add(line);
                continue;
            }

            // Парсим двоичное число типа 0b00010000, игнорируя запятую
            if (trimmed.startsWith("0b") || trimmed.startsWith("0B")) {
                String binStr = trimmed.substring(2).replace(",", "").trim();
                if (binStr.length() >= 8) {
                    binStr = binStr.substring(0, 8); // берем ровно 8 бит
                    int val = Integer.parseInt(binStr, 2);
                    currentBlockBytes.add(val);
                }

                // Как только накопили 8 строк (матрица одного символа)
                if (currentBlockBytes.size() == 8) {
                    // Разворачиваем матрицу на 90 градусов по часовой стрелке
                    int[] rotated = rotate8x8Cw90(currentBlockBytes);

                    // Пишем сохраненные комментарии для этого символа
                    for (String comment : currentBlockComments) {
                        result.append(comment).append("\n");
                    }

                    // Пишем развернутые байты обратно в формате 0b...
                    for (int i = 0; i < 8; i++) {
                        String binaryString = String.format("%8s", Integer.toBinaryString(rotated[i])).replace(' ', '0');
                        result.append("\t\t0b").append(binaryString);
                        // Добавляем запятую, если это не самый последний байт всего массива (упростим: пишем всегда, кроме финала, если нужно)
                        result.append(",\n");
                    }

                    // Очищаем списки для следующего символа
                    currentBlockComments.clear();
                    currentBlockBytes.clear();
                }
            } else {
                // Любые другие строки (например, пробелы или скобки, если они просочились)
                result.append(line).append("\n");
            }
        }

        // Если файл закончился, а запятая осталась на конце последней строки, 
        // подчистим её аккуратно, чтобы компилятор J8B не ругался на trailing comma (если критично)
        String resStr = result.toString();
        // В J8B trailing comma внутри массива обычно разрешена, так что оставляем.
        return resStr;
    }

    // Алгоритм поворота матрицы 8х8 на 90 градусов по часовой стрелке
    private static int[] rotate8x8Cw90(List<Integer> src) {
        int[] dest = new int[8];
        for (int col = 0; col < 8; col++) {
            int resultOctet = 0;
            for (int row = 0; row < 8; row++) {
                // Для поворота по часовой стрелке:
                // Строка 'row' оригинального шрифта вносится в столбец 'col'
                // Бит берется слева направо (7 - col), а укладывается снизу вверх (row)
                int bit = (src.get(row) >> (7 - col)) & 0x01;
                resultOctet |= (bit << row);
            }
            dest[col] = resultOctet;
        }
        return dest;
    }
}

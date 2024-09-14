import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MonsterFluff {
    String name;
    String source;
    List<String> entries;
    String imagePath;  // Изменили поле с List<Image> на один String

    public static MonsterFluff[] parseMonsterFluffJson(File jsonFile) throws IOException {
        List<MonsterFluff> monsters = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Чтение JSON файла
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        // Извлекаем узел monsterFluff
        JsonNode monsterFluffNode = rootNode.get("monsterFluff");
        if (monsterFluffNode != null && monsterFluffNode.isArray()) {
            for (JsonNode monsterNode : monsterFluffNode) {
                MonsterFluff monster = new MonsterFluff();

                // Парсим поле "name"
                JsonNode nameNode = monsterNode.get("name");
                if (nameNode != null && nameNode.isTextual()) {
                    monster.name = nameNode.asText();
                }

                // Парсим поле "source"
                JsonNode sourceNode = monsterNode.get("source");
                if (sourceNode != null && sourceNode.isTextual()) {
                    monster.source = sourceNode.asText();
                }

                // Парсим "entries" (вложенные структуры)
                JsonNode entriesNode = monsterNode.get("entries");
                monster.entries = new ArrayList<>();
                if (entriesNode != null && entriesNode.isArray()) {
                    extractEntries(entriesNode, monster.entries);
                }

                // Парсим изображения и сохраняем только первое изображение
                JsonNode imagesNode = monsterNode.get("images");
                if (imagesNode != null && imagesNode.isArray()) {
                    for (JsonNode imageNode : imagesNode) {
                        JsonNode hrefNode = imageNode.get("href");
                        if (hrefNode != null && hrefNode.has("path")) {
                            // Убираем "bestiary/" из пути к изображению
                            String imagePath = hrefNode.get("path").asText().replaceFirst("bestiary/", "");
                            monster.imagePath = imagePath;
                        }
                    }
                }

                monsters.add(monster);
            }
        }

        return monsters.toArray(new MonsterFluff[0]);
    }

    // Метод для рекурсивного извлечения всех вложенных записей
    private static void extractEntries(JsonNode node, List<String> entriesList) {
        if (node.isArray()) {
            for (JsonNode entry : node) {
                extractEntries(entry, entriesList);
            }
        } else if (node.isObject() && node.has("entries")) {
            extractEntries(node.get("entries"), entriesList);
        } else if (node.isTextual()) {
            entriesList.add(node.asText());
        }
    }

    public static List<MonsterFluff> processFiles() {
        String directoryPath = "C:\\Users\\SumalyFly\\Downloads\\jsonDescription";  // Путь к папке с файлами
        File folder = new File(directoryPath);

        File[] jsonFiles = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".json") && name.toLowerCase().startsWith("fluff"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("Файлы .json не найдены в указанной папке.");
            return null;
        }

        List<MonsterFluff> npcList = new ArrayList<>();

        for (File jsonFile : jsonFiles) {
            try {
                MonsterFluff[] monsters = parseMonsterFluffJson(jsonFile);
                npcList.addAll(Arrays.asList(monsters));  // Добавляем всех монстров в список
            } catch (IOException e) {
                System.out.println("Ошибка при чтении файла: " + jsonFile.getName());
                e.printStackTrace();
            }
        }

        return npcList;
    }
}

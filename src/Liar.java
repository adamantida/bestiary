import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Liar {
    String name;
    Map<String, List<String>> itemsName;
    List<String> lairActions;
    String description;
    String source;  // Новое поле для хранения источника

    public static Liar[] parseLairJson() throws IOException {
        List<Liar> liars = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Указываем путь к папке с файлами
        String directoryPath = "C:\\Users\\SumalyFly\\Downloads\\jsonbest";
        File folder = new File(directoryPath);
        File[] jsonFilesLiar = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".json") && name.equals("legendarygroups.json")
        );

        if (jsonFilesLiar == null || jsonFilesLiar.length == 0) {
            throw new RuntimeException("Файл legendarygroups.json не найден в указанной папке.");
        }

        File jsonFile = jsonFilesLiar[0];
        JsonNode rootNode = objectMapper.readTree(jsonFile);

        // Извлекаем узел legendaryGroup
        JsonNode legendaryGroupNode = rootNode.get("legendaryGroup");
        if (legendaryGroupNode != null && legendaryGroupNode.isArray()) {
            for (JsonNode group : legendaryGroupNode) {
                Liar liar = new Liar();

                // Проверка наличия поля "name"
                JsonNode nameNode = group.get("name");
                if (nameNode != null && nameNode.isTextual()) {
                    liar.name = nameNode.asText();
                } else {
                    liar.name = "Unnamed";  // Значение по умолчанию, если имя не указано
                }

                // Извлекаем поле "source"
                JsonNode sourceNode = group.get("source");
                if (sourceNode != null && sourceNode.isTextual()) {
                    liar.source = sourceNode.asText();
                } else {
                    liar.source = "Unknown Source";  // Значение по умолчанию, если источник не указан
                }

                // Парсим lairActions
                JsonNode lairActionsNode = group.get("lairActions");
                if (lairActionsNode != null && lairActionsNode.isArray()) {
                    liar.lairActions = new ArrayList<>();
                    for (JsonNode action : lairActionsNode) {
                        if (action.isTextual()) {
                            liar.lairActions.add(action.asText());
                        } else if (action.has("items")) {
                            JsonNode itemsNode = action.get("items");
                            liar.itemsName = new HashMap<>();
                            for (JsonNode item : itemsNode) {
                                String itemName = item.has("name") ? item.get("name").asText() : "Unnamed Item";
                                List<String> entriesList = new ArrayList<>();

                                // Получаем entries, если они есть
                                if (item.has("entries")) {
                                    for (JsonNode entry : item.get("entries")) {
                                        entriesList.add(entry.asText());
                                    }
                                }

                                liar.itemsName.put(itemName, entriesList);
                            }
                        }
                    }
                }

                // Описание (если lairActions присутствуют, генерируем описание)
                if (group.has("lairActions")) {
                    StringBuilder descriptionBuilder = new StringBuilder();
                    for (JsonNode lairAction : lairActionsNode) {
                        if (lairAction.isTextual()) {
                            descriptionBuilder.append(lairAction.asText()).append(" ");
                        }
                    }
                    liar.description = descriptionBuilder.toString().trim();  // Сохраняем описание
                }

                liars.add(liar);
            }
        }

        return liars.toArray(new Liar[0]);
    }
}

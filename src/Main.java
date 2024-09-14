import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;

import java.io.*;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        String directoryPath = "jsonbest";  // Путь к папке с файлами
        File folder = new File(directoryPath);

        // Получаем список всех файлов .json в папке
        File[] jsonFiles = folder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".json") && !name.equalsIgnoreCase("legendarygroups.json")
        );

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("Файлы .json не найдены в указанной папке.");
            return;
        }

        List<NPC> npcList = new ArrayList<>();
        Liar[] liarList;
        try {
            liarList = Liar.parseLairJson();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга JSON: ", e);
        }

        // Обработка каждого файла
        for (File jsonFile : jsonFiles) {
            String filePath = jsonFile.getAbsolutePath();
            List<NPC> loadedNPCs = loadNPCs(filePath, liarList);

            if (loadedNPCs != null) {
                npcList.addAll(loadedNPCs);
            } else {
                System.out.println("Failed to load NPCs from file: " + jsonFile.getName());
            }
        }

        // Удаляем все элементы, которые равны null
        npcList.removeIf(Objects::isNull);

        List<MonsterFluff> MonsterDescr = MonsterFluff.processFiles();

// Сравнение имен и добавление описания к NPC + скачивание изображений
        for (NPC npc : npcList) {
            for (MonsterFluff fluff : MonsterDescr) {
                if (npc.name.equalsIgnoreCase(fluff.name)) {
                    // Добавляем описание к NPC, если имена совпадают
                    String description = String.join(" \\n", fluff.entries);  // Собираем все entries в одну строку
                    npc.description = description;
                    // Скачиваем изображения и устанавливаем пути только при успешном скачивании
                    downloadMonsterImages(npc, fluff);
                    break;  // Выходим из внутреннего цикла, если совпадение найдено
                }
            }
        }


        // Генерация XML документа
        try {
            NPCXmlGenerator generator = new NPCXmlGenerator();
            Document document = generator.createXMLDocument();

            for (int i = 0; i < npcList.size(); i++) {
                generator.addNPC(document, npcList.get(i), i + 1);
            }

            String xmlFileName = "bestiary.xml";
            generator.saveDocument(document, xmlFileName);
            System.out.println("XML-документ создан: " + xmlFileName);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void downloadMonsterImages(NPC npc, MonsterFluff fluff) {
        if (fluff.imagePath != null && !fluff.imagePath.isEmpty()) {
            // Формируем URL для токенов и портретов
            String tokenUrl = "https://5e.tools/img/bestiary/tokens/" + fluff.imagePath;
            String portraitUrl = "https://5e.tools/img/bestiary/" + fluff.imagePath;

            // Папки для сохранения изображений
            String tokenDestination = "tokens/" + fluff.imagePath;
            String portraitDestination = "portraits/" + fluff.imagePath;

            // Скачиваем токены и портреты
            boolean tokenDownloaded = downloadImage(tokenUrl, tokenDestination);
            boolean portraitDownloaded = downloadImage(portraitUrl, portraitDestination);

            if (tokenDownloaded){
                npc.TokenPath = "tokens/" + fluff.imagePath;
            }
            if (portraitDownloaded){
                npc.PortraitPath = "portraits/" + fluff.imagePath;
            }
            // Если хотя бы одно изображение скачалось успешно или уже существует, возвращаем true
        }
    }


    public static boolean downloadImage(String imageUrl, String destinationPath) {
        final int MAX_ATTEMPTS = 1;  // Максимальное количество попыток
        int attempts = 0;

        try {
            // Создаем путь к файлу
            Path destination = Paths.get(destinationPath);

            // Проверяем, существует ли файл. Если да, возвращаем true (файл уже скачан)
            if (Files.exists(destination)) {
                System.out.println("Файл уже существует, пропускаем: " + destinationPath);
                return true;
            }

            // Создаем директорию, если ее нет
            Path parentDir = destination.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Пытаемся скачать изображение
            while (attempts < MAX_ATTEMPTS) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {  // Код 200 означает успешный запрос
                        try (InputStream in = connection.getInputStream()) {
                            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Изображение загружено: " + destinationPath);
                            return true;  // Успешное скачивание
                        }
                    } else if (responseCode == 404) {
                        System.out.println("Ошибка загрузки (файл не найден): " + imageUrl);
                        return false;  // Ошибка 404 — файл не найден
                    } else {
                        throw new IOException("HTTP ошибка: " + responseCode);
                    }

                } catch (IOException e) {
                    attempts++;
                    System.out.println("Ошибка загрузки изображения: " + imageUrl + ". Попытка " + attempts + " из " + MAX_ATTEMPTS);

                    if (attempts >= MAX_ATTEMPTS) {
                        System.out.println("Максимальное количество попыток исчерпано для: " + imageUrl);
                    } else {
                        // Ожидание перед повторной попыткой (например, 2 секунды)
                        try {
                            TimeUnit.SECONDS.sleep(2);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();  // Восстанавливаем статус прерывания
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Ошибка загрузки изображения: " + imageUrl);
        }

        return false;  // Если не удалось скачать
    }


    // Метод загрузки NPC
    private static List<NPC> loadNPCs(String filePath, Liar[] liarList) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> monsters;
        try {
            monsters = (List<Map<String, Object>>) objectMapper.readValue(new File(filePath), Map.class).get("monster");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<NPC> npcList = new ArrayList<>();
        for (Map<String, Object> monster : monsters) {
            npcList.add(parseNPC(monster, liarList));
        }
        return npcList;
    }

    // Метод разбора информации о монстре
    private static NPC parseNPC(Map<String, Object> monster, Liar[] liarList) {
        NPC npc = new NPC();

        // Проверка CR
        if (monster.containsKey("cr")) {
            Object crObj = monster.get("cr");

            if (crObj != null) {
                if (crObj instanceof String) {
                    npc.cr = (String) crObj;
                } else if (crObj instanceof Map<?, ?>) {
                    Object crValue = ((Map<?, ?>) crObj).get("cr");
                    if (crValue != null) {
                        npc.cr = crValue.toString();
                    } else {
                        System.out.println("CR value not found in the map for monster: " + monster.get("name"));
                        return null;
                    }
                } else {
                    System.out.println("Unexpected data type for CR: " + crObj + " for monster: " + monster.get("name"));
                    return null;
                }
            } else {
                System.out.println("CR is null for monster: " + monster.get("name"));
                return null;
            }
        } else {
            System.out.println("CR information not found for monster: " + monster.get("name"));
            return null;
        }

        // Проверка HP
        if (monster.containsKey("hp")) {
            Map<String, Object> hp = (Map<String, Object>) monster.get("hp");

            if (hp != null && hp.containsKey("average") && hp.get("average") != null) {
                Object averageHP = hp.get("average");

                if (averageHP instanceof Integer) {
                    npc.HP = (int) averageHP;
                } else {
                    System.out.println("Unexpected data type for HP 'average': " + averageHP);
                    return null;
                }
            } else {
                System.out.println("HP 'average' not found or is null for monster: " + monster.get("name"));
                return null;
            }
        } else {
            System.out.println("HP information not found for monster: " + monster.get("name"));
            return null;
        }

        // Проверка AC
        if (monster.containsKey("ac")) {
            Object acObject = monster.get("ac");

            if (acObject != null) {
                try {
                    // Проверяем, является ли ac массивом
                    if (acObject instanceof List) {
                        List<?> acList = (List<?>) acObject;
                        if (!acList.isEmpty()) {
                            Object firstAc = acList.get(0);

                            if (firstAc instanceof Integer) {
                                // Если первый элемент — это целое число
                                npc.AC = (Integer) firstAc;
                                if (acList.size() > 1) {
                                    if (acList.get(1) instanceof Map) {
                                        Map<String, Object> acMap = (Map<String, Object>) acList.get(1);

                                        // Проверяем наличие и тип данных для ключа "ac"
                                        if (acMap.get("ac") instanceof Integer) {
                                            npc.AC_description = acMap.get("ac").toString(); // Получаем значение AC
                                        }

                                        // Проверяем наличие "condition" и добавляем его в описание
                                        if (acMap.containsKey("condition")) {
                                            npc.AC_description += " " + acMap.get("condition").toString();
                                        }

                                        // Проверяем наличие "from" и добавляем его в описание
                                        if (acMap.containsKey("from")) {
                                            Object fromObj = acMap.get("from");
                                            if (fromObj instanceof List) {
                                                List<?> fromList = (List<?>) fromObj;
                                                if (!fromList.isEmpty()) {
                                                    npc.AC_description += " from " + fromList.get(0).toString();
                                                }
                                            }
                                        }

                                        System.out.println(acMap);
                                    }
                                }
                            } else if (firstAc instanceof Map) {
                                // Если первый элемент — это объект, ищем ключ "ac"
                                Map<?, ?> acMap = (Map<?, ?>) firstAc;
                                if (acMap.containsKey("ac")) {
                                    npc.AC = extractAC(acMap.get("ac"));
                                    if (acMap.containsKey("from"))
                                        npc.AC_description = acMap.get("from").toString().replace("[", "").replace("]", "");
                                } else {
                                    System.out.println("AC information missing in object for monster: " + monster.get("name"));
                                    return null;
                                }
                            }
                        }
                    } else {
                        // Если ac не массив, обрабатываем как отдельный объект
                        npc.AC = extractAC(acObject);
                    }

                    if (npc.AC < 0) {
                        System.out.println("Invalid AC value for monster: " + monster.get("name"));
                        return null;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error parsing AC value for monster: " + monster.get("name") + ". Exception: " + e.getMessage());
                    return null;
                }
            } else {
                System.out.println("AC information is null for monster: " + monster.get("name"));
                return null;
            }
        } else {
            System.out.println("AC information not found for monster: " + monster.get("name"));
            return null;
        }

        // Проверка Type
        if (monster.containsKey("type")) {
            Object typeObject = monster.get("type");

            if (typeObject != null) {
                npc.type = getType(typeObject);

                if (npc.type == null || npc.type.trim().isEmpty()) {
                    System.out.println("Invalid or empty type for monster: " + monster.get("name"));
                    return null;
                }
            } else {
                System.out.println("Type information is null for monster: " + monster.get("name"));
                return null;
            }
        } else {
            System.out.println("Type information not found for monster: " + monster.get("name"));
            return null;
        }

        // Присвоение оставшихся полей
        npc.name = (String) monster.get("name");
        npc.size = getSize(monster.get("size"));
        npc.speed = getSpeed(monster.get("speed"));
        npc.Strength = getIntValue(monster, "str");
        npc.Dexterity = getIntValue(monster, "dex");
        npc.Constitution = getIntValue(monster, "con");
        npc.Intellect = getIntValue(monster, "int");
        npc.Wisdom = getIntValue(monster, "wis");
        npc.Charisma = getIntValue(monster, "cha");
        npc.senses = getListAsArray(monster, "senses", String.class);
        npc.passivePerception = "passive Perception " + monster.get("passive");
        npc.languages = getListAsArray(monster, "languages", String.class);
        npc.traits = getEntityFeatures(monster, "trait");
        npc.actions = getEntityFeatures(monster, "action");
        npc.legendaryActions = getEntityFeatures(monster, "legendary");
        npc.bonusActions = getEntityFeatures(monster, "bonus");
        npc.savingThrows = getSavingThrows(monster);

        npc.HP_formula = (String) ((Map<String, Object>) monster.get("hp")).get("formula");
        npc.reactions = getEntityFeatures(monster, "reaction");
        npc.xp = CRtoXP.getXpByCr(npc.cr);
        npc.skills = getSkills(monster);
        npc.Alignment = AlignmentConverter.convertAlignment(monster.get("alignment"));


        if (monster.containsKey("spellcasting")) {
            List<Map<String, Object>> spellcastingList = (List<Map<String, Object>>) monster.get("spellcasting");

            // Извлекаем первый объект в списке
            Map<String, Object> spellcasting = spellcastingList.get(0);

            // Проверяем, содержит ли поле name строку "Innate Spellcasting"
            String name = (String) spellcasting.get("name");
            if (name != null && name.contains("Innate Spellcasting")) {
                if (spellcasting.get("will") != null)
                    npc.innateSpellcasting.will = (List<String>) spellcasting.get("will");
                if (spellcasting.get("daily") != null)
                    npc.innateSpellcasting.daily = (Map<String, List<String>>) spellcasting.get("daily");

                System.out.println("Innate Spellcasting");
            }
        }

        // Обработка поля "Innate Spellcasting" в traits
        if (monster.containsKey("spellcasting")) {
            Map<String, Object> spellcasting = ((List<Map<String, Object>>) monster.get("spellcasting")).get(0); // Исправлено на get(0), так как нет метода getFirst()
            String nameCheck = (String) spellcasting.get("name");

            if (nameCheck != null && nameCheck.contains("Innate Spellcasting")) {
                List<Map<String, Object>> spellcastingList = (List<Map<String, Object>>) monster.get("spellcasting");

                List<EntityFeature> spellcastingFeatures = new ArrayList<>();

                for (Map<String, Object> spellcastingEntry : spellcastingList) {
                    String name = (String) spellcastingEntry.get("name");

                    // Проверка на null для headerEntries
                    List<String> headerEntries = (List<String>) spellcastingEntry.get("headerEntries");
                    StringBuilder descriptionBuilder = new StringBuilder();

                    if (headerEntries != null) {
                        // Преобразуем headerEntries в строку, если оно существует
                        for (String entry : headerEntries) {
                            descriptionBuilder.append(entry).append(" ");
                        }
                    }

                    // Создаем новый EntityFeature и добавляем в список spellcastingFeatures
                    EntityFeature feature = new EntityFeature(name, descriptionBuilder.toString().trim());
                    spellcastingFeatures.add(feature);
                }

                // Проверка на наличие существующих traits
                if (npc.traits != null) {
                    // Объединяем существующие traits с новыми
                    List<EntityFeature> allFeatures = new ArrayList<>(Arrays.asList(npc.traits));
                    allFeatures.addAll(spellcastingFeatures);
                    npc.traits = allFeatures.toArray(new EntityFeature[0]);
                } else {
                    // Если traits еще нет, то просто присваиваем новые
                    npc.traits = spellcastingFeatures.toArray(new EntityFeature[0]);
                }
            }
        }
        npc.conditionImmunities = damageResistances.processResistances(monster, "conditionImmune");
        npc.damageImmunities = damageResistances.processResistances(monster, "immune");
        npc.damageResistances = damageResistances.processResistances(monster, "resist");
        npc.source = (String) monster.get("source");
        npc = setLairActionsFromLiars(liarList, npc);

        processNPCFeatures(npc);

        return npc;
    }

    // Метод для установки lairActions на основе сравнения
    public static NPC setLairActionsFromLiars(Liar[] liars, NPC npc) {
        if (liars == null) return npc;

        for (Liar liar : liars) {
            // Убираем "Adult" и "Young" из имени
            String normalizedName = normalizeName(npc.name);

            if (liar.name.equals(normalizedName) && liar.source.equals(liar.source)) {
                npc.lairActions = liar;
            }
        }
        // Если нет совпадений, оставляем lairActions как есть
        return npc;
    }

    // Метод для нормализации имени
    private static String normalizeName(String name) {
        return name.replaceAll(" (Adult|Young)$", "");
    }

    // Процессинг для NPC (DC, описание атак и урона)
    private static void processNPCFeatures(NPC npc) {
        if (npc != null) {
            npc.actions = replaceDescriptions(npc.actions);
            npc.traits = replaceDescriptions(npc.traits);
            npc.legendaryActions = replaceDescriptions(npc.legendaryActions);
            npc.bonusActions = replaceDescriptions(npc.bonusActions);
            npc.reactions = replaceDescriptions(npc.reactions);
            npc.innateSpellcasting.daily = replaceDescriptions(npc.innateSpellcasting.daily);
            npc.innateSpellcasting.will = replaceDescriptions(npc.innateSpellcasting.will);


            changeDC(npc);
            changeHit(npc);
        }
    }


    // Преобразование значений DC
    private static void changeDC(NPC npc) {
        npc.actions = processEntityFeaturesDC(npc.actions);
        npc.traits = processEntityFeaturesDC(npc.traits);
        npc.legendaryActions = processEntityFeaturesDC(npc.legendaryActions);
        npc.bonusActions = processEntityFeaturesDC(npc.bonusActions);
    }

    // Преобразование описаний с шаблоном DC
    private static EntityFeature[] processEntityFeaturesDC(EntityFeature[] features) {
        if (features == null) return null;
        for (EntityFeature feature : features) {
            feature.description = feature.description.replaceAll("\\{@dc (\\d+)}", "DC $1");
        }
        return features;
    }

    // Замена шаблонов в описаниях
    private static EntityFeature[] replaceDescriptions(EntityFeature[] features) {
        if (features == null) return null;

        for (EntityFeature feature : features) {
            if (feature != null) {
                String description = feature.description != null ? feature.description : "";
                String name = feature.name != null ? feature.name : "";

                // Замены в описании
                description = description.replaceAll("\\{@atk mw,rw}", getAttackTypeReplacement(description, "Melee Weapon Attack", "Ranged Weapon Attack"));
                description = description.replaceAll("\\{@atk mw}", getAttackTypeReplacement(description, "Melee Weapon Attack", null));
                description = description.replaceAll("\\{@atk rw}", getAttackTypeReplacement(description, "Ranged Weapon Attack", null));
                description = description.replaceAll("\\{@atk rs}", getAttackTypeReplacement(description, "Ranged Spell Attack", null));
                description = description.replaceAll("\\{@atk ms}", getAttackTypeReplacement(description, "Melee Spell Attack", null));
                description = description.replaceAll("\\{@hit (\\d+)}", "+$1");
                description = description.replaceAll("\\{@damage (.+?)}", "$1");
                description = description.replaceAll("\\{@dice (.+?)}", "($1)");
                description = description.replaceAll("\\{@(creature|spell|condition|skill|status) (.+?)}", "$2");
                description = description.replaceAll("\\{@(i) (.+?)}", "\n$2");

                // Замены в имени
                name = name
                        .replaceAll("\\{@recharge (\\d)}", "(Recharge $1-6)")
                        .replaceAll("\\{@recharge}", "(Recharge 6)");

                feature.description = description;
                feature.name = name;
            }
        }

        return features;
    }

    private static List<String> replaceDescriptions(List<String> descriptions) {
        if (descriptions == null) return null;

        List<String> updatedDescriptions = new ArrayList<>();
        for (String description : descriptions) {
            if (description != null) {
                description = description.replaceAll("\\{@atk mw,rw}", getAttackTypeReplacement(description, "Melee Weapon Attack", "Ranged Weapon Attack"));
                description = description.replaceAll("\\{@atk mw}", getAttackTypeReplacement(description, "Melee Weapon Attack", null));
                description = description.replaceAll("\\{@atk rw}", getAttackTypeReplacement(description, "Ranged Weapon Attack", null));
                description = description.replaceAll("\\{@atk rs}", getAttackTypeReplacement(description, "Ranged Spell Attack", null));
                description = description.replaceAll("\\{@atk ms}", getAttackTypeReplacement(description, "Melee Spell Attack", null));
                description = description.replaceAll("\\{@hit (\\d+)}", "+$1");
                description = description.replaceAll("\\{@damage (.+?)}", "$1");
                description = description.replaceAll("\\{@dice (.+?)}", "($1)");
                description = description.replaceAll("\\{@(creature|spell|condition|skill|status) (.+?)}", "$2");
                description = description.replaceAll("\\{@(i) (.+?)}", "\n$2");

                updatedDescriptions.add(description);
            }
        }
        return updatedDescriptions;
    }

    private static Map<String, List<String>> replaceDescriptions(Map<String, List<String>> dailyDescriptions) {
        if (dailyDescriptions == null) return null;

        Map<String, List<String>> updatedDailyDescriptions = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : dailyDescriptions.entrySet()) {
            String key = entry.getKey();
            List<String> descriptions = entry.getValue();

            List<String> updatedDescriptions = replaceDescriptions(descriptions); // Используем ранее написанный метод
            updatedDailyDescriptions.put(key, updatedDescriptions);
        }
        return updatedDailyDescriptions;
    }


    // Вспомогательный метод для замены атак с учетом {@hit X}
    private static String getAttackTypeReplacement(String description, String attackType1, String attackType2) {
        String hitValue = findHitValue(description); // Найдем {@hit X}, если он есть

        if (attackType2 != null) {
            // Если есть два типа атаки (например, mw,rw)
            if (!hitValue.isEmpty()) {
                return attackType1 + " " + hitValue + " to hit" + " or " + attackType2;
            } else {
                return attackType1 + " or " + attackType2;
            }
        } else {
            // Если только один тип атаки (например, mw или rw)
            return attackType1;
        }
    }

    // Вспомогательный метод для поиска значения {@hit X} и {@h}
    private static String findHitValue(String description) {
        // Находим значение для {@hit X} или {@h X} с учетом знака +
        Matcher matcher = Pattern.compile("\\{@hit ([+-]?\\d+)}|\\{@h ([+-]?\\d+)}").matcher(description);
        if (matcher.find()) {
            String hitValue = matcher.group(1);
            if (hitValue == null) {
                hitValue = matcher.group(2);
            }
            // Возвращаем значение с соответствующим знаком
            return hitValue;
        }
        return "";
    }

    // Метод для замены значений {@hit X} и {@h} в описаниях
    private static String replaceHitValues(String description) {
        if (description == null) {
            return "";
        }
        // Заменяем {@hit X} на Hit: X
        description = description.replaceAll("\\{@hit ([+-]?\\d+)}", "$1");
        // Заменяем {@h} на Hit:
        description = description.replaceAll("\\{@h}", "Hit: ");
        return description;
    }

    // Преобразование {@h} и {@hit} в описаниях
    private static NPC changeHit(NPC npc) {
        for (EntityFeature action : npc.actions) {
            action.description = replaceHitValues(action.description);
        }
        for (EntityFeature trait : npc.traits) {
            trait.description = replaceHitValues(trait.description);
        }
        for (EntityFeature bonusAction : npc.bonusActions) {
            bonusAction.description = replaceHitValues(bonusAction.description);
        }
        for (EntityFeature legendaryAction : npc.legendaryActions) {
            legendaryAction.description = replaceHitValues(legendaryAction.description);
        }
        return npc;
    }


    // Вспомогательные методы для получения данных о монстрах
    private static String getSize(Object sizeObj) {
        if (sizeObj instanceof List) {
            String size = ((List<String>) sizeObj).isEmpty() ? "" : ((List<String>) sizeObj).get(0);
            return switch (size) {
                case "T" -> "Tiny";
                case "S" -> "Small";
                case "M" -> "Medium";
                case "L" -> "Large";
                case "H" -> "Huge";
                case "G" -> "Gargantuan";
                case "C" -> "Colossal";
                default -> size;
            };
        }
        return "";
    }

    private static String getType(Object typeObj) {
        if (typeObj instanceof Map) {
            // Попытка получить значение по ключу "type"
            Object typeValue = ((Map<?, ?>) typeObj).get("type");
            return typeValue != null ? typeValue.toString() : "";
        } else if (typeObj instanceof String) {
            return (String) typeObj;
        } else {
            System.out.println("Unexpected data type for type: " + typeObj);
            return "";
        }
    }


    private static String[] getSpeed(Object speedObj) {
        if (speedObj instanceof Map) {
            return ((Map<String, Object>) speedObj).toString().replace("{", "").replace("}", "").split(", ");
        }
        return new String[0];
    }

    private static int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Integer ? (Integer) value : 0;
    }

    private static <T> T[] getListAsArray(Map<String, Object> map, String key, Class<T> clazz) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return (T[]) list.toArray((T[]) Array.newInstance(clazz, list.size()));
        }
        return (T[]) Array.newInstance(clazz, 0);
    }

    private static EntityFeature[] getEntityFeatures(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) value;
            return list.stream()
                    .map(entry -> new EntityFeature(
                            (String) entry.get("name"),
                            processEntries(entry.get("entries"))))
                    .toArray(EntityFeature[]::new);
        }
        return new EntityFeature[0];
    }

    private static String processEntries(Object entries) {
        if (entries instanceof List) {
            StringBuilder result = new StringBuilder();
            for (Object entry : (List<?>) entries) {
                result.append(entry.toString()).append(" ");
            }
            return result.toString().trim();
        }
        return entries != null ? entries.toString() : "";
    }

    private static String[] getSavingThrows(Map<String, Object> monster) {
        if (monster.containsKey("save")) {
            return monster.get("save").toString().replace("{", "").replace("}", "").replace("=", " ").split(", ");
        }
        return new String[0];
    }

    private static String getCR(Map<String, Object> monster) {
        Object crObj = monster.get("cr");
        return crObj instanceof String ? (String) crObj : ((Map<?, ?>) crObj).get("cr").toString();
    }

    private static String[] getSkills(Map<String, Object> monster) {
        if (monster.containsKey("skill")) {
            return monster.get("skill").toString().replace("{", "").replace("}", "").replace("=", " ").split(", ");
        }
        return new String[0];
    }

    private static int extractAC(Object acObj) {
        return Integer.parseInt(acObj.toString().replaceAll("[^0-9]", ""));
    }
}

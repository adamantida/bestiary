import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NPC {
    String source;
    EntityFeature[] actions;
    EntityFeature[] traits;
    EntityFeature[] legendaryActions;
    EntityFeature[] bonusActions;
    Liar lairActions;
    EntityFeature[] reactions;


    String conditionImmunities; //conditionImmune
    String damageImmunities; //immune
    String damageResistances; //resist
    InnateSpellcasting innateSpellcasting = new InnateSpellcasting();

    String HP_formula;
    String cr;
    String name;
    String passivePerception;
    String size;
    String type;
    String[] languages;
    String[] senses;
    String[] skills;
    String[] savingThrows;
    String[] speed;
    String Alignment;

    String description;
    String PortraitPath;
    String TokenPath;

    int AC;
    String AC_description;
    int Charisma;
    int Constitution;
    int Dexterity;
    int HP;
    int Intellect;
    int Strength;
    int Wisdom;
    int xp;
}


class EntityFeature {
    public String name;
    public String description;

    public EntityFeature(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public EntityFeature() {
    }

    @Override
    public String toString() {
        return "EntityFeature{name='" + name + "', description='" + description + "'}";
    }
}

class damageResistances {
    String[] name;
    String note;
    String[] nameType2;

    // Основной метод для обработки сопротивлений, иммунитетов и состояний
    public static String processResistances(Map<String, Object> objectMap, String type) {
        damageResistances resist = getResistances(objectMap, type);
        var ret = formatResistances(resist);
        return ret == null || ret.isEmpty() ? null : ret;
    }

    // Метод для извлечения данных из objectMap и создания объекта damageResistances
    public static damageResistances getResistances(Map<String, Object> objectMap, String type) {
        if (!objectMap.containsKey(type)) {
            return null;
        }

        Object resistValue = objectMap.get(type);
        damageResistances resist = new damageResistances();

        if (resistValue instanceof List<?>) {
            List<?> resistList = (List<?>) resistValue;

            // Проверяем, что первый элемент списка является строкой
            if (resistList.get(0) instanceof String) {
                resist.name = new String[]{(String) resistList.get(0)};
            } else if (resistList.get(0) instanceof Map<?, ?>) {
                // Если это карта (LinkedHashMap), то её нужно обрабатывать по-другому
                Map<String, Object> resistMap = (Map<String, Object>) resistList.get(0);

                // Обрабатываем это как другой формат данных, если это необходимо
                // Здесь можно добавлять другую логику, если необходимо извлекать данные из карты
            }

            // Проверяем, что второй элемент списка является картой (случай с дополнительной информацией)
            if (resistList.size() > 1 && resistList.get(1) instanceof Map<?, ?>) {
                Map<String, Object> resistMap = (Map<String, Object>) resistList.get(1);

                // Заполняем дополнительные сопротивления (например, "bludgeoning", "piercing", "slashing")
                Object resistInner = resistMap.get("resist");
                if (resistInner instanceof List<?>) {
                    List<String> innerResistList = (List<String>) resistInner;
                    resist.nameType2 = innerResistList.toArray(new String[0]);
                }

                // Заполняем поле note (примечание)
                resist.note = (String) resistMap.get("note");
            } else {
                // Обрабатываем простой список сопротивлений (например, "cold", "fire", "lightning")
                List<String> allResists = new ArrayList<>();
                for (Object item : resistList) {
                    if (item instanceof String) {
                        allResists.add((String) item);
                    }
                }
                resist.name = allResists.toArray(new String[0]);
            }
        }

        return resist;
    }


    // Метод для форматирования объекта damageResistances в строку
    public static String formatResistances(damageResistances resist) {
        if (resist == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        // Основные сопротивления
        if (resist.name != null && resist.name.length > 0) {
            result.append(String.join(", ", resist.name));
        }

        // Дополнительные сопротивления
        if (resist.nameType2 != null && resist.nameType2.length > 0) {
            result.append("; ");
            if (resist.nameType2.length > 1) {
                result.append(String.join(", ", Arrays.asList(resist.nameType2).subList(0, resist.nameType2.length - 1)))
                        .append(", and ")
                        .append(resist.nameType2[resist.nameType2.length - 1]);
            } else {
                result.append(resist.nameType2[0]);
            }
        }

        // Примечание
        if (resist.note != null && !resist.note.isEmpty()) {
            result.append(" ").append(resist.note);
        }

        return result.isEmpty() ? null : result.toString(); // Возвращаем null, если строка пустая
    }
}
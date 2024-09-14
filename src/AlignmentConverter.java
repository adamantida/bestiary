import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlignmentConverter {

    // Карта для сопоставления символов выравнивания с их полными значениями
    private static final Map<String, String> alignmentMap = new HashMap<>();

    static {
        alignmentMap.put("A", "Any");
        alignmentMap.put("C", "Chaotic");
        alignmentMap.put("E", "Evil");
        alignmentMap.put("G", "Good");
        alignmentMap.put("L", "Lawful");
        alignmentMap.put("N", "Neutral");
        alignmentMap.put("U", "Unaligned");
    }

    // Метод для получения выравнивания в виде строки
    public static String convertAlignment(Object alignmentObj) {
        if (alignmentObj instanceof List) {
            List<?> alignmentList = (List<?>) alignmentObj; // Используем универсальный список
            return convertFromList(alignmentList);
        } else if (alignmentObj instanceof String) {
            return convertFromString((String) alignmentObj);
        }

        return "";
    }

    // Преобразование списка символов или карт в строку
    private static String convertFromList(List<?> alignmentList) {
        StringBuilder result = new StringBuilder();
        for (Object alignment : alignmentList) {
            if (alignment instanceof String) {
                // Приводим alignment к строке перед использованием
                String alignmentStr = (String) alignment;
                result.append(alignmentMap.getOrDefault(alignmentStr, alignmentStr)).append(" ");
            } else if (alignment instanceof Map) {
                // Если элемент - карта, пытаемся достать значение "alignment"
                Object alignmentMapEntry = ((Map<?, ?>) alignment).get("alignment");
                if (alignmentMapEntry != null) {
                    // Обработка если alignmentMapEntry - строка
                    if (alignmentMapEntry instanceof String) {
                        String alignmentMapEntryStr = (String) alignmentMapEntry;
                        result.append(alignmentMap.getOrDefault(alignmentMapEntryStr, alignmentMapEntryStr)).append(" ");
                    }
                    // Обработка если alignmentMapEntry - массив строк
                    else if (alignmentMapEntry instanceof String[]) {
                        String[] alignmentArray = (String[]) alignmentMapEntry;
                        for (String alignmentElement : alignmentArray) {
                            result.append(alignmentMap.getOrDefault(alignmentElement, alignmentElement)).append(" ");
                        }
                    }
                }
            }
        }
        return result.toString().trim();
    }

    // Преобразование строки символов в полные названия
    private static String convertFromString(String alignmentStr) {
        StringBuilder result = new StringBuilder();
        for (char alignmentChar : alignmentStr.toCharArray()) {
            String alignment = alignmentMap.getOrDefault(String.valueOf(alignmentChar), String.valueOf(alignmentChar));
            result.append(alignment).append(" ");
        }
        return result.toString().trim();
    }
}

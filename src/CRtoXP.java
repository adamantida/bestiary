import java.util.HashMap;
import java.util.Map;

public class CRtoXP {

    // Маппинг значений CR на XP
    private static final Map<String, Integer> crToXpMap = new HashMap<>();

    // Статический блок для инициализации карты
    static {
        crToXpMap.put("0", 10);
        crToXpMap.put("1/8", 25);
        crToXpMap.put("1/4", 50);
        crToXpMap.put("1/2", 100);
        crToXpMap.put("1", 200);
        crToXpMap.put("2", 450);
        crToXpMap.put("3", 700);
        crToXpMap.put("4", 1100);
        crToXpMap.put("5", 1800);
        crToXpMap.put("6", 2300);
        crToXpMap.put("7", 2900);
        crToXpMap.put("8", 3900);
        crToXpMap.put("9", 5000);
        crToXpMap.put("10", 5900);
        crToXpMap.put("11", 7200);
        crToXpMap.put("12", 8400);
        crToXpMap.put("13", 10000);
        crToXpMap.put("14", 11500);
        crToXpMap.put("15", 13000);
        crToXpMap.put("16", 15000);
        crToXpMap.put("17", 18000);
        crToXpMap.put("18", 20000);
        crToXpMap.put("19", 22000);
        crToXpMap.put("20", 25000);
        crToXpMap.put("21", 33000);
        crToXpMap.put("22", 41000);
        crToXpMap.put("23", 50000);
        crToXpMap.put("24", 62000);
        crToXpMap.put("25", 75000);
        crToXpMap.put("26", 90000);
        crToXpMap.put("27", 105000);
        crToXpMap.put("28", 120000);
        crToXpMap.put("29", 135000);
        crToXpMap.put("30", 155000);
    }

    // Метод для получения XP по строковому значению CR
    public static int getXpByCr(String cr) {
        return crToXpMap.getOrDefault(cr, -1); // Возвращает XP или -1, если CR не найдено
    }
}

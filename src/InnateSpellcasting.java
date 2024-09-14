import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InnateSpellcasting {

    public List<String> will; // Список заклинаний, которые можно использовать без ограничений
    public Map<String, List<String>> daily; // Карта заклинаний с ограниченным количеством использования

    // Конструктор
    public InnateSpellcasting() {
        this.will = new ArrayList<>();
        this.daily = new HashMap<>();
    }

    // Метод для добавления заклинаний в список will
    public void addWillSpell(String spell) {
        will.add(spell);
    }

    // Метод для добавления заклинаний в карту daily
    public void addDailySpell(String key, String spell) {
        daily.computeIfAbsent(key, k -> new ArrayList<>()).add(spell);
    }


    // Метод для получения всех заклинаний will
    public List<String> getWill() {
        return will;
    }

    // Метод для получения всех заклинаний daily
    public Map<String, List<String>> getDaily() {
        return daily;
    }

}

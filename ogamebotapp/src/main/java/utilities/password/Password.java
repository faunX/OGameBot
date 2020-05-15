package utilities.password;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jarndt on 5/28/17.
 */
public class Password {
    private static List<String> characters =
            IntStream.range('a', 'z' + 1).boxed().map(i -> "" + ((char) i.intValue())).collect(Collectors.toList());
    static {
        IntStream.range(0,10).forEach(i->characters.add(i+""));
    }

    private static Random random = new Random();
    public static String randomString(int length){
        StringBuilder builder = new StringBuilder("");
        for(int i = 0; i<length; i++)
            builder.append(characters.get(random.nextInt(characters.size())));
        return builder.toString();
    }

    public static List<String> getCharacters() {
        return characters;
    }

    public static String getRandomPassword(int length){
        return randomString(length);
    }
}

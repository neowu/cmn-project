package core.aws.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamHelperTest {
    @Test
    void onlyOne() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        Optional<Integer> result = list.stream().filter(i -> i > 3).reduce(StreamHelper.onlyOne());
        assertEquals((Integer) 4, result.get());
    }

    @Test
    void instanceOf() {
        List<Object> list = new ArrayList<>();
        list.add("text1");
        list.add(1);
        list.add("text2");
        list.add(2);

        List<String> result = list.stream().flatMap(StreamHelper.instanceOf(String.class)).collect(Collectors.toList());

        assertEquals(2, result.size());
        assertEquals("text1", result.get(0));
        assertEquals("text2", result.get(1));
    }
}

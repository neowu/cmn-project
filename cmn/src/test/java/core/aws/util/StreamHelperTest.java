package core.aws.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StreamHelperTest {
    @Test
    public void onlyOne() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        Optional<Integer> result = list.stream().filter(i -> i > 3).reduce(StreamHelper.onlyOne());
        Assert.assertEquals((Integer) 4, result.get());
    }

    @Test
    public void instanceOf() {
        List<Object> list = new ArrayList<>();
        list.add("text1");
        list.add(1);
        list.add("text2");
        list.add(2);

        List<String> result = list.stream().flatMap(StreamHelper.instanceOf(String.class)).collect(Collectors.toList());

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("text1", result.get(0));
        Assert.assertEquals("text2", result.get(1));
    }
}
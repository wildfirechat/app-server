package cn.wildfirechat.app.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NumericIdGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(NumericIdGenerator.class);
    public static String getId(List<Integer> firstNumber, List<Integer> firstExceptNumber, int idLength) {
        List<Integer> numbers = new ArrayList<>();
        if(firstNumber != null && !firstNumber.isEmpty()) {
            numbers.addAll(firstNumber);
        } else {
            for (int i = 0; i <= 9; i++) {
                numbers.add(i);
            }
        }

        if(firstExceptNumber != null && !firstExceptNumber.isEmpty()) {
            numbers.removeAll(firstExceptNumber);
        }

        numbers.remove((Integer)4);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < idLength; i++) {
            if(i == 0 && !numbers.isEmpty()) {
                sb.append(numbers.get((int)(Math.random() * numbers.size())));
            } else {
                int n;
                do {
                     n = (int)(Math.random()*10);
                } while (n == 4);
                sb.append(n);
            }
        }
        return sb.toString();
    }
    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            String id = getId(null, Arrays.asList(0), 6);
            LOG.info("{}", id);
        }
    }
}

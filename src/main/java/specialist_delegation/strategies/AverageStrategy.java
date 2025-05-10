package specialist_delegation.strategies;

import java.util.ArrayList;
import java.util.Arrays;

public class AverageStrategy implements Strategy {
    @Override
    public ArrayList<Double> executeOperation(ArrayList<Double> recvData) {
        return new ArrayList<>(Arrays.asList(((double) Math.round((recvData.stream().mapToDouble(element -> element).sum()/recvData.size()) * 100)/100)));
    }
}

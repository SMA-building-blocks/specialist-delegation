package specialist_delegation.strategies;

import java.util.ArrayList;
import java.util.Arrays;

public class StdDeviationStrategy implements Strategy {
    @Override
    public ArrayList<Double> executeOperation(ArrayList<Double> recvData) {
        double avg = recvData.stream().mapToDouble(element -> element).sum()/recvData.size();

        double variance = recvData.stream().mapToDouble(element -> ((element - avg)*(element - avg))).sum()/recvData.size();

        return new ArrayList<>(Arrays.asList(((double) Math.round(Math.sqrt(variance) * 100)/100)));
    }
}

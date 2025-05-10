package specialist_delegation.strategies;

import java.util.ArrayList;
import java.util.Arrays;

public class MedianStrategy implements Strategy {
    @Override
    public ArrayList<Double> executeOperation(ArrayList<Double> recvData) {
        recvData.sort(null);
        return new ArrayList<>(Arrays.asList(( recvData.size() % 2 != 0 ? 
            (double) recvData.get(Math.floorDiv(recvData.size(),2)) : 
            (double) ((recvData.get(recvData.size()/2) + recvData.get((recvData.size()/2) - 1))/2))));
    }
}

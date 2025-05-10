package specialist_delegation.strategies;

import java.util.ArrayList;

public class SortStrategy implements Strategy {
    @Override
    public ArrayList<Double> executeOperation(ArrayList<Double> recvData) {
        recvData.sort(null);
        return recvData;
    }
}

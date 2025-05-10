package specialist_delegation.strategies;

import java.util.ArrayList;

public interface Strategy {
    ArrayList<Double> executeOperation(ArrayList<Double> recvData);
}

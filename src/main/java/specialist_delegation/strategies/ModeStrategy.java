package specialist_delegation.strategies;

import java.util.ArrayList;

public class ModeStrategy implements Strategy {
    @Override
    public ArrayList<Double> executeOperation(ArrayList<Double> recvData) {
        recvData.sort(null);
        double prev = recvData.get(0);
        int maxCount = 1;
        int count = 1;
        ArrayList<Double> mode = new ArrayList<>();

        for(int i = 1; i< recvData.size(); i++){
            if(recvData.get(i) == prev){
                count++;
            } else {
                if( count > maxCount){
                    maxCount = count;
                    mode.clear();
                    mode.add(prev);
                } else if (count == maxCount){
                    mode.add(prev);
                }
                count = 1;
                prev = recvData.get(i);
            }
        }
        
        if( count > maxCount){
            mode.clear();
            mode.add(prev);
        } else if (count == maxCount){
            mode.add(prev);
        }

        return mode;
    }
}

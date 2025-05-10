package specialist_delegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Manager extends BaseAgent {

	private static final long serialVersionUID = 1L;

	private Queue<String> operations;

	@Override
	protected void setup() {
		logger.log(Level.INFO, "I'm the manager!");
		this.registerDF(this, "Manager", "manager");

		addBehaviour(handleMessages());
	}

	@Override
	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if (msg.getContent().startsWith(START) && msg.getContent().contains(DATA)) {
					logger.log(Level.INFO, String.format("%s MANAGER AGENT RECEIVED A START!", getLocalName()));
					workingData.clear();
					workingData = parseData(msg);
					dataSize = workingData.size();

					Collections.shuffle(originalOperations);
					operations = new LinkedList<>(originalOperations);

					String msgContentData = String.format("%s %d %s", DATA, workingData.size(), prepareSendingData(workingData));

					ArrayList<DFAgentDescription> foundWorkers = new ArrayList<>(
							Arrays.asList(searchAgentByType("subordinate")));

					Collections.shuffle(foundWorkers);

					foundWorkers.forEach(ag -> {
						sendMessage(ag.getName().getLocalName(), ACLMessage.REQUEST,
								String.format("%s %s", operations.remove(), msgContentData));
					});

					logger.log(Level.INFO, String.format("%s SENT START MESSAGE TO WORKERS!", getLocalName()));
				} else if (msg.getContent().startsWith(THANKS)) {
					logger.log(Level.INFO, String.format("%s RECEIVED THANKS FROM %s!", 
						getLocalName(), msg.getSender().getLocalName()));
				} else if (msg.getContent().startsWith(INFORM)) {
					ArrayList<String> msgContent = new ArrayList<>(Arrays.asList(msg.getContent().split(" ")));
					String performedOp = msgContent.get(1);

					ArrayList<Double> recvData = parseData(msg);

					logger.log(Level.INFO, String.format("%s RECEIVED DATA FROM %s AFTER %s OPERATION: %s!", getLocalName(), msg.getSender().getLocalName(), performedOp, recvData.toString()));

					int ansPerformative = ACLMessage.INFORM;
					String ansContent = THANKS;

					if ( !operations.isEmpty() ) {
						ansPerformative = ACLMessage.REQUEST;
						ansContent = String.format("%s %s %d %s", operations.remove(), DATA, workingData.size(), prepareSendingData(workingData));
					}

					sendMessage(msg.getSender().getLocalName(), ansPerformative, ansContent);
				} else {
					logger.log(Level.INFO,
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
									msg.getSender().getLocalName()));
				}
			}
		};
	}

	private String prepareSendingData (ArrayList<Double> inputWorkingData) {
		StringBuilder builder = new StringBuilder();

		for ( double val : workingData ) {
			builder.append(String.format("%s ", Double.toString(val)));
		}

		return builder.toString().trim();
	}
}

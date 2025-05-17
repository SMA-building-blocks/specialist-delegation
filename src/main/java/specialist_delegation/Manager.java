package specialist_delegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Manager extends BaseAgent {

	private static final long serialVersionUID = 1L;

	private static List<String>  operations = Collections.synchronizedList(new ArrayList<>());

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

					operations = Collections.synchronizedList(new ArrayList<>(originalOperations));

					for ( String opp : operations ) {
						searchSubordinatesByOperation(msg, opp);
					}

					logger.log(Level.INFO, String.format("%s SENT CFP MESSAGE TO WORKERS!", getLocalName()));
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

					sendMessage(msg.getSender().getLocalName(), ansPerformative, ansContent);
				} else if (msg.getContent().startsWith("CREATED")) {
					String [] splittedMsg = msg.getContent().split(" ");

					searchSubordinatesByOperation(msg, splittedMsg[1]);
				} else {
					logger.log(Level.INFO,
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
									msg.getSender().getLocalName()));
				}
			}
		};
	}
	@Override
	protected OneShotBehaviour handleCfp(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if ( msg.getPerformative() == ACLMessage.PROPOSE ) {
					if (msg.getContent().startsWith("PROFICIENCE")) {
						String [] splittedMsg = msg.getContent().split(" ");
						String recvPerfOpp = splittedMsg[1];

						/*
						 * Aqui deve ser feita a verificação do 
						 * THRESHOLD de Proficiência
						 */

						ACLMessage replyMsg = msg.createReply();
						if ( operations.contains(recvPerfOpp) ) {

							dataSize = workingData.size();
							String msgContentData = String.format("%s %d %s", DATA, workingData.size(), prepareSendingData(workingData));

							replyMsg.setContent(String.format("%s %s", recvPerfOpp, msgContentData));
							replyMsg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
							
							operations.remove(recvPerfOpp);

							logger.log(Level.INFO, String.format("%s SENT MESSAGE WITH WORKLOAD TO WORKER %s!", getLocalName(), msg.getSender().getLocalName()));
						} else {
							replyMsg.setContent("REJECTED");
							replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);

							logger.log(Level.INFO, String.format("%s SENT REJECT MESSAGE TO WORKER %s!", getLocalName(), msg.getSender().getLocalName()));
						}
						send(replyMsg);

						
					}
				}
				
			}
		};
	}

	/*
	 * TO-DO: create handler for unknown performative
	 */
	@Override
	protected OneShotBehaviour handleUnk(ACLMessage msg){
		return new OneShotBehaviour(this){
			private static final long serialVersionUID = 1L;

			public void action() {

			}
		};
	}

	private void searchSubordinatesByOperation(ACLMessage msg, String opp) {
		ArrayList<DFAgentDescription> foundWorkers = new ArrayList<>(
			Arrays.asList(searchAgentByType(opp)));

		if ( foundWorkers.isEmpty() ) {
			ACLMessage reqAgentMsg = msg.createReply();
			reqAgentMsg.setPerformative(ACLMessage.REQUEST);
			reqAgentMsg.setContent(String.format("%s %s", "CREATE", opp));
			send(reqAgentMsg);
			return;
		}

		foundWorkers.forEach(ag -> {
			sendMessage(ag.getName().getLocalName(), ACLMessage.CFP,
					String.format("%s %s", "PROFICIENCE", opp));
		});
	}

	private String prepareSendingData (ArrayList<Double> inputWorkingData) {
		StringBuilder builder = new StringBuilder();

		for ( double val : workingData ) {
			builder.append(String.format("%s ", Double.toString(val)));
		}

		return builder.toString().trim();
	}
}

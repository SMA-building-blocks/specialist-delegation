package specialist_delegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Manager extends BaseAgent {

	private static final long serialVersionUID = 1L;

	private static Map<String, Integer>  operations = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, ArrayList<AID>>  operationsRequested = Collections.synchronizedMap(new HashMap<>());

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
					generateRandomThresholds();
					logger.log(Level.INFO, String.format("%s MANAGER AGENT RECEIVED A START!", getLocalName()));
					workingData.clear();
					workingData = parseData(msg);

					for ( String opp : operations.keySet() ) {
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

					operations.remove(performedOp);

					int ansPerformative = ACLMessage.INFORM;
					String ansContent = THANKS;

					sendMessage(msg.getSender().getLocalName(), ansPerformative, ansContent);
				} else if (msg.getContent().startsWith("CREATED")) {
					String [] splittedMsg = msg.getContent().split(" ");

					String agentName = msg.getContent().substring(msg.getContent().indexOf(splittedMsg[2], 0));

					if (operationsRequested.get(splittedMsg[1]) == null){
						operationsRequested.put(splittedMsg[1], new ArrayList<AID>(Arrays.asList(new AID(agentName, true))));
					} else {
						operationsRequested.put(splittedMsg[1],operationsRequested.get(splittedMsg[1]) );
					}

					sendMessage(new AID(agentName, AID.ISLOCALNAME).getLocalName(), ACLMessage.CFP,
						String.format("%s %s", PROFICIENCE, splittedMsg[1]));

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
					if (msg.getContent().startsWith(PROFICIENCE)) {
						String [] splittedMsg = msg.getContent().split(" ");
						String recvPerfOpp = splittedMsg[1];
						int recvOppProficience = Integer.parseInt(splittedMsg[2]);

						ACLMessage replyMsg = msg.createReply();

						operationsRequested.get(recvPerfOpp).remove(msg.getSender());

						if ( operations.keySet().contains(recvPerfOpp) && operations.get(recvPerfOpp) <= recvOppProficience) {

							dataSize = workingData.size();
							String msgContentData = String.format("%s %d %s", DATA, workingData.size(), prepareSendingData(workingData));

							replyMsg.setContent(String.format("%s %s", recvPerfOpp, msgContentData));
							replyMsg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

							send(replyMsg);

							logger.log(Level.INFO, String.format("%s SENT MESSAGE WITH WORKLOAD TO WORKER %s!", getLocalName(), msg.getSender().getLocalName()));
						} else if (!operations.keySet().contains(recvPerfOpp)){
							replyMsg.setContent("REJECTED");
							replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
							
							send(replyMsg);

							logger.log(Level.INFO, String.format("%s SENT REJECT MESSAGE TO WORKER %s!", getLocalName(), msg.getSender().getLocalName()));
						} else if (operationsRequested.get(recvPerfOpp).isEmpty() && operations.containsKey(recvPerfOpp)) {
							ACLMessage reqAgentMsg = new ACLMessage(ACLMessage.REQUEST);
							reqAgentMsg.setContent(String.format("%s %s", "CREATE", recvPerfOpp));
							reqAgentMsg.addReceiver(searchAgentByType("Creator")[0].getName());
							send(reqAgentMsg);
							//searchAgentByType(recvPerfOpp);
						}
						
					}
				}
				
			}
		};
	}

	@Override
	protected OneShotBehaviour handleRefuse (ACLMessage msg) {
		return new OneShotBehaviour(this){
			private static final long serialVersionUID = 1L;

			public void action() {

				if ( msg.getContent().startsWith("OPERATION") ) {
					String [] splittedMsg = msg.getContent().split(" ");

					if ( !operations.keySet().contains(splittedMsg[1]) ) {
						logger.log(Level.WARNING,
							String.format("%s %s %s %s %s", ANSI_YELLOW, getLocalName(), ": OPERATION NOT NEEDED SENT FROM",
									msg.getSender().getLocalName(), ANSI_RESET));
					} else {
						searchSubordinatesByOperation(msg, splittedMsg[1], msg.getSender());
					}

				} else {
					logger.log(Level.INFO,
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
									msg.getSender().getLocalName()));
				}

			}
		};
	}
	
	private void generateRandomThresholds() {
		for(int i =0; i< originalOperations.size(); i++){
			operations.put(originalOperations.get(i), rand.nextInt(MIN_PROFICIENCE, MAX_PROFICIENCE));
		}
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

		ArrayList<AID> workersArray = new ArrayList<>(foundWorkers.stream().map(val -> val.getName()).toList());
		
		if (operationsRequested.get(opp) == null){
			operationsRequested.put(opp, workersArray);
		} else {
			workersArray.addAll(operationsRequested.get(opp));
			operationsRequested.put(opp, workersArray );
		}

		foundWorkers.forEach(ag -> {
			sendMessage(ag.getName().getLocalName(), ACLMessage.CFP,
			String.format("%s %s", PROFICIENCE, opp));
		});
	}

	private void searchSubordinatesByOperation(ACLMessage msg, String opp, AID notThisAgent) {
		ArrayList<DFAgentDescription> foundWorkers = new ArrayList<>(
			Arrays.asList(searchAgentByType(opp)));

		if ( foundWorkers.isEmpty() ) {
			ACLMessage reqAgentMsg = msg.createReply();
			reqAgentMsg.setPerformative(ACLMessage.REQUEST);
			reqAgentMsg.setContent(String.format("%s %s", "CREATE", opp));
			send(reqAgentMsg);
			return;
		}

		ArrayList<AID> workersArray = new ArrayList<>(foundWorkers.stream().map(val -> val.getName()).toList());
		
		workersArray.remove(notThisAgent);

		if (operationsRequested.get(opp) == null){
			operationsRequested.put(opp, workersArray);
		} else {
			workersArray.addAll(operationsRequested.get(opp));
			operationsRequested.put(opp, workersArray );
		}

		foundWorkers.forEach(ag -> {
			if ( !ag.getName().equals(notThisAgent) ) {
				sendMessage(ag.getName().getLocalName(), ACLMessage.CFP,
						String.format("%s %s", PROFICIENCE, opp));
			}
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

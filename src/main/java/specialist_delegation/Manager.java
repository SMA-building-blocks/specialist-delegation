package specialist_delegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Manager extends BaseAgent {

	private static final long serialVersionUID = 1L;

	private static Map<String, Integer>  operations = Collections.synchronizedMap(new HashMap<>());
	private static Set<AID> timedOutAgents = Collections.synchronizedSet(new HashSet<AID>());
	private static Map<String, ArrayList<AID>>  operationsRequested = Collections.synchronizedMap(new HashMap<>());
	private static Set<String> operationsSent = Collections.synchronizedSet(new HashSet<String>());

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

					for ( String opp : operations.keySet() ) searchSubordinatesByOperation(opp);

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
					timedOutAgents.remove(msg.getSender());
					operationsSent.remove(performedOp);

					int ansPerformative = ACLMessage.INFORM;
					String ansContent = THANKS;

					sendMessage(msg.getSender().getLocalName(), ansPerformative, ansContent);
				} else if (msg.getContent().startsWith("CREATED")) {
					String [] splittedMsg = msg.getContent().split(" ");

					if (operationsRequested.get(splittedMsg[1]) == null){
						operationsRequested.put(splittedMsg[1], new ArrayList<>(Arrays.asList(new AID(splittedMsg[2], AID.ISLOCALNAME))));
					} else {
						operationsRequested.put(splittedMsg[1], operationsRequested.get(splittedMsg[1]) );
					}

					sendMessage(new AID(splittedMsg[2], AID.ISLOCALNAME).getLocalName(), ACLMessage.CFP,
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
				if ( msg.getPerformative() == ACLMessage.PROPOSE && msg.getContent().startsWith(PROFICIENCE)) {
					String [] splittedMsg = msg.getContent().split(" ");
					String recvPerfOpp = splittedMsg[1];
					int recvOppProficience = Integer.parseInt(splittedMsg[2]);

					ACLMessage replyMsg = msg.createReply();

					operationsRequested.get(recvPerfOpp).remove(msg.getSender());

					if ( operations.keySet().contains(recvPerfOpp) && operations.get(recvPerfOpp) <= recvOppProficience && !operationsSent.contains(recvPerfOpp)  ) {
						operationsSent.add(recvPerfOpp);
						
						dataSize = workingData.size();
						String msgContentData = String.format("%s %d %s", DATA, workingData.size(), prepareSendingData(workingData));
						
						replyMsg.setContent(String.format("%s %s", recvPerfOpp, msgContentData));
						replyMsg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);

						send(replyMsg);
						operationsRequested.get(recvPerfOpp).clear();

						addBehaviour(timeoutBehaviour(msg.getSender(), recvPerfOpp, TIMEOUT_LIMIT));

						logger.log(Level.INFO, String.format("%s %s SENT MESSAGE WITH %s WORKLOAD TO WORKER %s! %s", ANSI_PURPLE, getLocalName(), recvPerfOpp, msg.getSender().getLocalName(), ANSI_RESET));
					} else if (!operations.keySet().contains(recvPerfOpp)){
						replyMsg.setContent("REJECTED");
						replyMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);

						operationsRequested.get(recvPerfOpp).clear();
						
						send(replyMsg);

						logger.log(Level.INFO, String.format("%s SENT REJECT MESSAGE TO WORKER %s!", getLocalName(), msg.getSender().getLocalName()));
					} else if (operationsRequested.get(recvPerfOpp).isEmpty() && operations.containsKey(recvPerfOpp) && !operationsSent.contains(recvPerfOpp)) {
						ACLMessage reqAgentMsg = new ACLMessage(ACLMessage.REQUEST);
						reqAgentMsg.setContent(String.format("%s %s", CREATE, recvPerfOpp));
						reqAgentMsg.addReceiver(searchAgentByType(CREATOR)[0].getName());
						send(reqAgentMsg);
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
						searchSubordinatesByOperation(splittedMsg[1], Arrays.asList(msg.getSender()));
					}
				} else {
					logger.log(Level.INFO,
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
									msg.getSender().getLocalName()));
				}

			}
		};
	}

	@Override
	protected WakerBehaviour timeoutBehaviour(AID requestedAgent, String requestedOperation, long timeout) {
		return new WakerBehaviour(this, timeout) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onWake() {
				if ( operations.containsKey(requestedOperation) ) {
					logger.log(Level.WARNING,
						String.format("%s Agent %s timed out! %s", ANSI_YELLOW, requestedAgent.getLocalName(), ANSI_RESET));

					operationsSent.remove(requestedOperation);

					timedOutAgents.add(requestedAgent);
					searchSubordinatesByOperation(requestedOperation, new ArrayList<>(timedOutAgents));
				}
			}
		};
	}
	
	private void generateRandomThresholds() {
		for ( String op : originalOperations )
			operations.put(op, rand.nextInt(MIN_PROFICIENCE, MAX_PROFICIENCE));
	}

	private void searchSubordinatesByOperation(String opp) {
		ArrayList<DFAgentDescription> foundWorkers = new ArrayList<>(
			Arrays.asList(searchAgentByType(opp)));

		if ( foundWorkers.isEmpty() ) {
			ACLMessage reqAgentMsg = new ACLMessage(ACLMessage.REQUEST);
			reqAgentMsg.addReceiver(searchAgentByType(CREATOR)[0].getName());
			reqAgentMsg.setContent(String.format("%s %s", CREATE, opp));
			send(reqAgentMsg);
			return;
		}

		ArrayList<AID> workersArray = new ArrayList<>(foundWorkers.stream().map(val -> val.getName()).toList());

		if (operationsRequested.get(opp) == null){
			operationsRequested.put(opp, workersArray);
		} else {
			workersArray.addAll(operationsRequested.get(opp));
			operationsRequested.put(opp, workersArray);
		}

		foundWorkers.forEach(ag -> 
			sendMessage(ag.getName().getLocalName(), ACLMessage.CFP,
				String.format("%s %s", PROFICIENCE, opp))
		);
	}

	private void searchSubordinatesByOperation(String opp, List<AID> unwantedAgents) {
		ArrayList<DFAgentDescription> foundWorkers = new ArrayList<>(
			Arrays.asList(searchAgentByType(opp)));

		ArrayList<AID> workersArray = new ArrayList<>(foundWorkers.stream().map(val -> val.getName()).toList());

		for ( AID notThisAgent : unwantedAgents ) 
			workersArray.remove(notThisAgent);

		if ( workersArray.isEmpty() ) {
			ACLMessage reqAgentMsg = new ACLMessage(ACLMessage.REQUEST);
			reqAgentMsg.addReceiver(searchAgentByType(CREATOR)[0].getName());
			reqAgentMsg.setContent(String.format("%s %s", CREATE, opp));
			send(reqAgentMsg);
			return;
		}
		
		if (operationsRequested.get(opp) == null){
			operationsRequested.put(opp, workersArray);
		} else {
			workersArray.addAll(operationsRequested.get(opp));
			operationsRequested.put(opp, workersArray );
		}

		workersArray.forEach(ag -> 
			sendMessage(ag.getLocalName(), ACLMessage.CFP,
					String.format("%s %s", PROFICIENCE, opp))
		);
	}

	private String prepareSendingData (ArrayList<Double> inputWorkingData) {
		StringBuilder builder = new StringBuilder();

		for ( double val : inputWorkingData ) {
			builder.append(String.format("%s ", Double.toString(val)));
		}

		return builder.toString().trim();
	}
}

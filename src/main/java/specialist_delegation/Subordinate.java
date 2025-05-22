package specialist_delegation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import specialist_delegation.strategies.AverageStrategy;
import specialist_delegation.strategies.MedianStrategy;
import specialist_delegation.strategies.ModeStrategy;
import specialist_delegation.strategies.SortStrategy;
import specialist_delegation.strategies.StdDeviationStrategy;
import specialist_delegation.strategies.Strategy;

public class Subordinate extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private transient Strategy strategyOp;

	private Map<String, Integer> agentSpeciality = new HashMap<>();

	@Override
	protected void setup() {
		addBehaviour(handleMessages());
		if ( !randomAgentMalfunction || rand.nextInt(11) != 10 ) {
			logger.log(Level.INFO, String.format("I'm the %s!", getLocalName()));
		} else {
			brokenAgent = true;
			logger.log(Level.WARNING,
				String.format("%s I'm agent %s and I have a malfunction! %s", ANSI_CYAN, getLocalName(), ANSI_RESET));
		}
		
		registerServices();

		ArrayList<DFAgentDescription> foundAgent = new ArrayList<>(
			Arrays.asList(searchAgentByType(CREATOR)));

		StringBuilder strBld = new StringBuilder();
		agentSpeciality.keySet().forEach(el -> 
			strBld.append(String.format("%s ", el))
		);
		sendMessage(foundAgent.get(0).getName().getLocalName(), ACLMessage.INFORM, String.format("%s %s", "CHECK", strBld.toString().trim()));
	}

	private void registerServices() {
		Object[] args = getArguments();

		if (args != null && args.length > 0) {
			int binaryCounter = 0;
			int proficiency = 0;
			String specArea = "speciality";

			for ( int i = 0; i < args.length; ++i ) {
				if ( binaryCounter == 0 ) {
					specArea = args[i].toString();
				} else {
					proficiency = Integer.parseInt(args[i].toString());

					agentSpeciality.put(specArea, proficiency);
				}
				binaryCounter = 1 - binaryCounter;
			}

			ArrayList<String> agServices = new ArrayList<>(agentSpeciality.keySet());
			agServices.add("Subordinate");

			registerDF(this, agServices);
		}
	}

	@Override
	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if (msg.getContent().startsWith(THANKS)) {
					logger.log(Level.INFO, String.format("%s RECEIVED THANKS FROM %s!", 
						getLocalName(), msg.getSender().getLocalName()));
				} else {
					logger.log(Level.INFO,
							String.format("%s RECEIVED AN UNEXPECTED MESSAGE FROM %s", getLocalName(),
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
				int recvPerformative = msg.getPerformative();

				switch ( recvPerformative ) {
					case ACLMessage.CFP:
						receivedCfpHandler(msg);
						break;
					case ACLMessage.ACCEPT_PROPOSAL:
						if ( !brokenAgent )
							receivedAcceptedProposalHandler(msg);
						break;
					default:
						logger.log(Level.INFO,
							String.format("%s %s RECEIVED UNEXPECTED MESSAGE PERFORMATIVE FROM %s %s", ANSI_YELLOW, getLocalName(),
									msg.getSender().getLocalName(), ANSI_RESET));
				}
			}

			private void receivedAcceptedProposalHandler(ACLMessage msg) {
				String reqOperation = msg.getContent().split(" ")[0];
					
				ACLMessage msg2 = msg.createReply();
				boolean strategySet = true;

				if ( !agentSpeciality.containsKey(reqOperation) ) {
				
					String msgContent = String.format("OPERATION %s UNKNOWN", reqOperation);
					msg2.setContent(msgContent);
					msg2.setPerformative(ACLMessage.REFUSE);
					logger.log(Level.INFO, String.format("%s SENT OPERATION UNKNOWN MESSAGE TO %s", getLocalName(),
						msg.getSender().getLocalName()));
				
				} else {

					workingData.clear();
					workingData = parseData(msg);
					dataSize = workingData.size();

					logger.log(Level.INFO, String.format("%s AGENT RECEIVED A TASK (%s) AND DATA: %s!",
							getLocalName(), reqOperation, workingData.toString()));

					switch (reqOperation) {
						case AVERAGE:
							strategyOp = new AverageStrategy();
							break;
						case MEDIAN:
							strategyOp = new MedianStrategy();
							break;
						case MODE:
							strategyOp = new ModeStrategy();
							break;
						case STD_DEVIATION:
							strategyOp = new StdDeviationStrategy();
							break;
						case SORT:
							strategyOp = new SortStrategy();
							break;
						default:
							strategySet = false;
					}
					
					msg2 = handleSetStrategy(msg, reqOperation, msg2, strategySet);
				}
				send(msg2);
			}

			private ACLMessage handleSetStrategy(ACLMessage msg, String reqOperation, ACLMessage msg2, boolean strategySet) {
				if (strategySet) {
					ArrayList<Double> objRet = strategyOp.executeOperation(workingData);
					String strRet = objRet.stream().map(val -> String.format("%s", Double.toString(val)))
						.collect(Collectors.joining(" ")).trim();
					
					logger.log(Level.INFO, String.format("%s I'm %s and I performed %s on data, resulting on: %s %s", ANSI_GREEN, 
						getLocalName(), reqOperation, strRet, ANSI_RESET));
					
					msg2.setPerformative(ACLMessage.INFORM);
					msg2.setContent(String.format("%s %s %s %d %s", INFORM, reqOperation, DATA, objRet.size(), strRet));
					
					logger.log(Level.INFO, String.format("%s SENT RETURN DATA MESSAGE TO %s", getLocalName(),
						msg.getSender().getLocalName()));
				} else {
					logger.log(Level.INFO,
						String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
							msg.getSender().getLocalName()));
						
					String msgContent = String.format("OPERATION %s UNKNOWN", reqOperation);
					msg2.setContent(msgContent);
					msg2.setPerformative(ACLMessage.REFUSE);
					logger.log(Level.INFO, String.format("%s SENT OPERATION UNKNOWN MESSAGE TO %s", getLocalName(),
						msg.getSender().getLocalName()));
				}

				return msg2;
			}

			private void receivedCfpHandler(ACLMessage msg) {
				if (msg.getContent().startsWith(PROFICIENCE)) {
					String [] splittedMsg = msg.getContent().split(" ");

					ACLMessage replyMsg = msg.createReply();

					int operationSpeciality = ( agentSpeciality.get(splittedMsg[1]) == null ? 0 : agentSpeciality.get(splittedMsg[1]));
					String replyContent = String.format("%s %s %d", PROFICIENCE, splittedMsg[1], operationSpeciality);

					replyMsg.setContent(replyContent);
					replyMsg.setPerformative(ACLMessage.PROPOSE);
					send(replyMsg);
				} else {
					logger.log(Level.INFO,
							String.format("%s RECEIVED AN UNEXPECTED MESSAGE FROM %s", getLocalName(),
								msg.getSender().getLocalName()));
				}
			}
		};
	}
}

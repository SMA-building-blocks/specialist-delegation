package specialist_delegation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import specialist_delegation.strategies.AverageStrategy;
import specialist_delegation.strategies.MedianStrategy;
import specialist_delegation.strategies.ModeStrategy;
import specialist_delegation.strategies.SortStrategy;
import specialist_delegation.strategies.StdDeviationStrategy;
import specialist_delegation.strategies.Strategy;

public class Subordinate extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private Strategy strategyOp;

	private Map<String, Integer> agentSpeciality = new HashMap<>();


	@Override
	protected void setup() {

		logger.log(Level.INFO, "I'm the subordinate!");
		this.registerDF(this, "Subordinate", "subordinate");

		
		registerSpecialities();

		addBehaviour(handleMessages());
	}

	private void registerSpecialities() {
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
					this.registerDF(this, specArea, specArea);
				}
				binaryCounter = 1 - binaryCounter;
			}
		}

		
	}

	@Override
	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if (msg.getContent().startsWith(START)) {
					logger.log(Level.INFO, String.format("%s SUBORDINATE AGENT RECEIVED A TASK!", getLocalName()));

					ACLMessage msg2 = msg.createReply();

					msg2.setContent(THANKS);

					send(msg2);
					logger.log(Level.INFO, String.format("%s SENT THANKS MESSAGE TO %s", getLocalName(),
							msg.getSender().getLocalName()));
				} else if (msg.getContent().startsWith(THANKS)) {
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
	protected OneShotBehaviour handleRequest(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String reqOperation = msg.getContent().split(" ")[0];

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
						logger.log(Level.INFO,
								String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG,
										msg.getSender().getLocalName()));
						break;
				}

				ArrayList<Double> objRet = strategyOp.executeOperation(workingData);
				String strRet = objRet.stream().map(val -> String.format("%s", Double.toString(val))).collect(Collectors.joining(" ")).trim();

				logger.log(Level.INFO, String.format("%s I'm %s and I performed %s on data, resulting on: %s %s", ANSI_GREEN, 
						getLocalName(), reqOperation, strRet, ANSI_RESET));

				ACLMessage msg2 = msg.createReply();
				msg2.setPerformative(ACLMessage.INFORM);
				msg2.setContent(String.format("%s %s %s %d %s", INFORM, reqOperation, DATA, objRet.size(), strRet));
				send(msg2);

				logger.log(Level.INFO, String.format("%s SENT RETURN DATA MESSAGE TO %s", getLocalName(),
						msg.getSender().getLocalName()));
			}
		};
	}
}

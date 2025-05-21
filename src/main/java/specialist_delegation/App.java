package specialist_delegation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Class that set the main agent and it's actions
 */
public class App extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private static List<String>  waitingAgents = Collections.synchronizedList(new ArrayList<>());
	int workersQuorum = 0;

	@Override
	protected void setup() {

		loggerSetup();

		registerDF(this, "Creator", "creator");
		addBehaviour(handleMessages());

		logger.log(Level.INFO, "Starting Agents...");

		logger.log(Level.INFO, "Creating Workers...");

		ArrayList<String> workersName = new ArrayList<>();

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			workersQuorum = Integer.parseInt(args[0].toString());
			specialities_qt = Math.min(Integer.parseInt(args[1].toString()), originalOperations.size());
			RANDOM_AGENT_MALFUNCTION = (Integer.parseInt(args[2].toString()) > 0);
		}

		for (int i = 0; i < workersQuorum; ++i) workersName.add("subordinate_" + i);

		try {
			AgentContainer container = getContainerController();

			workersName.forEach(worker -> {
				this.launchAgent(worker, "specialist_delegation.Subordinate", generateSpeciality(specialities_qt));
				logger.log(Level.INFO, String.format("%s CREATED AND STARTED NEW WORKER: %s ON CONTAINER %s",
						getLocalName(), worker, container.getName()));
			});
		} catch (Exception any) {
			logger.log(Level.SEVERE, String.format("%s ERROR WHILE CREATING AGENTS %s", ANSI_RED, ANSI_RESET));
			any.printStackTrace();
		}

		String m1AgentName = "Manager";
		launchAgent(m1AgentName, "specialist_delegation.Manager", null);

		logger.log(Level.INFO, "Agents started...");
		pauseSystem();
		// send them a message demanding start
		logger.log(Level.INFO, "Starting system!");

		dataSize = rand.nextInt(5, 11);
		String numbers = generateData(dataSize);
		String content = String.format("START DATA %d %s", dataSize, numbers);

		sendMessage(m1AgentName, ACLMessage.INFORM, content);
		logger.log(Level.INFO, String.format("%s SENT START MESSAGE TO %s", getLocalName(), m1AgentName));
	}

	private void pauseSystem() {
		try {
			logger.log(Level.WARNING, String.format(
					"%s The system is paused -- this action is here only to let you activate the sniffer on the agents, if you want (see documentation) %s",
					ANSI_YELLOW, ANSI_RESET));
			logger.log(Level.WARNING,
					String.format("%s Press enter in the console to start the agents %s", ANSI_YELLOW, ANSI_RESET));
			System.in.read();
		} catch (IOException e) {
			logger.log(Level.SEVERE, String.format("%s ERROR STARTING THE SYSTEM %s", ANSI_RED, ANSI_RESET));
			e.printStackTrace();
		}
	}

	private void launchAgent(String agentName, String className, Object[] args) {
		try {
			AgentContainer container = getContainerController(); // get a container controller for creating new agents
			AgentController newAgent = container.createNewAgent(agentName, className, args);
			newAgent.start();
		} catch (Exception e) {
			logger.log(Level.SEVERE, String.format("%s ERROR WHILE LAUNCHING AGENTS %s", ANSI_RED, ANSI_RESET));
			e.printStackTrace();
		}
	}

	private String generateData(int newDataSize) {

		StringBuilder newData = new StringBuilder();

		for (int i = 0; i < newDataSize; i++) {
			newData.append(String.format("%d ", rand.nextInt(1, 101)));
		}

		return newData.toString().trim();
	}

	private Object [] generateSpeciality(int specQuant) {
		Object [] specs = new Object[2 * specQuant];

		Collections.shuffle(originalOperations);
		for ( int i = 0; i < specQuant; ++i ) {
			specs[2 * i] = originalOperations.get(i);

			specs[(2 * i) + 1] = rand.nextInt(MIN_PROFICIENCE, MAX_PROFICIENCE + 1);
		}

		return specs;
	}

	@Override
	protected OneShotBehaviour handleRequest(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if (msg.getContent().startsWith("CREATE")) {
					String [] splittedMsg = msg.getContent().split(" ");

					try {
						Object [] newAgentSpec = { splittedMsg[1], rand.nextInt(MIN_PROFICIENCE, MAX_PROFICIENCE + 1) };
						String worker = String.format("%s%d", "subordinate_", workersQuorum++);

						launchAgent(worker, "specialist_delegation.Subordinate", newAgentSpec);	
						waitingAgents.add(worker);

						AgentContainer container = getContainerController();
						logger.log(Level.INFO, String.format("%s CREATED AND STARTED NEW WORKER: %s ON CONTAINER %s",
								getLocalName(), worker, container.getName()));	
					} catch (Exception any) {
						logger.log(Level.SEVERE, String.format("%s ERROR WHILE CREATING AGENTS %s", ANSI_RED, ANSI_RESET));
						any.printStackTrace();
					}
				}
			}
		};
	}

	@Override
	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				if ( msg.getContent().startsWith("CHECK") && waitingAgents.contains(msg.getSender().getLocalName()) ) {
					String [] splittedMsg = msg.getContent().split(" ");

					ArrayList<DFAgentDescription> foundAgent = new ArrayList<>(Arrays.asList(searchAgentByType("Manager")));
					sendMessage(foundAgent.get(0).getName().getLocalName(), ACLMessage.INFORM, 
						String.format("%s %s %s", "CREATED", splittedMsg[1], msg.getSender().getLocalName()));

					logger.log(Level.INFO, String.format("%s RECEIVED A CHECK MESSAGE FROM AGENT %s WITH OPERATION %s",
								getLocalName(), msg.getSender().getLocalName(), splittedMsg[1]));	

					waitingAgents.remove(msg.getSender().getLocalName());
				}
			}
		};
	}
}
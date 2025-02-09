package uk.ac.cam.cares.jps.base.config;

public interface JPSConstants {

	static String KNOWLEDGE_BASE_JPS = "jps";
	static String KNOWLEDGE_BASE_PATH_JPS_DATASET = "/jps/dataset";
	
	static String SCENARIO_NAME_BASE = "base";
	static String SCENARIO_SUBDIR_KB = "kb";
	static String SCENARIO_SUBDIR_DATA = "data";
	static String SCENARIO_NAME_TEST = "testPOWSYSNuclearStartSimulationAndProcessResultAgentCallForTestScenario";
	static String SCENARIO_JPS_CONTEXT = "jpscontext";
	static String SCENARIO_URL = "scenariourl";
	static String SCENARIO_USE_CASE_URL = "usecaseurl";
	static String SCENARIO_RESOURCE = "scenarioresource";
	static String SCENARIO_DATASET = "dataset";
	static String SCENARIO_AGENT = "scenarioagent";
	static String SCENARIO_AGENT_OPERATION = "scenarioagentoperation";
	static String SCENARIO_SIMULATION_TIME = "simulationtime";
	
	static String QUERY_SPARQL_QUERY = "sparqlquery";
	static String QUERY_SPARQL_UPDATE = "sparqlupdate";
	static String QUERY_FILE = "file";
	
	static String RUN_SIMULATION = "runsimulation";
	
	/**
	 * If true then all read files are copied into the scenario bucket.
	 * If false then copy-on-write, i.e. files are only copied into the scenario bucket if they are changed.
	 */
	// TODO-AE SC make this configurable
	static boolean COPY_ON_READ = true;
	
	public static final String SCENARIO_OPTION_COPY_ON_READ = "copyonread";
}

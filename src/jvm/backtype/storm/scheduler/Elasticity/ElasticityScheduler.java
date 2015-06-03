package backtype.storm.scheduler.Elasticity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.ExecutorDetails;
import backtype.storm.scheduler.IScheduler;
import backtype.storm.scheduler.SchedulerAssignment;
import backtype.storm.scheduler.Topologies;
import backtype.storm.scheduler.EvenScheduler;
import backtype.storm.scheduler.TopologyDetails;
import backtype.storm.scheduler.WorkerSlot;
import backtype.storm.scheduler.Elasticity.GetStats.ComponentStats;
import backtype.storm.scheduler.Elasticity.MsgServer.MsgServer;
import backtype.storm.scheduler.Elasticity.Strategies.*;

public class ElasticityScheduler implements IScheduler {
	private static final Logger LOG = LoggerFactory
			.getLogger(ElasticityScheduler.class);
	@SuppressWarnings("rawtypes")
	
	/**
	 * Number of machines to remove during scale-in
	 */
	private final Integer NUM_MACHINES_REMOVE_FOR_SCALE_IN = 4;
	
	private Map _conf;

	@Override
	public void prepare(@SuppressWarnings("rawtypes") Map conf) {
		_conf = conf;
	}

	@Override
	public void schedule(Topologies topologies, Cluster cluster) {
		LOG.info("\n\n\nRerunning ElasticityScheduler...");

		/**
		 * Starting msg server
		 */
		MsgServer msgServer = MsgServer.start(5001);

		/**
		 * Get Global info
		 */
		GlobalState globalState = GlobalState
				.getInstance("ElasticityScheduler");
		globalState.updateInfo(cluster, topologies);

		LOG.info("Global State:\n{}", globalState);

		/**
		 * Get stats
		 */
		GetStats stats = GetStats.getInstance("ElasticityScheduler");

		/**
		 * Start Scheduling
		 */
		for (TopologyDetails topo : topologies.getTopologies()) {
			globalState.logTopologyInfo(topo);
			String status = HelperFuncs.getStatus(topo.getId());
			LOG.info("status: {}", status);

			/**
			 * get message if any form client
			 */
			MsgServer.Signal signal = msgServer.getMessage();
			
			/**
			 * check if scale-out operation needs to occur
			 */
			if(signal == MsgServer.Signal.ScaleOut || (globalState.rebalancingState == MsgServer.Signal.ScaleOut && status.equals("REBALANCING"))){
				this.scaleOut(msgServer, topo, topologies, globalState, stats, cluster);
				globalState.rebalancingState = MsgServer.Signal.ScaleOut;
			} 
			/**
			 * check if scale-in operation needs to occur
			 */
			else if (signal == MsgServer.Signal.ScaleIn) {
				this.scaleIn(msgServer, topo, topologies, globalState, stats, cluster);
			} else {
				LOG.info("ID: {} NAME: {}", topo.getId(), topo.getName());
				LOG.info("Unassigned Executors for {}: ", topo.getName());

				for (Map.Entry<ExecutorDetails, String> k : cluster
						.getNeedsSchedulingExecutorToComponents(topo)
						.entrySet()) {
					LOG.info("{} -> {}", k.getKey(), k.getValue());
				}

				LOG.info("running EvenScheduler now...");
				new backtype.storm.scheduler.EvenScheduler().schedule(
						topologies, cluster);

				globalState.storeState(cluster, topologies);
				globalState.isBalanced = false;
			}

			LOG.info("Current Assignment: {}",
					HelperFuncs.nodeToTask(cluster, topo.getId()));
		}
		if (topologies.getTopologies().size() == 0) {
			globalState.clearStoreState();
		}

	}
	
	public void scaleIn(MsgServer msgServer, TopologyDetails topo, Topologies topologies, GlobalState globalState, GetStats stats, Cluster cluster) {
		LOG.info("/*** Scaling In ***/");
		
		StellaInStrategy si = new StellaInStrategy(globalState, stats, topo, cluster, topologies);
		TreeMap<Node, Integer> rankMap = si.StrategyScaleInAll();

		ScaleInETPStrategy strategy= new ScaleInETPStrategy(globalState, stats, topo, cluster, topologies, rankMap);

		
		strategy.removeNodesBySupervisorId(NUM_MACHINES_REMOVE_FOR_SCALE_IN);
		
		Map<WorkerSlot, List<ExecutorDetails>> schedMap = strategy
				.getNewScheduling();
		LOG.info("SchedMap: {}", schedMap);
		if (schedMap != null) {
			cluster.freeSlots(schedMap.keySet());
			for (Map.Entry<WorkerSlot, List<ExecutorDetails>> sched : schedMap
					.entrySet()) {
				cluster.assign(sched.getKey(),
						topo.getId(), sched.getValue());
				LOG.info("Assigning {}=>{}",
						sched.getKey(), sched.getValue());
			}
		}
		
		globalState.rebalancingState = MsgServer.Signal.ScaleIn;
	}
	
	public void scaleOut(MsgServer msgServer, TopologyDetails topo, Topologies topologies, GlobalState globalState, GetStats stats, Cluster cluster) {
		String status = HelperFuncs.getStatus(topo.getId());
		if (msgServer.isRebalance() == true) {
			if (globalState.stateEmpty() == false) {
				List<Node> newNodes = globalState.getNewNode();
				
				if (newNodes.size() > 0) {

					LOG.info("Increasing parallelism...");
					StellaOutStrategy strategy = new StellaOutStrategy(globalState, stats, topo, cluster, topologies);
					HashMap<Component, Integer> compMap = strategy.StellaStrategy(new HashMap<String, Component>());
					
					HelperFuncs.changeParallelism2(compMap, topo);

				}

			}
		} else if (status.equals("REBALANCING")) {
			if (globalState.isBalanced == false) {
				LOG.info("Rebalancing...{}=={}", cluster
						.getUnassignedExecutors(topo).size(), topo
						.getExecutors().size());
				if (cluster.getUnassignedExecutors(topo).size() == topo
						.getExecutors().size()) {
					if (globalState.stateEmpty() == false) {
						LOG.info("Unassigned executors: {}", cluster.getUnassignedExecutors(topo));
						LOG.info("Making migration assignments...");
						globalState.schedState.get(topo.getId());

						IncreaseParallelism strategy = new IncreaseParallelism(
								globalState, stats, topo, cluster,
								topologies);
						Map<WorkerSlot, List<ExecutorDetails>> schedMap = strategy
								.getNewScheduling();
						LOG.info("SchedMap: {}", schedMap);
						if (schedMap != null) {
							for (Map.Entry<WorkerSlot, List<ExecutorDetails>> sched : schedMap
									.entrySet()) {
								cluster.assign(sched.getKey(),
										topo.getId(), sched.getValue());
								LOG.info("Assigning {}=>{}",
										sched.getKey(), sched.getValue());
							}
						}

					}

					globalState.isBalanced = true;
				}
			}
		
		}
	}
}

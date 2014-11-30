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
import backtype.storm.scheduler.Elasticity.Strategies.*;

public class ElasticityScheduler implements IScheduler {
	private static final Logger LOG = LoggerFactory
			.getLogger(ElasticityScheduler.class);
	@SuppressWarnings("rawtypes")
	private Map _conf;

	@Override
	public void prepare(@SuppressWarnings("rawtypes") Map conf) {
		_conf = conf;
	}

	@Override
	public void schedule(Topologies topologies, Cluster cluster) {
		LOG.info("\n\n\nRerunning ElasticityScheduler...");
		
		/**
		 * Get Global info
		 */
		GlobalState globalState = GlobalState.getInstance("ElasticityScheduler");
		globalState.updateInfo(cluster, topologies);

		LOG.info("Global State:\n{}", globalState);

		/**
		 * Get stats
		 */
		GetStats stats = GetStats.getInstance("ElasticityScheduler");
		stats.getStatistics();
		//LOG.info(stats.printTransferThroughputHistory());
		//LOG.info(stats.printEmitThroughputHistory());
		//LOG.info(stats.printExecuteThroughputHistory());
		/**
		 * Start hardware monitoring server
		 */
		Master server = Master.getInstance();

		/**
		 * Start Scheduling
		 */
		for (TopologyDetails topo : topologies.getTopologies()) {
			String status = HelperFuncs.getStatus(topo.getId());
			LOG.info("status: {}", status);
//			long unixTime = (System.currentTimeMillis() / 1000)
//					- stats.startTimes.get(topo.getId());
//			LOG.info("Time: {}", unixTime);
//			if(unixTime > 120 && globalState.isBalanced==false) {
//				HelperFuncs.changeParallelism2(topo, "exclaim2", 4);
//				globalState.isBalanced=true;
//			}
			
			
//			globalState.logTopologyInfo(topo);
//			String status = HelperFuncs.getStatus(topo.getId());
//			LOG.info("status: {}", status);
//			if (status.equals("REBALANCING")) {
//				if (globalState.balancingState == 0) {
//					LOG.info("Rebalancing...{}=={}", cluster
//							.getUnassignedExecutors(topo).size(), topo
//							.getExecutors().size());
//					if (cluster.getUnassignedExecutors(topo).size() == topo
//							.getExecutors().size()) {
//						if (globalState.stateEmpty() == false) {
//							LOG.info("Making migration assignments...");
//							
//							IncreaseParallelismTest strategy = new IncreaseParallelismTest(
//									globalState, stats, topo, cluster,
//									topologies);
//							Map<WorkerSlot, List<ExecutorDetails>> schedMap = strategy
//									.getNewScheduling();
//							if(schedMap != null) {
//								for (Map.Entry<WorkerSlot, List<ExecutorDetails>> sched : schedMap
//										.entrySet()) {
//									HelperFuncs.assignTasks(sched.getKey(),
//											topo.getId(), sched.getValue(),
//											cluster, topologies);
//									LOG.info("Assigning {}=>{}", sched.getKey(),
//											sched.getValue());
//								}
//							}
//							
//						}
//						
//						globalState.balancingState = 1;
//					}
//				} 
//			} else {
//				if(globalState.balancingState==1) {
//					HelperFuncs.changeParallelism(topo, "exclaim2", 4);
//					globalState.balancingState=2;
//				} else {
//
//					LOG.info("ID: {} NAME: {}", topo.getId(), topo.getName());
//					LOG.info("Unassigned Executors for {}: ", topo.getName());
//	
//					for (Map.Entry<ExecutorDetails, String> k : cluster
//							.getNeedsSchedulingExecutorToComponents(topo)
//							.entrySet()) {
//						LOG.info("{} -> {}", k.getKey(), k.getValue());
//					}
//	
//					LOG.info("running EvenScheduler now...");
//					new backtype.storm.scheduler.EvenScheduler().schedule(
//							topologies, cluster);
//	
//					globalState.storeState(cluster, topologies);
//					globalState.isBalanced = false;
//				}
//			}
//
//			LOG.info("Current Assignment: {}",
//					HelperFuncs.nodeToTask(cluster, topo.getId()));
		}
		LOG.info("running EvenScheduler now...");
		new backtype.storm.scheduler.EvenScheduler().schedule(
				topologies, cluster);
		if (topologies.getTopologies().size() == 0) {
			globalState.clearStoreState();
		}

	}
}

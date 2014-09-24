package backtype.storm.scheduler.Elasticity;

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
import backtype.storm.scheduler.Elasticity.GetTopologyInfo.Component;


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
		for (TopologyDetails topo : topologies.getTopologies()) {
			String status = HelperFuncs.getStatus(topo.getId());
			LOG.info("status: {}", status);
			if(status.equals("REBALANCING")) {
				LOG.info("Do nothing....");
				LOG.info("ID: {} NAME: {}", topo.getId(), topo.getName());
				LOG.info("Unassigned Executors for {}: ", topo.getName());
				for (Map.Entry<ExecutorDetails, String> k : cluster.getNeedsSchedulingExecutorToComponents(topo).entrySet()) {
					LOG.info("{} -> {}", k.getKey(), k.getValue());
				}
				LOG.info("running EvenScheduler now...");
				
			} else {
				LOG.info("ID: {} NAME: {}", topo.getId(), topo.getName());
				LOG.info("Unassigned Executors for {}: ", topo.getName());
				for (Map.Entry<ExecutorDetails, String> k : cluster.getNeedsSchedulingExecutorToComponents(topo).entrySet()) {
					LOG.info("{} -> {}", k.getKey(), k.getValue());
				}
				LOG.info("running EvenScheduler now...");
				new backtype.storm.scheduler.EvenScheduler().schedule(topologies, cluster);
			}
			
			
			LOG.info("Current Assignment: {}", HelperFuncs.nodeToTask(cluster, topo.getId()));
		}
		GetStats gs = GetStats.getInstance("ElasticityScheduler");
		GetTopologyInfo gt = new GetTopologyInfo();
		gs.getStatistics();
		gt.getTopologyInfo();
		LOG.info("Topology layout: {}", gt.all_comp);
		
		TreeMap<Component, Integer> comp = Strategies.centralityStrategy(gt.all_comp);
		LOG.info("priority queue: {}", comp);

		
	}
}

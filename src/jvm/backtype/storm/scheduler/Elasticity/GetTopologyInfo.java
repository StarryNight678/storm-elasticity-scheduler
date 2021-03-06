package backtype.storm.scheduler.Elasticity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.generated.Bolt;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.GlobalStreamId;
import backtype.storm.generated.Grouping;
import backtype.storm.generated.Nimbus;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.StreamInfo;
import backtype.storm.generated.TopologySummary;
import backtype.storm.scheduler.ExecutorDetails;

public class GetTopologyInfo {

	// private static GetTopologyInfo instance = null;
	private HashMap<String, Component> bolts = null;
	private HashMap<String, Component> spouts = null;
	public HashMap<String, Component> all_comp = null;

	private static final Logger LOG = LoggerFactory
			.getLogger(GetTopologyInfo.class);

	public GetTopologyInfo() {
		this.bolts = new HashMap<String, Component>();
		this.spouts = new HashMap<String, Component>();
		this.all_comp = new HashMap<String, Component>();
	}

	public void getTopologyInfo(String topoId) {
		LOG.info("Getting Topology info...");

		TSocket tsocket = new TSocket("localhost", 6627);
		TFramedTransport tTransport = new TFramedTransport(tsocket);
		TBinaryProtocol tBinaryProtocol = new TBinaryProtocol(tTransport);
		Nimbus.Client client = new Nimbus.Client(tBinaryProtocol);
		try {
			tTransport.open();

			ClusterSummary clusterSummary = client.getClusterInfo();
			List<TopologySummary> topologies = clusterSummary.get_topologies();

			for (TopologySummary topo : topologies) {
				if (topo.get_id().equals(topoId) == true){
					StormTopology storm_topo = client
							.getTopology(topo.get_id());
					// spouts
					for (Map.Entry<String, SpoutSpec> s : storm_topo
							.get_spouts().entrySet()) {
						if (s.getKey().matches("(__).*") == false) {
							Component newComp = null;
							if (this.all_comp.containsKey(s.getKey())) {
								newComp = this.all_comp.get(s.getKey());
							} else {
								newComp = new Component(s.getKey());

								this.all_comp.put(s.getKey(), newComp);
							}

							for (Map.Entry<GlobalStreamId, Grouping> entry : s
									.getValue().get_common().get_inputs()
									.entrySet()) {

								if (entry.getKey().get_componentId()
										.matches("(__).*") == false) {

									newComp.parents.add(entry.getKey()
											.get_componentId());
									if (this.all_comp.containsKey(entry
											.getKey().get_componentId()) == false) {
										this.all_comp.put(entry.getKey()
												.get_componentId(),
												new Component(entry.getKey()
														.get_componentId()));
									}
									this.all_comp.get(entry.getKey()
											.get_componentId()).children.add(s
											.getKey());
								}
							}

						}
					}
					// bolt
					for (Map.Entry<String, Bolt> s : storm_topo.get_bolts()
							.entrySet()) {
						if (s.getKey().matches("(__).*") == false) {
							Component newComp = null;
							if (this.all_comp.containsKey(s.getKey())) {
								newComp = this.all_comp.get(s.getKey());
							} else {
								newComp = new Component(s.getKey());
								this.all_comp.put(s.getKey(), newComp);
							}
							for (Map.Entry<GlobalStreamId, Grouping> entry : s
									.getValue().get_common().get_inputs()
									.entrySet()) {
								if (entry.getKey().get_componentId()
										.matches("(__).*") == false) {

									newComp.parents.add(entry.getKey()
											.get_componentId());
									if (this.all_comp.containsKey(entry
											.getKey().get_componentId()) == false) {
										this.all_comp.put(entry.getKey()
												.get_componentId(),
												new Component(entry.getKey()
														.get_componentId()));
									}
									this.all_comp.get(entry.getKey()
											.get_componentId()).children.add(s
											.getKey());
								}
							}
						}
					}
				}
			}
		} catch (TException e) {
			e.printStackTrace();
		} catch (NotAliveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

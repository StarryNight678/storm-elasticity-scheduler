package backtype.storm.scheduler.Elasticity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.ExecutorDetails;
import backtype.storm.scheduler.SupervisorDetails;
import backtype.storm.scheduler.WorkerSlot;


public class Node {
	
	public String supervisor_id;
	public SupervisorDetails sup;
	public String hostname;
	List<WorkerSlot> slots;
	List<ExecutorDetails> execs;
	Map<WorkerSlot, List<ExecutorDetails>> slot_to_exec;
	
	public Node(String supervisor_id, Cluster cluster) {
		this.sup = cluster.getSupervisors().get(supervisor_id);
		this.hostname = this.sup.getHost();
		this.supervisor_id = sup.getId();
		this.slots = cluster.getAssignableSlots(sup);
		this.execs = new ArrayList<ExecutorDetails>();
		slot_to_exec = new HashMap<WorkerSlot, List<ExecutorDetails>>();
		for(WorkerSlot ws : this.slots) {
			slot_to_exec.put(ws, new ArrayList<ExecutorDetails>());
		}
		
		
	}
	
}

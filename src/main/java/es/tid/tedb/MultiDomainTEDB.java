package es.tid.tedb;

import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;


public interface MultiDomainTEDB extends TEDB {

	public void addInterdomainLink( Object localDomainID, Object localRouterASBR, long localRouterASBRIf, Object remoteDomainID, Object remoteRouterASBR, long remoteRouterASBRIf, TE_Information te_info );
	public void addReachabilityIPv4(Inet4Address domainId,Inet4Address aggregatedIPRange,int prefix);


	DirectedWeightedMultigraph<Object, InterDomainEdge> getNetworkDomainGraph();
}

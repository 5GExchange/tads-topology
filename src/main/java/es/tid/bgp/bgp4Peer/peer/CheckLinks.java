package es.tid.bgp.bgp4Peer.peer;

import es.tid.tedb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

//import java.net.InetAddress;

/**
 * Class to send periodically the topology. It sends the topology to the active BGP4 sessions.
 * @author pac
 *
 */
public class CheckLinks implements Runnable {

	/**
	 * 1= optical
	 * 0= L3
	 */
	//TEDBs
	private Hashtable<String, TEDB> teds;

	// Multi-domain TEDB to redistribute Multi-domain Topology
	private MultiDomainTEDB md;

	private SendTopology sendTopology;
	private Logger log;


	public CheckLinks() {
		log = LoggerFactory.getLogger("BGP4Peer");
	}


	public void configure(Hashtable<String, TEDB> intraTEDBs, SendTopology sendTopology, MultiDomainTEDB multiTED) {
		this.teds = intraTEDBs;
		this.sendTopology = sendTopology;
		this.md = multiTED;
	}

	/**
	 * Function to send the topology database.
	 */


	public void run() {
		log.debug("Run of check links.");

		try {
			LinkedList<InterDomainEdge> interdomainLinks = md.getInterDomainLinks();

			log.debug(interdomainLinks.size()+ "  in interdomainLinks graph");

			Enumeration keys = null;
			if((md!=null)&&(md.getTemps()!=null)){
				keys=md.getTemps().keys();
			}
			String key;
			boolean sfound = false;
			boolean dfound = false;

			if ((md!=null)&&(md.getTemps()!=null)){
				if (md.getTemps().size()>0){
					if(keys !=null){
						while (keys.hasMoreElements()) {
							key = (String) keys.nextElement();
							InterDomainEdge edge = md.getTemps().get(key);
							log.debug("CHeck links: Looking for temp interdomain link "+ edge.toString());

							Enumeration<String> iter = teds.keys();
							while (iter.hasMoreElements()) {
								String domainID = iter.nextElement();
								if ((domainID != null)&&(!domainID.equals("multidomain"))) {
									log.debug("temp procedure checking domain_id: " + domainID);
									TEDB ted = teds.get(domainID);
									if (ted instanceof DomainTEDB) {
										Iterator<Object> vertexIt = ((DomainTEDB) ted).getIntraDomainLinksvertexSet().iterator();
										while (vertexIt.hasNext()) {
											Inet4Address nodex = null;
											long node = 0L;
											Object v = vertexIt.next();
											Node_Info node_info = null;
											if (v instanceof Inet4Address) {
												nodex = (Inet4Address) v;
												node_info = ((DomainTEDB) ted).getNodeTable().get(nodex);
											} else if (v instanceof Long) {
												node = (long) v;
												node_info = ((DomainTEDB) ted).getNodeTable().get(node);
											}
											if (node_info != null) {

												String nodeip = node_info.getIpv4AddressLocalNode().getCanonicalHostName();
												log.debug("Current node ID=" + nodeip);
												//src node
												if (edge.getLocal_Node_Info()==null){
													if(edge.getSrc_router_id() instanceof Inet4Address) {
														if (((Inet4Address) edge.getSrc_router_id()).getHostAddress().equals(nodeip)) {
															log.debug("Node info id = to src");
															sfound = true;
															edge.setLocal_Node_Info(node_info);
															if (v instanceof Long) {
																edge.setSrc_router_id(node);
																log.debug("ISIS");
															}
															else
																log.debug("ipv4");

														}
														else
															log.debug("edge src and node ip are different");
													}
													else
														log.debug("not ipv4");
												}
												else{
													log.debug("Src info already present");
													sfound=true;
												}

												//dest node
												if (edge.getRemote_Node_Info()==null){
													if(edge.getDst_router_id() instanceof Inet4Address) {
														if (((Inet4Address) edge.getDst_router_id()).getHostAddress().equals(nodeip)) {
															log.debug("Node info id = to dst");
															dfound = true;
															edge.setRemote_Node_Info(node_info);
															if (v instanceof Long) {
																edge.setDst_router_id(node);
																log.debug("ISIS");
															} else
																log.debug("ipv4");
															Inet4Address dom = null;
															try { // d_router_id_addr type: Inet4Address
																dom = (Inet4Address) Inet4Address.getByName(domainID);
															} catch (Exception e) { // d_router_id_addr type: DataPathID
																log.debug(e.toString());
															}
															if (dom != null) {
																md.getNetworkDomainGraph().addVertex(dom);
																edge.setDomain_dst_router(dom);
															} else
																log.debug("dom is null");
														}
														else{
															log.debug("edge dst and node ip are different");
															log.debug(((Inet4Address) edge.getDst_router_id()).getHostAddress());
															log.debug(nodeip);
														}
													}
													else
														log.debug("not ipv4");
												}
												else{
													log.debug("dst info already present");
													dfound=true;
												}


											}
											else
												log.debug("node info null");
										}
									}
									else
										log.debug("not a domainTEDB instance");

								}
								else
									log.debug("domain null or multidomani");

							}
							if(sfound&&dfound){
								//Only add if the source and destination domains are different

								//setInterDomainEdgeUpdateTime(localDomainID, LocalNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(), remoteDomainID, RemoteNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(), System.currentTimeMillis());
								edge.setComplete(true);
								md.getNetworkDomainGraph().addEdge((Inet4Address) edge.getDomain_src_router(), edge.getDomain_dst_router(), edge);
								md.getTemps().remove(key);
								log.info("Added interdomain link to md ted and sending update");
								sendTopology.sendLinkNLRI(md, teds);
								log.debug(edge.toString());
							}else{
								log.debug("This link is still not complete");
								if (dfound) log.debug("dst found");
								else log.debug("dst not found");
								if (sfound) log.debug("src found");
								else log.debug("src not found");
							}


						}
					}
				}//xx
				else
					log.debug("md temps size 0");

			}//xx
			else
				log.debug("md null or md.temp null");




			if (md!=null){
				if (md.getNetworkDomainGraph()!=null){
					log.debug("Number of nodes: "+ String.valueOf(md.getNetworkDomainGraph().vertexSet().size()));
					log.debug("Number of links: "+ String.valueOf(md.getNetworkDomainGraph().edgeSet().size()));
					log.debug("Number of domain: "+ String.valueOf(teds.size()));
					Enumeration<String> iter = teds.keys();
					while (iter.hasMoreElements()) {
						String domainID = iter.nextElement();
						if ((domainID != null) && (!domainID.equals("multidomain"))) {
							log.debug("temp procedure checking domain_id: " + domainID);
							TEDB ted = teds.get(domainID);
							if (ted instanceof DomainTEDB) {
								if (((DomainTEDB)ted).getMDPCE()!=null){
									log.info("Domain "+ domainID+" found MD-PCE with ip "+((DomainTEDB)ted).getMDPCE().getPCEipv4().getHostName() );
								}
								else
									log.info("No PCE info for domain "+ domainID );

							}

						}
					}
				}
				else
					log.debug("getNetworkDomainGraph is null");
			}
			else {
				log.debug("md is null");
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error("PROBLEM SENDING TOPOLOGY: " + e.toString());
		}

	}

}
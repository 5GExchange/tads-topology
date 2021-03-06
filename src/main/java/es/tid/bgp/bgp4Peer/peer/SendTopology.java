package es.tid.bgp.bgp4Peer.peer;

import com.google.common.base.Splitter;
import es.tid.bgp.bgp4.messages.BGP4Update;
import es.tid.bgp.bgp4.update.fields.*;
import es.tid.bgp.bgp4.update.fields.pathAttributes.*;
import es.tid.bgp.bgp4.update.tlv.*;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.*;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.*;
import es.tid.bgp.bgp4Peer.bgp4session.BGP4SessionsInformation;
import es.tid.bgp.bgp4Peer.bgp4session.GenericBGP4Session;
import es.tid.ospf.ospfv2.OSPFv2LinkStateUpdatePacket;
import es.tid.ospf.ospfv2.lsa.LSA;
import es.tid.ospf.ospfv2.lsa.LSATypes;
import es.tid.ospf.ospfv2.lsa.OSPFTEv2LSA;
import es.tid.ospf.ospfv2.lsa.tlv.LinkTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.*;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.tedb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

//import java.net.InetAddress;

/**
 * Class to send periodically the topology. It sends the topology to the active BGP4 sessions.
 * @author pac
 *
 */
public class SendTopology implements Runnable {

	/**
	 * 1= optical
	 * 0= L3
	 */
	private String BGPID;
	private int identifier = 0;
	private static final int IPV4_PART_COUNT = 4;
	private static final int IPV6_PART_COUNT = 8;
	private static final Splitter IPV4_SPLITTER = Splitter.on('.').limit(IPV4_PART_COUNT);
	//TEDBs 
	private Hashtable<String, TEDB> intraTEDBs;

	// Multi-domain TEDB to redistribute Multi-domain Topology
	private MultiDomainTEDB multiDomainTEDB;

	private boolean sendTopology;
	private boolean isTest = false;
	private BGP4SessionsInformation bgp4SessionsInformation;
	private Logger log;
	private int instanceId = 1;
	private int layer = 0; //0->IP, 1-> optical
	private boolean sendIntraDomainLinks = false;
	private int ASnumber = 1;
	private int LocalPref = 100;
	private boolean isASnumber = false;
	private boolean send4AS = false;


	private Inet4Address localBGPLSIdentifer;
	private Inet4Address localAreaID;

	public SendTopology() {
		log = LoggerFactory.getLogger("BGP4Peer");
	}

	public void configure(Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, boolean sendTopology, int instanceId, boolean sendIntraDomainLinks, MultiDomainTEDB multiTED) {
		configure(intraTEDBs, bgp4SessionsInformation, sendTopology, instanceId, sendIntraDomainLinks, multiTED, false, 0, 0, null);

	}

	public void configure(Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, boolean sendTopology, int instanceId, boolean sendIntraDomainLinks, MultiDomainTEDB multiTED, boolean test) {
		configure(intraTEDBs, bgp4SessionsInformation, sendTopology, instanceId, sendIntraDomainLinks, multiTED, test, 0, 0, null);

	}


	public void configure(Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, boolean sendTopology, int instanceId, boolean sendIntraDomainLinks, MultiDomainTEDB multiTED, int AS, int pref, String BGPIdent) {
		configure(intraTEDBs, bgp4SessionsInformation, sendTopology, instanceId, sendIntraDomainLinks, multiTED, false, AS, pref, BGPIdent);

	}


	public void configure(Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, boolean sendTopology, int instanceId, boolean sendIntraDomainLinks, MultiDomainTEDB multiTED, boolean test, int AS, int localPref, String BGPIdent) {
		this.intraTEDBs = intraTEDBs;
		this.bgp4SessionsInformation = bgp4SessionsInformation;
		this.sendTopology = sendTopology;
		this.instanceId = instanceId;
		this.sendIntraDomainLinks = sendIntraDomainLinks;
		this.multiDomainTEDB = multiTED;
		this.BGPID=BGPIdent;
		if (AS != 0) this.ASnumber = AS;
		this.isTest = test;
		if (localPref != 0) this.LocalPref = localPref;
		try {
			this.localAreaID = (Inet4Address) Inet4Address.getByName("0.0.0.0");
			this.localBGPLSIdentifer = (Inet4Address) Inet4Address.getByName("1.1.1.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public void configure(Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, boolean sendTopology, int instanceId, boolean sendIntraDomainLinks, MultiDomainTEDB multiTED, boolean test, int AS, String BGPIdent) {
		this.intraTEDBs = intraTEDBs;
		this.bgp4SessionsInformation = bgp4SessionsInformation;
		this.sendTopology = sendTopology;
		this.instanceId = instanceId;
		this.sendIntraDomainLinks = sendIntraDomainLinks;
		this.multiDomainTEDB = multiTED;
		this.isTest = test;
		this.BGPID=BGPIdent;
		this.ASnumber = AS;
		try {
			this.localAreaID = (Inet4Address) Inet4Address.getByName("0.0.0.0");
			this.localBGPLSIdentifer = (Inet4Address) Inet4Address.getByName("1.1.1.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Function to send the topology database.
	 */


	public void run() {
		log.debug("Run of Sending topology.");

		try {
			if (sendTopology) {
				if (!(bgp4SessionsInformation.getSessionList().isEmpty())) {
					if (multiDomainTEDB != null) {
						log.debug("Sending Multi-Domain TEDB");
						sendLinkNLRI(multiDomainTEDB, intraTEDBs);
					} else {
						log.debug("Sending from TEDB");
						Enumeration<TEDB> iter = intraTEDBs.elements();
						while (iter.hasMoreElements()) {

							sendLinkNLRI(iter.nextElement().getInterDomainLinks());
						}
					}

					if (sendIntraDomainLinks) {//Intradomain Links
						log.debug("sendIntraDomainLinks activated");
						Enumeration<String> iter = intraTEDBs.keys();
						while (iter.hasMoreElements()) {
							String domainID = iter.nextElement();
							//Andrea
							if (domainID != null) {
								log.debug("Sending TED from Domain " + domainID);

								TEDB ted = intraTEDBs.get(domainID);
								if (ted instanceof DomainTEDB) {
									if ((((DomainTEDB) ted).getIGPType() == 1) || (((DomainTEDB) ted).getIGPType() == 2)) {
										sendNodeNLRIISIS(((DomainTEDB) ted).getIntraDomainLinksvertexSet(), ((DomainTEDB) ted).getNodeTable());
									} else {
										sendNodeNLRI(((DomainTEDB) ted).getIntraDomainLinksvertexSet(), ((DomainTEDB) ted).getNodeTable());
									}
									if ((((DomainTEDB) ted).getIGPType() == 1) || (((DomainTEDB) ted).getIGPType() == 2)) {
										sendLinkNLRIISIS(((DomainTEDB) ted).getIntraDomainLinks(), domainID);
									} else {
										sendLinkNLRI(((DomainTEDB) ted).getIntraDomainLinks(), domainID);
									}//log.info(" XXXX ted.getNodeTable():"+ted.getNodeTable());
									if (((DomainTEDB) ted).getItResources() != null) {
										sendITNodeNLRI(domainID, ((DomainTEDB) ted).getItResources(),  ((DomainTEDB) ted).getItResources().getLearntFrom());
									}

									if (((DomainTEDB) ted).getMDPCE() != null && domainID != null) {
										log.info("Sending MDPCE address for Domain " + domainID + " with IP: " + ((DomainTEDB) ted).getMDPCE().getPCEipv4().getHostAddress());
										log.info((((DomainTEDB) ted).getMDPCE()).toString());
										sendMDPCENLRI(domainID, ((DomainTEDB) ted).getMDPCE(), ((DomainTEDB) ted).getMDPCE().getLearntFrom());
									}

								}
							}


						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("PROBLEM SENDING TOPOLOGY: " + e.toString());
		}

	}

	/**
	 * This function sends a BGP4 update message (encoded in a NodeNLRI) for each node in the set
	 *
	 * @param vertexIt
	 */
	private void sendNodeNLRI(Set<Object> vertexSet, Hashtable<Object, Node_Info> NodeTable) {
		Iterator<Object> vertexIt = vertexSet.iterator();
		//Enviamos primero los nodos. Un Node NLRI por cada nodo.
		while (vertexIt.hasNext()) {
			Inet4Address node = null;
			Object v = vertexIt.next();
			if (v instanceof es.tid.tedb.elements.Node) {
				try {
					node = (Inet4Address) Inet4Address.getByName(((es.tid.tedb.elements.Node) v).getAddress().get(0));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				node = (Inet4Address) v;
			}
			Node_Info node_info = NodeTable.get(node);
			if (node_info != null) {
				log.info("Sending node: (" + node + ")");//with node_info "+ NodeTable.get(node).toString());
				//Mandamos NodeNLRI
				BGP4Update update = createMsgUpdateNodeNLRI(node_info,  NodeTable.get(node).getLearntFrom());
				sendMessage(update);
			} else {
				log.debug("Node " + node + " HAS No Node_info in NodeTable");
			}

		}
	}

	private void sendNodeNLRIISIS(Set<Object> vertexSet, Hashtable<Object, Node_Info> NodeTable) {
		Iterator<Object> vertexIt = vertexSet.iterator();
		//Enviamos primero los nodos. Un Node NLRI por cada nodo.
		while (vertexIt.hasNext()) {
			long node = 0L;
			Object v = vertexIt.next();
			if (v instanceof es.tid.tedb.elements.Node) {
				log.debug("instance of Node");
				//node = Integer.valueOf(((es.tid.tedb.elements.Node) v).getAddress().get(0));
				node=((es.tid.tedb.elements.Node) v).getISIS_ID();
				log.debug("Send NLRI ISIS node id "+ String.valueOf(node));
			}
			else if (v instanceof Inet4Address){
				Inet4Address nodex = null;
				nodex = (Inet4Address) v;
				Node_Info node_info = NodeTable.get(nodex);
				if (node_info != null) {
					log.debug("Sending node: (" + nodex + ")");
					//Mandamos NodeNLRI
					BGP4Update update = createMsgUpdateNodeNLRI(node_info,  NodeTable.get(nodex).getLearntFrom());
					sendMessage(update);
				} else {
					log.debug("Node " + nodex + " HAS No Node_info in NodeTable");
				}
			}
			else {
				node = (long) v;
			}


			//log.info(" XXXX node: "+ node);
			Node_Info node_info = NodeTable.get(node);
			//log.info(" XXXX node_info: "+ node_info);
			if (node_info != null) {
				log.debug("Sending node: (" + node + ")");
				//Mandamos NodeNLRI
				BGP4Update update = createMsgUpdateNodeNLRIISIS(node_info, NodeTable.get(node).getLearntFrom());
				sendMessage(update);
			} else {
				log.debug("Node " + node + " HAS No Node_info in NodeTable");
			}

		}
	}

	/**
	 * This function sends a BGP4 update message (encoded in a ITNodeNLRI) for each node in the set
	 *
	 * @param vertexIt
	 */


	private void sendITNodeNLRI(String domainID, IT_Resources itResources, String learntFrom) {
		//Andrea
		log.debug("Sending IT Resources");
		BGP4Update update = createMsgUpdateITNodeNLRI(domainID, itResources, learntFrom);
		sendMessage(update);
//		Iterator<Object> vertexIt = vertexSet.iterator();	
//		//Enviamos primero los nodos. Un Node NLRI por cada nodo.
//		while (vertexIt.hasNext()){		
//			Inet4Address node = (Inet4Address)vertexIt.next();
//			//log.info(" XXXX node: "+ node);
//			Node_Info node_info = NodeTable.get(node);
//			//log.info(" XXXX node_info: "+ node_info);
//			if (node_info!=null){
//				log.debug("Sending node: ("+node+")");
//				//Mandamos NodeNLRI
//				BGP4Update update = createMsgUpdateNodeNLRI(node_info);
//				sendMessage(update);	
//			}else {
//				log.error("Node "+node+ " HAS NO node_info in NodeTable");
//			}
//			
//
//		}
	}

	private void sendMDPCENLRI(String domainID, PCEInfo IP, String learntFrom) {
		//Andrea
		log.debug("Sending PCE Address");
		BGP4Update update = createMsgUpdateMDPCENLRI(domainID, IP, learntFrom);
		sendMessage(update);
//		Iterator<Object> vertexIt = vertexSet.iterator();
//		//Enviamos primero los nodos. Un Node NLRI por cada nodo.
//		while (vertexIt.hasNext()){
//			Inet4Address node = (Inet4Address)vertexIt.next();
//			//log.info(" XXXX node: "+ node);
//			Node_Info node_info = NodeTable.get(node);
//			//log.info(" XXXX node_info: "+ node_info);
//			if (node_info!=null){
//				log.debug("Sending node: ("+node+")");
//				//Mandamos NodeNLRI
//				BGP4Update update = createMsgUpdateNodeNLRI(node_info);
//				sendMessage(update);
//			}else {
//				log.error("Node "+node+ " HAS NO node_info in NodeTable");
//			}
//
//
//		}
	}


	/**
	 * This function sends a BGP4 update message (encoded in a LinkNLRI) for each link in the list
	 *
	 * @param interdomainLinks
	 */
	private void sendLinkNLRI(LinkedList<InterDomainEdge> interdomainLinks) {


		if (true) {
			int lanID = 1; ///INVENTADOO
			ArrayList<Object> addressList = new ArrayList<>();
			ArrayList<Inet4Address> interfacesList = new ArrayList<Inet4Address>();
			Iterator<InterDomainEdge> edgeIt = interdomainLinks.iterator();
			while (edgeIt.hasNext()) {

				InterDomainEdge edge = edgeIt.next();
				if(edge.getComplete()) {
					Object source = null;
					Object dst = null;
					if (edge.getSrc_router_id() instanceof Inet4Address)
						source = (Inet4Address) edge.getSrc_router_id();
					if (edge.getSrc_router_id() instanceof Long)
						source = (long) edge.getSrc_router_id();
					if (edge.getDst_router_id() instanceof Inet4Address)
						dst = (Inet4Address) edge.getDst_router_id();
					if (edge.getDst_router_id() instanceof Long)
						dst = (Inet4Address) edge.getDst_router_id();
					if ((source != null) && (dst != null))
						log.debug("Sending ID Edge: (" + source.toString() + "in domain " + ((Inet4Address)((InterDomainEdge) edge).getDomain_src_router()).getCanonicalHostName() + "," + dst.toString() + ")");
					addressList = new ArrayList<Object>();
					addressList.add(0, source);
					addressList.add(1, dst);
					//Link Local Remote Identifiers
					ArrayList<Long> localRemoteIfList = null;
					localRemoteIfList = new ArrayList<Long>();
					localRemoteIfList.add(0, ((InterDomainEdge) edge).getSrc_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier());
					localRemoteIfList.add(1, ((InterDomainEdge) edge).getDst_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier());
					interfacesList.add(0, ((InterDomainEdge) edge).getLocalInterfaceIPv4());
					interfacesList.add(1, ((InterDomainEdge) edge).getNeighborIPv4());


					ArrayList<String> domainList = new ArrayList<String>(2);
					//FIXME: chequear
					TE_Information te_info = ((InterDomainEdge) edge).getTE_info();

					domainList.add(((Inet4Address) edge.getDomain_src_router()).getHostAddress());
					//System.out.println("SRC Domain is "+((Inet4Address)edge.getDomain_src_router()).getHostAddress().toString() );
					domainList.add(((Inet4Address) edge.getDomain_dst_router()).getHostAddress());
					log.debug("Source Domain is " + (Inet4Address) edge.getDomain_dst_router());
					BGP4Update update = createMsgUpdateLinkNLRI2(null, addressList, localRemoteIfList, lanID, domainList, false, te_info, edge.getLearntFrom(),
							interfacesList);
					update.setLearntFrom(edge.getLearntFrom());
					log.debug("Update message Created for  interdomain edge");
					sendMessage(update);
				}
			}
		}

	}
	//Corrente....
	//used function that checks if there is some uncomplete intedomain links and then sends updates
	public void sendLinkNLRI(MultiDomainTEDB md, Hashtable<String, TEDB> teds) {

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
						log.debug("Looking for temp interdomain link "+ edge.toString());
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
											log.debug(nodex.getHostAddress());
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
							log.info("Adding interdomain link to md ted");
							//Only add if the source and destination domains are different

							//setInterDomainEdgeUpdateTime(localDomainID, LocalNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(), remoteDomainID, RemoteNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(), System.currentTimeMillis());
							edge.setComplete(true);
							md.getNetworkDomainGraph().addEdge((Inet4Address) edge.getDomain_src_router(), edge.getDomain_dst_router(), edge);
							md.getTemps().remove(key);
							log.debug(edge.toString());
						}else{
							log.info("This link is still not complete");
							if (dfound) log.info("dst found");
							else log.info("dst not found"+((Inet4Address) edge.getDst_router_id()).getHostAddress());
							if (sfound) log.info("src found");
							else log.info("src not found");
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
								log.debug("Domain "+ domainID+" found MD-PCE with ip "+((DomainTEDB)ted).getMDPCE().getPCEipv4().getHostName() );
							}
							else
								log.debug("No PCE info for domain "+ domainID );

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

		if (true) {
			int lanID = 1; ///INVENTADOO
			ArrayList<Object> addressList = new ArrayList<>();
			ArrayList<Inet4Address> interfacesList = new ArrayList<Inet4Address>();
			Iterator<InterDomainEdge> edgeIt = interdomainLinks.iterator();
			while (edgeIt.hasNext()) {

				InterDomainEdge edge = edgeIt.next();
				if(edge.getComplete()) {
					Object source = null;
					Object dst = null;
					log.debug("edge before sending it");
					log.debug(edge.toString());
					if (edge.getSrc_router_id() instanceof Inet4Address)
						source = (Inet4Address) edge.getSrc_router_id();
					if (edge.getSrc_router_id() instanceof Long)
						source = (long) edge.getSrc_router_id();
					if (edge.getDst_router_id() instanceof Inet4Address)
						dst = (Inet4Address) edge.getDst_router_id();
					if (edge.getDst_router_id() instanceof Long)
						dst = (long) edge.getDst_router_id();
					if ((source != null) && (dst != null)){
						log.info("Sending interdomain  Edge: (" + (source.toString()).replaceAll("/","") +" " + (dst.toString()).replaceAll("/","") + ")");
						addressList = new ArrayList<Object>();
						addressList.add(0, source);
						addressList.add(1, dst);
					}
					//Link Local Remote Identifiers
					ArrayList<Long> localRemoteIfList = null;
					localRemoteIfList = new ArrayList<Long>();
					localRemoteIfList.add(0, ((InterDomainEdge) edge).getSrc_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier());
					localRemoteIfList.add(1, ((InterDomainEdge) edge).getDst_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier());
					interfacesList.add(0, ((InterDomainEdge) edge).getLocalInterfaceIPv4());
					interfacesList.add(1, ((InterDomainEdge) edge).getNeighborIPv4());


					ArrayList<String> domainList = new ArrayList<String>(2);
					//FIXME: chequear
					TE_Information te_info = ((InterDomainEdge) edge).getTE_info();

					domainList.add(((Inet4Address) edge.getDomain_src_router()).getHostAddress().toString());
					//System.out.println("SRC Domain is "+((Inet4Address)edge.getDomain_src_router()).getHostAddress().toString() );
					domainList.add(((Inet4Address) edge.getDomain_dst_router()).getHostAddress().toString());
					log.debug("Source Domain is " + ((Inet4Address) edge.getDomain_src_router()).getHostAddress());
					log.debug("Dst Domain is " + ((Inet4Address) edge.getDomain_dst_router()).getHostAddress());
					BGP4Update update = createMsgUpdateLinkNLRI2(null, addressList, localRemoteIfList, lanID, domainList, false, te_info, edge.getLearntFrom(),
							interfacesList);
					update.setLearntFrom(edge.getLearntFrom());
					log.debug("Update message created for interdomain link");
					log.debug("learnt from "+ edge.getLearntFrom());
					sendMessage(update);
				}
				else log.info ("Edge not complete/n"+edge.toString());
			}
		}

	}


	/**
	 * This function sends a BGP4 update message (encoded in a LinkNLRI) for each link in the set
	 *
	 * @param edgeIt
	 */
	private void sendLinkNLRI(Set<IntraDomainEdge> edgeSet, String domainID) {
		int lanID = 1; ///INVENTADOO
		ArrayList<Inet4Address> addressList = new ArrayList<Inet4Address>();
		Iterator<IntraDomainEdge> edgeIt = edgeSet.iterator();
		while (edgeIt.hasNext()) {

			IntraDomainEdge edge = edgeIt.next();
			Inet4Address source = null;
			if (edge.getSource() instanceof es.tid.tedb.elements.Node) {
				try {
					source = (Inet4Address) Inet4Address.getByName(((es.tid.tedb.elements.Node) edge.getSource()).getAddress().get(0));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				source = (Inet4Address) edge.getSource();
			}

			Inet4Address dst = null;
			if (edge.getTarget() instanceof es.tid.tedb.elements.Node) {
				try {
					dst = (Inet4Address) Inet4Address.getByName(((es.tid.tedb.elements.Node) edge.getTarget()).getAddress().get(0));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				dst = (Inet4Address) edge.getTarget();
			}
			log.debug("Sending: (" + source.toString() + "," + dst.toString() + ")");
			addressList = new ArrayList<Inet4Address>();
			addressList.add(0, source);
			addressList.add(1, dst);
			//Link Local Remote Identifiers
			ArrayList<Long> localRemoteIfList = null;
			localRemoteIfList = new ArrayList<Long>();
			localRemoteIfList.add(0, ((IntraDomainEdge) edge).getSrc_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier());
			localRemoteIfList.add(1, ((IntraDomainEdge) edge).getDst_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier());

			//MPLS
			float maximumBandwidth = 0;
			float[] unreservedBandwidth = null;
			float maximumReservableBandwidth = 0;
			int undirLinkDelay = 0;
			int metric = 0;
			long te_metric = 0;

			//GMPLS
			AvailableLabels availableLabels = null;
			MF_OTPAttribTLV mfOTP = null;


			TE_Information te_info = ((IntraDomainEdge) edge).getTE_info();

			ArrayList<String> domainList = new ArrayList<String>(2);
			domainList.add(domainID);
			domainList.add(domainID);
			BGP4Update update = createMsgUpdateLinkNLRI(edge, addressList, localRemoteIfList, lanID, domainList, true, te_info, edge.getLearntFrom(), null);
			update.setLearntFrom(edge.getLearntFrom());
			sendMessage(update);

		}

	}

	/**
	 * This function sends a BGP4 update message (encoded in a LinkNLRI) for each link in the set
	 *
	 * @param edgeIt
	 */
	private void sendLinkNLRIISIS(Set<IntraDomainEdge> edgeSet, String domainID) {
		int lanID = 1; ///INVENTADOO
		ArrayList<Integer> addressList = new ArrayList<Integer>();
		Iterator<IntraDomainEdge> edgeIt = edgeSet.iterator();
		while (edgeIt.hasNext()) {

			IntraDomainEdge edge = edgeIt.next();
			long source = 0L;
			/*
						Object v = vertexIt.next();
			if( v instanceof es.tid.tedb.elements.Node){
				node= Integer.valueOf(((es.tid.tedb.elements.Node)v).getAddress().get(0));

			}else{
				node = (Integer)v;
			}

			 */
			if (edge.getSource() instanceof es.tid.tedb.elements.Node) {

				//source = Integer.valueOf(((es.tid.tedb.elements.Node) edge.getSource()).getAddress().get(0));
				source= ((es.tid.tedb.elements.Node) edge.getSource()).getISIS_ID();
			}
			else {
				source = (long) edge.getSource();
			}

			long dst = 0L;
			if (edge.getTarget() instanceof es.tid.tedb.elements.Node) {
				//dst = Integer.valueOf(((es.tid.tedb.elements.Node) edge.getTarget()).getAddress().get(0));
				dst = ((es.tid.tedb.elements.Node) edge.getTarget()).getISIS_ID();


			}
			else {
				dst = (long) edge.getTarget();
			}

			//addressList = new ArrayList<long>();
			//addressList.add(0, source);
			//addressList.add(1, dst);
			//Link Local Remote Identifiers
			ArrayList<Long> localRemoteIfList = null;
			localRemoteIfList = new ArrayList<Long>();
			localRemoteIfList.add(0, ((IntraDomainEdge) edge).getSrc_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier());
			localRemoteIfList.add(1, ((IntraDomainEdge) edge).getDst_if_id());//te_info.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier());

			//MPLS
			float maximumBandwidth = 0;
			float[] unreservedBandwidth = null;
			float maximumReservableBandwidth = 0;
			int undirLinkDelay = 0;
			int metric = 0;
			long te_metric = 0;

			//GMPLS
			AvailableLabels availableLabels = null;
			MF_OTPAttribTLV mfOTP = null;


			TE_Information te_info = ((IntraDomainEdge) edge).getTE_info();
			log.debug("Sending: (" + String.valueOf(source) + "," + String.valueOf(dst) + ") with TE info "+ te_info.toString());
			Inet4Address local = ((IntraDomainEdge) edge).getLocalInterfaceIPv4();
			Inet4Address neighbor = ((IntraDomainEdge) edge).getNeighborIPv4();
			if (local != null && neighbor != null)
				log.debug("IPv4 addresses: source->" + local.toString() + "///// dest->" + neighbor.toString() + ")");
			else {
				if (local == null)
					log.debug("local==null");
				if (neighbor == null)
					log.debug("neighbor==null");
			}
			ArrayList<String> domainList = new ArrayList<String>(2);
			domainList.add(domainID);
			domainList.add(domainID);

			BGP4Update update = createMsgUpdateLinkNLRIISIS(edge, source, dst, lanID, domainList, true, te_info, edge.getLearntFrom(), local, neighbor);
			update.setLearntFrom(edge.getLearntFrom());
			sendMessage(update);

		}

	}

	/**
	 * Function to send a BGP4 update message to the connected peers.
	 *
	 * @param update
	 */
	private void sendMessage(BGP4Update update) {

		Enumeration<GenericBGP4Session> sessions = bgp4SessionsInformation.getSessionList().elements();

		log.debug("Sending a BGP4 update message:" + update.toString());
		while (sessions.hasMoreElements()) {
			GenericBGP4Session session = sessions.nextElement();
			if (session == null) {
				log.error("SESSION NULL");
			} else {
				if (session.getSendTo()) {
					String destination = session.getRemotePeerIP().getHostAddress();
					log.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxBGP4 Update learnt from:" + update.getLearntFrom());
					if (update.getLearntFrom() != null) {

						if (update.getLearntFrom().contains("/"))
							log.debug("BGP4 Update learnt from new:" + update.getLearntFrom().replaceAll("/", ""));
						if (isTest) {
							log.debug("Sending BGP4 update to:" + destination + " with no check on the ID since it is test");
							if (session.getMyAutonomousSystem() != session.getRemoteAutonomousSystem()) {
								log.debug("size before: " + String.valueOf(update.getPathAttributes().size()));
								for (PathAttribute attr : update.getPathAttributes()) {
									if (attr.getTypeCode() == PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_LOCAL_PREF)
										update.getPathAttributes().remove(attr);
									log.debug("size after removing: " + String.valueOf(update.getPathAttributes().size()));
								}
							}
							session.sendBGP4Message(update);
						} else {
							try {
								if ((update.getLearntFrom() != null) && (update.getLearntFrom().contains("/"))) {
									//log.info(update.getLearntFrom().substring(1));
									if (!destination.equals(update.getLearntFrom().substring(1))) {
										//log.info("id da getLearnt "+ update.getLearntFrom());
										log.debug("Sending update to destination " + destination + " for info learnt from " + update.getLearntFrom().substring(1));
										//log.info("Sending BGP4 update to:" + destination);
										session.sendBGP4Message(update);
									} else
										log.debug("destination " + destination + " and source of information " + update.getLearntFrom().substring(1) + " are equal");
								} else {
									if (!destination.equals(update.getLearntFrom())) {
										//log.info("id da getLearnt "+ update.getLearntFrom());
										log.debug("Sending update to destination " + destination + " for info learnt from " + update.getLearntFrom());
										//log.info("Sending BGP4 update to:" + destination);
										session.sendBGP4Message(update);
									} else
										log.debug("destination " + destination + " and source of information " + update.getLearntFrom() + " are equal");
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

						}
					}
					else
						log.info("update.getLearntFrom() = null");
				}
				else
					log.debug("Do not send to this peer");
			}

		}
	}

	/**
	 * This function create a BGP4 Message with NodeNLRI field
	 *
	 * @param node_info
	 */
	private BGP4Update createMsgUpdateNodeNLRI(Node_Info node_info, String learntFrom) {
		try {


			BGP4Update update = new BGP4Update();
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();

			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);
			//AS_PATH


			if (send4AS == true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			} else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}


			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);

			//Node Attribute

			LinkStateAttribute linkStateAttribute = new LinkStateAttribute();
			boolean linkStateNeeded = false;

			if (node_info.getSid() != 0) {
				int sid = node_info.getSid();
				SidLabelNodeAttribTLV sidLabelTLV = new SidLabelNodeAttribTLV();
				sidLabelTLV.setSid(sid);
				linkStateAttribute.setSidLabelTLV(sidLabelTLV);
				linkStateNeeded = true;
			}


			if (node_info.getName() != null) {
				log.debug("Sending node name: "+new String (node_info.getName()));
				NodeNameNodeAttribTLV nna = new NodeNameNodeAttribTLV();
				nna.setNameb(node_info.getName());
				linkStateAttribute.setNodeNameTLV(nna);
				linkStateNeeded=true;
			}

			/*if (node_info.getName() != null) {
				log.info("the name is "+new String(node_info.getName()));
				NodeNameNodeAttribTLV nna = new NodeNameNodeAttribTLV();
				//nna.setName(new String(node_info.getName()));
				log.info("adding node name");
				linkStateAttribute.setNodeNameTLV(nna);
				linkStateNeeded=true;
				log.info("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww "+linkStateAttribute.getNodeNameTLV().toString());
			}*/

			
			if (node_info.getArea_id()!=null){
				byte[] address = null;
				address=new byte[4];
				Inet4Address idarea = null;
				System.arraycopy(node_info.getArea_id(),0, address, 0, 4);
				try {
					idarea= (Inet4Address) Inet4Address.getByAddress(address);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
				ipv4Id.setIpv4Address(idarea);
				linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
				linkStateNeeded=true;
			}
			else
				log.debug("area is null");
			if (node_info.getIpv4Address()!=null){
				Inet4Address ip = node_info.getIpv4Address();
				IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
				ipv4Id.setIpv4Address(ip);
				log.debug("adding router id");
				linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
				linkStateNeeded=true;
			}


			if (linkStateNeeded) {
				log.debug("Node Attribute added....");
				pathAttributes.add(linkStateAttribute);
			}

			//NLRI
			NodeNLRI nodeNLRI = new NodeNLRI();
			nodeNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
			nodeNLRI.setRoutingUniverseIdentifier(identifier);
			LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();

			//igp router id
			if (node_info.getIpv4Address() != null) {
				IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
				igpRouterIDLNSubTLV.setIpv4AddressOSPF(node_info.getIpv4Address());
				igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
				localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);

			}

			//as number
			if (node_info.getAs_number() != null) {
				AutonomousSystemNodeDescriptorSubTLV asNodeDescrSubTLV = new AutonomousSystemNodeDescriptorSubTLV();
				asNodeDescrSubTLV.setAS_ID(node_info.getAs_number());
				localNodeDescriptors.setAutonomousSystemSubTLV(asNodeDescrSubTLV);
			}
			//Complete Dummy TLVs
			//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV = new BGPLSIdentifierNodeDescriptorSubTLV();
			//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
			//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
			AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
			areaID.setAREA_ID(this.localAreaID);
			//commented for compliance with ODL
			// localNodeDescriptors.setAreaID(areaID);

			nodeNLRI.setLocalNodeDescriptors(localNodeDescriptors);
			BGP_LS_MP_Reach_Attribute ra = new BGP_LS_MP_Reach_Attribute();
			ra.setLsNLRI(nodeNLRI);
			if ((learntFrom!="local")&&(learntFrom!=null)){
				try {
					ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}//aaaa
			}
			pathAttributes.add(ra);
			update.setLearntFrom(node_info.getLearntFrom());
			log.debug("Node update: "+update.toString());
			return update;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}


	private  BGP4Update createMsgUpdateNodeNLRIISIS(Node_Info node_info, String learntFrom){
		try{


			BGP4Update update= new BGP4Update();
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();

			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);
			//AS_PATH

			if (send4AS==true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}
			else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}


			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);

			//Node Attribute

			LinkStateAttribute  linkStateAttribute = new LinkStateAttribute();
			boolean linkStateNeeded=false;

			if (node_info.getSid()!=0){
				int sid = node_info.getSid();
				SidLabelNodeAttribTLV sidLabelTLV = new SidLabelNodeAttribTLV();
				sidLabelTLV.setSid(sid);
				linkStateAttribute.setSidLabelTLV(sidLabelTLV);
				linkStateNeeded=true;
			}
			if (node_info.getName() != null) {
				log.debug("Sending node name: "+new String (node_info.getName()));
				NodeNameNodeAttribTLV nna = new NodeNameNodeAttribTLV();
				nna.setNameb(node_info.getName());
				linkStateAttribute.setNodeNameTLV(nna);
				linkStateNeeded=true;
			}
			//da sistemare
			if (node_info.getIpv4areaIDs()!=null){
				int vlen=node_info.getValid_len();
				if (vlen==3){
					byte[] address = null;
					address=new byte[4];
					Inet4Address idarea = null;
					System.arraycopy(node_info.getIpv4areaIDs().get(0).getAddress(),1, address, 1, 3);
					try {
						idarea= (Inet4Address) Inet4Address.getByAddress(address);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					log.debug("Sending ISIS Area ID: "+idarea.getHostAddress());
					IS_IS_AreaIdentifierNodeAttribTLV isisarea = new IS_IS_AreaIdentifierNodeAttribTLV();
					isisarea.setValid_len(3);
					isisarea.setIpv4areaIDs(node_info.getIpv4areaIDs());
					linkStateAttribute.setAreaIDTLV(isisarea);


				}
				else if (vlen==4){
					byte[] address = null;
					address=new byte[4];
					Inet4Address idarea = null;
					for (int i=0;i<node_info.getIpv4areaIDs().size();++i) {
						idarea=node_info.getIpv4areaIDs().get(i);
						log.debug("Sending ISIS Area ID: "+idarea.getHostAddress());
						IS_IS_AreaIdentifierNodeAttribTLV isisarea = new IS_IS_AreaIdentifierNodeAttribTLV();
						isisarea.setValid_len(4);
						isisarea.setIpv4areaIDs(node_info.getIpv4areaIDs());
						linkStateAttribute.setAreaIDTLV(isisarea);
					}
				}

				linkStateNeeded=true;
			}
			if (node_info.getIpv4AddressLocalNode()!=null){

				Inet4Address ip = node_info.getIpv4AddressLocalNode();
                IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
				ipv4Id.setIpv4Address(ip);
				log.debug("Sending ipv4address: "+ip.getHostAddress());
				linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
				linkStateNeeded=true;
			}



			//nna.setName();

			if (linkStateNeeded){
				log.debug("Node Attribute needed and Added");
				pathAttributes.add(linkStateAttribute);
			}

			//NLRI
			NodeNLRI nodeNLRI = new NodeNLRI();
			nodeNLRI.setProtocolID(ProtocolIDCodes.IS_IS_Level2_Protocol_ID);
			nodeNLRI.setRoutingUniverseIdentifier(identifier);
			LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();

			//igp router id
			if(node_info.getISISid()!=0){
				log.debug("ISIS node id set in TED!!!!   ->   "+String.valueOf(node_info.getISISid()));
				IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
				igpRouterIDLNSubTLV.setISIS_ISO_NODE_ID(node_info.getISISid());
				igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO);
				localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);

			}

			//as number
			if(node_info.getAs_number()!=null){
				AutonomousSystemNodeDescriptorSubTLV asNodeDescrSubTLV = new AutonomousSystemNodeDescriptorSubTLV();
				asNodeDescrSubTLV.setAS_ID(node_info.getAs_number());
				localNodeDescriptors.setAutonomousSystemSubTLV(asNodeDescrSubTLV);
			}
			//Complete Dummy TLVs
			//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV =new BGPLSIdentifierNodeDescriptorSubTLV();
			//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
			//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
			AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
			areaID.setAREA_ID(this.localAreaID);
			//commented for compliance with ODL
			// localNodeDescriptors.setAreaID(areaID);

			nodeNLRI.setLocalNodeDescriptors(localNodeDescriptors);
			BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
			ra.setLsNLRI(nodeNLRI);
			if (learntFrom!="local"){
				try {
					ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}pathAttributes.add(ra);
			update.setLearntFrom(node_info.getLearntFrom());
			return update;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * This function create a BGP4 Message with NodeNLRI field
	 * @param addressList 
	 * @param node_info
	 */
	private  BGP4Update createMsgUpdateITNodeNLRI(String domainID, IT_Resources itResources, String learntFrom){
		try{
	
			BGP4Update update= new BGP4Update();	
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);

			//AS_PATH

			if (send4AS==true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}
			else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}


			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);

			//NLRI
			ITNodeNLRI itNodeNLRI = new ITNodeNLRI();
			itNodeNLRI.setNodeId(domainID);
			itNodeNLRI.setControllerIT(itResources.getControllerIT());
			itNodeNLRI.setCpu(itResources.getCpu());
			itNodeNLRI.setMem(itResources.getMem());
			itNodeNLRI.setStorage(itResources.getStorage());
			update.setLearntFrom(itResources.getLearntFrom());
			log.debug("Creating IT Update Related to Domain "+domainID+" Learnt from "+itResources.getLearntFrom());
			LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();
			
			//Complete Dummy TLVs
			//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV =new BGPLSIdentifierNodeDescriptorSubTLV();
			//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
			//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
			AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
			areaID.setAREA_ID(this.localAreaID);
			localNodeDescriptors.setAreaID(areaID);
	
			//itNodeNLRI.setLocalNodeDescriptors(localNodeDescriptors);
			BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
			ra.setLsNLRI(itNodeNLRI);
			if (learntFrom!="local"){
				try {
					ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			pathAttributes.add(ra);
			return update;
		
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		
	}

	/*
	private BGP4Update createMsgUpdateNodeNLRI(Node_Info node_info, String learntFrom) {
		try {


			BGP4Update update = new BGP4Update();
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();

			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);
			//AS_PATH


			if (send4AS == true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			} else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}


			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);

			//Node Attribute

			LinkStateAttribute linkStateAttribute = new LinkStateAttribute();
			boolean linkStateNeeded = false;

			if (node_info.getSid() != 0) {
				int sid = node_info.getSid();
				SidLabelNodeAttribTLV sidLabelTLV = new SidLabelNodeAttribTLV();
				sidLabelTLV.setSid(sid);
				linkStateAttribute.setSidLabelTLV(sidLabelTLV);
				linkStateNeeded = true;
			}


			if (node_info.getName() != null) {
				log.debug("Sending node name: "+new String (node_info.getName()));
				NodeNameNodeAttribTLV nna = new NodeNameNodeAttribTLV();
				nna.setNameb(node_info.getName());
				linkStateAttribute.setNodeNameTLV(nna);
				linkStateNeeded=true;
			}




	if (node_info.getArea_id()!=null){
		byte[] address = null;
		address=new byte[4];
		Inet4Address idarea = null;
		System.arraycopy(node_info.getArea_id(),0, address, 0, 4);
		try {
			idarea= (Inet4Address) Inet4Address.getByAddress(address);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
		ipv4Id.setIpv4Address(idarea);
		linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
		linkStateNeeded=true;
	}
	else
			log.debug("area is null");
	if (node_info.getIpv4Address()!=null){
		Inet4Address ip = node_info.getIpv4Address();
		IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
		ipv4Id.setIpv4Address(ip);
		log.debug("adding router id");
		linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
		linkStateNeeded=true;
	}


	if (linkStateNeeded) {
		log.debug("Node Attribute added....");
		pathAttributes.add(linkStateAttribute);
	}

	//NLRI
	NodeNLRI nodeNLRI = new NodeNLRI();
	nodeNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
	nodeNLRI.setRoutingUniverseIdentifier(identifier);
	LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();

	//igp router id
	if (node_info.getIpv4Address() != null) {
		IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		igpRouterIDLNSubTLV.setIpv4AddressOSPF(node_info.getIpv4Address());
		igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
		localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);

	}

	//as number
	if (node_info.getAs_number() != null) {
		AutonomousSystemNodeDescriptorSubTLV asNodeDescrSubTLV = new AutonomousSystemNodeDescriptorSubTLV();
		asNodeDescrSubTLV.setAS_ID(node_info.getAs_number());
		localNodeDescriptors.setAutonomousSystemSubTLV(asNodeDescrSubTLV);
	}
	//Complete Dummy TLVs
	//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV = new BGPLSIdentifierNodeDescriptorSubTLV();
	//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
	//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
	AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
	areaID.setAREA_ID(this.localAreaID);
	//commented for compliance with ODL
	// localNodeDescriptors.setAreaID(areaID);

	nodeNLRI.setLocalNodeDescriptors(localNodeDescriptors);
	BGP_LS_MP_Reach_Attribute ra = new BGP_LS_MP_Reach_Attribute();
	ra.setLsNLRI(nodeNLRI);
	if ((learntFrom!="local")&&(learntFrom!=null)){
		try {
			ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}//aaaa
	}
	pathAttributes.add(ra);
	update.setLearntFrom(node_info.getLearntFrom());
	log.debug("Node update: "+update.toString());
	return update;
} catch (Exception e) {
		e.printStackTrace();
		return null;
		}

		}

	*/


	private  BGP4Update createMsgUpdateMDPCENLRI(String domainID, PCEInfo IP, String learntFrom){
		try{

			BGP4Update update = new BGP4Update();
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();

			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);
			//AS_PATH


			if (send4AS == true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			} else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}


			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);

            //Node Attribute

            LinkStateAttribute linkStateAttribute = new LinkStateAttribute();
            boolean linkStateNeeded = false;


			/*if (node_info.getName() != null) {
				log.info("the name is "+new String(node_info.getName()));
				NodeNameNodeAttribTLV nna = new NodeNameNodeAttribTLV();
				//nna.setName(new String(node_info.getName()));
				log.info("adding node name");
				linkStateAttribute.setNodeNameTLV(nna);
				linkStateNeeded=true;
				log.info("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww "+linkStateAttribute.getNodeNameTLV().toString());
			}


            if (node_info.getArea_id()!=null){
                byte[] address = null;
                address=new byte[4];
                Inet4Address idarea = null;
                System.arraycopy(node_info.getArea_id(),0, address, 0, 4);
                try {
                    idarea= (Inet4Address) Inet4Address.getByAddress(address);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
                ipv4Id.setIpv4Address(idarea);
                linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
                linkStateNeeded=true;
            }
            else
                log.debug("area is null");
            */
            if (IP.getPCEipv4()!=null){
                Inet4Address ip = IP.getPCEipv4();
                IPv4RouterIDLocalNodeNodeAttribTLV ipv4Id = new IPv4RouterIDLocalNodeNodeAttribTLV();
                ipv4Id.setIpv4Address(ip);
                log.debug("adding PCE router id");
                linkStateAttribute.setIPv4RouterIDLocalNodeNATLV(ipv4Id);
                linkStateNeeded=true;
            }


            if (IP.getNeighbours() != null) {
                OpaqueNodeNodeAttribTLV ona = new OpaqueNodeNodeAttribTLV();
				PCEAttribSubTLV pce= new PCEAttribSubTLV();
				pce.setNeighbours(IP.getNeighbours());
                ona.setPCEAttribSubTLV(pce);
                linkStateAttribute.setOpaqueNodeTLV(ona);
				log.debug("Sending opaque node: "+ona.toString());
                linkStateNeeded=true;
            }

            if (linkStateNeeded) {
                log.debug("Node Attribute added....");
                pathAttributes.add(linkStateAttribute);
            }

            //NLRI
            NodeNLRI nodeNLRI = new NodeNLRI();
            nodeNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
            nodeNLRI.setRoutingUniverseIdentifier(identifier);
            LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();

            //igp router id
            if (IP.getPCEipv4() != null) {
                IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
                igpRouterIDLNSubTLV.setIpv4AddressOSPF(IP.getPCEipv4());
                igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
                localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);

            }

            //as number
            if (IP.getdomainID() != null) {
                AutonomousSystemNodeDescriptorSubTLV asNodeDescrSubTLV = new AutonomousSystemNodeDescriptorSubTLV();
                asNodeDescrSubTLV.setAS_ID((Inet4Address) Inet4Address.getByName(IP.getdomainID()));
                localNodeDescriptors.setAutonomousSystemSubTLV(asNodeDescrSubTLV);
            }
            //Complete Dummy TLVs
            //BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV = new BGPLSIdentifierNodeDescriptorSubTLV();
            //bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
            //localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
            AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
            areaID.setAREA_ID(this.localAreaID);
            //commented for compliance with ODL
            // localNodeDescriptors.setAreaID(areaID);

            nodeNLRI.setLocalNodeDescriptors(localNodeDescriptors);
            BGP_LS_MP_Reach_Attribute ra = new BGP_LS_MP_Reach_Attribute();
            ra.setLsNLRI(nodeNLRI);
            if ((learntFrom!="local")&&(learntFrom!=null)){
                try {
                    ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }//aaaa
            }
            pathAttributes.add(ra);
            update.setLearntFrom(IP.getLearntFrom());
            log.debug("Node PCE update: "+update.toString());
            return update;


		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/*
	private  BGP4Update createMsgUpdateMDPCENLRI(String domainID, PCEInfo IP, String learntFrom){
		try{

			String domainIDx= null;
			BGP4Update update= new BGP4Update();
			update.setLearntFrom(IP.getLearntFrom());
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);
			//AS_PATH

			if (send4AS==true) {

				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}
			else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = ASnumber;
				as_path_seg.setSegments(segs);
				//as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}

			//LOCAL PREF Attribute
			LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
			as_local_pref.setValue(LocalPref);
			pathAttributes.add(as_local_pref);


			//NLRI
			PCENLRI pceNLRI = new PCENLRI();
			pceNLRI.setProtocolID(ProtocolIDCodes.Static_Protocol_ID);
			pceNLRI.setRoutingUniverseIdentifier(identifier);
						  //PCE descriptor
			PCEv4DescriptorsTLV pcev4 = new PCEv4DescriptorsTLV();
			pcev4.setPCEv4Address(IP.getPCEipv4());
			pceNLRI.setPCEv4Descriptors(pcev4);

					//PCE Scope SubTLV
			PCEv4ScopeTLV pcev4scope= new PCEv4ScopeTLV();
			pcev4scope.setPre_L(5);
			pcev4scope.setPre_R(3);
			pcev4scope.setPre_S(4);
			pcev4scope.setPre_Y(1);
			pceNLRI.setPCEv4ScopeTLV(pcev4scope);
			log.info("Creating PCE Update related to Domain "+domainID);

			//Domain TLV
			PCEv4DomainTLV domTLV= new PCEv4DomainTLV();
			AreaIDNodeDescriptorSubTLV domID =new AreaIDNodeDescriptorSubTLV();
			domainIDx = domainID.replace("/", "");
			log.debug(domainIDx);
			//domID.setAREA_ID((Inet4Address) forString(domainIDx));
			//domID.setAREA_ID((Inet4Address) Inet4Address.getByName(domainID)));
			domID.setAREA_ID((Inet4Address) Inet4Address.getByName(domainIDx));
			//domTLV.addAreaIDSubTLV(domID);

			ArrayList<AreaIDNodeDescriptorSubTLV> list = new ArrayList<AreaIDNodeDescriptorSubTLV>();
			list.add(domID);
			domTLV.setAreaIDSubTLVs(list);
			//domTLV.getAreaIDSubTLVs().add(domID);
			pceNLRI.setPCEv4DomainID(domTLV);

								//add NLRI to BGP-LS
			BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
			ra.setLsNLRI(pceNLRI);
			if ((learntFrom!="local")&&(learntFrom!=null)){
				try {
					ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
			pathAttributes.add(ra);
			//log.info(ra.toString());
			return update;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	*/


	private static IllegalArgumentException formatIllegalArgumentException(String format, Object... args) {
		return new IllegalArgumentException(String.format(Locale.ROOT, format, args));
	}

	public static InetAddress forString(String ipString) {
		   byte[] addr = ipStringToBytes(ipString);
		   // The argument was malformed, i.e. not an IP string literal.
		   if (addr == null) {
			   throw formatIllegalArgumentException("'%s' is not an IP string literal.", ipString);
		   }
		   return bytesToInetAddress(addr);
	}
	private static InetAddress bytesToInetAddress(byte[] addr) {
		try {
		     return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			throw new AssertionError(e);
		}
	}

	//@Nullable
	private static byte[] ipStringToBytes(String ipString) {
		// Make a first pass to categorize the characters in this string.
		boolean hasColon = false;
		boolean hasDot = false;
		for (int i = 0; i < ipString.length(); i++) {
			char c = ipString.charAt(i);
			if (c == '.') {
				hasDot = true;
			} else if (c == ':') {
				if (hasDot) {
					return null; // Colons must not appear after dots.
				}
				hasColon = true;
			} else if (Character.digit(c, 16) == -1) {
				return null; // Everything else must be a decimal or hex digit.
			}
		}
		// Now decide which address family to parse.
		if (hasColon) {
			if (hasDot) {
				ipString = convertDottedQuadToHex(ipString);
				if (ipString == null) {
					return null;
				}
			}
			return textToNumericFormatV6(ipString);
		} else if (hasDot) {
			return textToNumericFormatV4(ipString);
		}
		return null;
	}

	private static String convertDottedQuadToHex(String ipString) {
		int lastColon = ipString.lastIndexOf(':');
		String initialPart = ipString.substring(0, lastColon + 1);
		String dottedQuad = ipString.substring(lastColon + 1);
		byte[] quad = textToNumericFormatV4(dottedQuad);
		if (quad == null) {
			return null;
		}
		String penultimate = Integer.toHexString(((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
		String ultimate = Integer.toHexString(((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
		return initialPart + penultimate + ":" + ultimate;
	}

	private static byte[] textToNumericFormatV4(String ipString) {
		byte[] bytes = new byte[IPV4_PART_COUNT];
		int i = 0;
		try {
			for (String octet : IPV4_SPLITTER.split(ipString)) {
				bytes[i++] = parseOctet(octet);
			}
		} catch (NumberFormatException ex) {
			return null;
		}

		return i == IPV4_PART_COUNT ? bytes : null;
	}

	private static byte parseOctet(String ipPart) {
	   // Note: we already verified that this string contains only hex digits.
	    int octet = Integer.parseInt(ipPart);
	    // Disallow leading zeroes, because no clear standard exists on
	    // whether these should be interpreted as decimal or octal.
	    if (octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
	        throw new NumberFormatException();
	    }
	    return (byte) octet;
	}

    private static byte[] textToNumericFormatV6(String ipString) {
        // An address can have [2..8] colons, and N colons make N+1 parts.
		String[] parts = ipString.split(":", IPV6_PART_COUNT + 2);
		if (parts.length < 3 || parts.length > IPV6_PART_COUNT + 1) {
			return null;
		}

		int skipIndex = -1;
		for (int i = 1; i < parts.length - 1; i++) {
			if (parts[i].length() == 0) {
			    if (skipIndex >= 0) {
			        return null; // Can't have more than one ::
			    }
			    skipIndex = i;
			}
		}
		int partsHi; // Number of parts to copy from above/before the "::"
		int partsLo; // Number of parts to copy from below/after the "::"
		if (skipIndex >= 0) {
			// If we found a "::", then check if it also covers the endpoints.
			partsHi = skipIndex;
			partsLo = parts.length - skipIndex - 1;
			if (parts[0].length() == 0 && --partsHi != 0) {
				return null; // ^: requires ^::
			}
			if (parts[parts.length - 1].length() == 0 && --partsLo != 0) {
				return null; // :$ requires ::$
			}
		} else {
			partsHi = parts.length;
			partsLo = 0;
		}
		int partsSkipped = IPV6_PART_COUNT - (partsHi + partsLo);
		if (!(skipIndex >= 0 ? partsSkipped >= 1 : partsSkipped == 0)) {
			  return null;
		}
		// Now parse the hextets into a byte array.
		ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
		try {
			for (int i = 0; i < partsHi; i++) {
				rawBytes.putShort(parseHextet(parts[i]));
			}
			for (int i = 0; i < partsSkipped; i++) {
				rawBytes.putShort((short) 0);
			}
			for (int i = partsLo; i > 0; i--) {
				rawBytes.putShort(parseHextet(parts[parts.length - i]));
			}
		} catch (NumberFormatException ex) {
			return null;
		}
		return rawBytes.array();
	}


	private static short parseHextet(String ipPart) {
		// Note: we already verified that this string contains only hex digits.
		int hextet = Integer.parseInt(ipPart, 16);
		if (hextet > 0xffff) {
			throw new NumberFormatException();
		}
		return (short) hextet;
	}




	/*private  BGP4Update createMsgUpdateMDPCENLRI(String domainID, Inet4Address IP){
		try{

			BGP4Update update= new BGP4Update();
			//Path Attributes
			ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
			//Origin
			OriginAttribute or = new OriginAttribute();
			if (learntFrom==BGPID)
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
			else
				or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
			pathAttributes.add(or);

			//AS_PATH
			if (send4AS==true) {
				AS4_Path_Attribute as_path = new AS4_Path_Attribute();
				AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
				long[] segs = new long[1];
				segs[0] = 65522;
				as_path_seg.setSegments(segs);
				as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}
			else {
				AS_Path_Attribute as_path = new AS_Path_Attribute();
				AS_Path_Segment as_path_seg = new AS_Path_Segment();
				int[] segs = new int[1];
				segs[0] = 65522;
				as_path_seg.setSegments(segs);
				as_path.getAsPathSegments().add(as_path_seg);
				pathAttributes.add(as_path);
			}

			//NLRI
			PCENLRI pceNLRI = new PCENLRI();
			PCEv4DescriptorsTLV pcev4 = new PCEv4DescriptorsTLV();
			pcev4.setPCEv4Address(IP);
			//update.setLearntFrom(itResources.getLearntFrom());
			log.info("Creating PCE Update related to domain "+domainID);
			AreaIDNodeDescriptorSubTLV domID =new AreaIDNodeDescriptorSubTLV();
			domID.setAREA_ID((Inet4Address) InetAddress.getByName(domainID));
			//pcev4.setArea_ID(domID);
			pceNLRI.setPCEv4Descriptors(pcev4);
			return update;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
*/

	/**
	 * Function to create a BGP4 update message with a link NRLI field. To send the links.
	 *  @param addressInterfaceList
	 * @param maximumBandwidth
	 * @param unreservedBandwidth
	 * @param maximumReservableBandwidth
	 * @param availableLabels
	 * @param metric
	 * @param linkDelay
	 * @param addressList
	 * @param localRemoteIfList
	 * @param lanID
	 * @param domainList
	 * @param intradomain
	 * @param learntFrom
	 */
	private BGP4Update createMsgUpdateLinkNLRI(IntraDomainEdge edgex, ArrayList<Inet4Address> addressList, ArrayList<Long> localRemoteIfList, int lanID, ArrayList<String> domainList, boolean intradomain, TE_Information te_info, String learntFrom, ArrayList<Inet4Address> interfacesList){
		BGP4Update update= new BGP4Update();	
		//1. Path Attributes
		ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
		//1.1. Origin
		OriginAttribute or = new OriginAttribute(); 
		if (intradomain)
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
		else
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
		pathAttributes.add(or);	
		///Andrea
		//update.setLearntFrom("192.168.0.1");
		//1.2. AS-PATH

/*
if(multiDomainTEDB.getAsInfo_DB().containsKey(learntFrom))
{
	log.info("AsInfo Key: " + learntFrom);
	for(AsInfo As : multiDomainTEDB.getAsInfo_DB().get(learntFrom))
		log.info("SegmentType: " + As.getType() + "SegmentNumber" + As.getsegmentNumbers() + "SegmentValue " + As.getsegmentValue());
}
*/

		if (send4AS==true) {

			AS4_Path_Attribute as_path = new AS4_Path_Attribute();
			AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
			long[] segs = new long[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));
		}
		else {
			AS_Path_Attribute as_path = new AS_Path_Attribute();
			AS_Path_Segment as_path_seg = new AS_Path_Segment();
			int[] segs = new int[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));

		}
		
		//LOCAL PREF Attribute
		LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
		as_local_pref.setValue(LocalPref);
		pathAttributes.add(as_local_pref);
		//1.2. LINK-STATE
		//MPLS
		float maximumBandwidth = 0; 
		float[] unreservedBandwidth = null;
		float maximumReservableBandwidth = 0; 	

		//GMPLS
		AvailableLabels availableLabels = null;
		MF_OTPAttribTLV mfOTP = null;

		int defmetric = 0;
		int te_metric = 0;
		int administrativeGroup=0;
		boolean adminGrouppresent= false;
		
		if (te_info != null){

			//MPLS
			if (te_info.getMaximumBandwidth() != null) {
				maximumBandwidth = te_info.getMaximumBandwidth().getMaximumBandwidth();
			}
			if (te_info.getUnreservedBandwidth() != null)
				unreservedBandwidth = te_info.getUnreservedBandwidth().getUnreservedBandwidth();
			if (te_info.getMaximumReservableBandwidth() != null)
				maximumReservableBandwidth = te_info.getMaximumReservableBandwidth().getMaximumReservableBandwidth();
			//GMPLS
			if (te_info.getAvailableLabels() != null)
				availableLabels = te_info.getAvailableLabels();
			if(te_info.getDefaultTEMetric()!=null){
				defmetric = te_info.getDefaultTEMetric().getLinkMetric();
				log.debug("Default Metric: " + defmetric);
			}
			if(te_info.getMetric()!=null){
				te_metric = te_info.getMetric().getMetric() ;
				log.debug("Metric en el metodo sendLinkNLRI es: " + te_metric);
			}
			if(te_info.getAdministrativeGroup()!=null){
				adminGrouppresent= true;
				administrativeGroup = te_info.getAdministrativeGroup().getAdministrativeGroup() ;
				log.debug("Administrative group : " + administrativeGroup);
			}
			if(te_info.getMfOTF()!=null){
				mfOTP =  te_info.getMfOTF();
			}

		}else{
			log.info("TE_Info is Null");
		}
		
		
		boolean linkStateNeeded = false;
		LinkStateAttribute  linkStateAttribute = new LinkStateAttribute();
		//1.2.1. MaxReservableBandwidth
		if (maximumReservableBandwidth != 0){
			MaxReservableBandwidthLinkAttribTLV maxReservableBandwidthTLV = new MaxReservableBandwidthLinkAttribTLV();
			maxReservableBandwidthTLV.setMaximumReservableBandwidth(maximumReservableBandwidth);
			linkStateAttribute.setMaxReservableBandwidthTLV(maxReservableBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.2. maxBandwidth
		if (maximumBandwidth != 0){
			MaximumLinkBandwidthLinkAttribTLV maximumLinkBandwidthTLV = new MaximumLinkBandwidthLinkAttribTLV();
			maximumLinkBandwidthTLV.setMaximumBandwidth(maximumBandwidth);
			linkStateAttribute.setMaximumLinkBandwidthTLV(maximumLinkBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.3. unreservedBandwidth
		if (unreservedBandwidth != null){
			UnreservedBandwidthLinkAttribTLV unreservedBandwidthTLV = new UnreservedBandwidthLinkAttribTLV();
			unreservedBandwidthTLV.setUnreservedBandwidth(unreservedBandwidth);
			linkStateAttribute.setUnreservedBandwidthTLV(unreservedBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.4. AvailableLabels
		if (availableLabels != null){
			log.debug("Available labels fields: "+availableLabels.getLabelSet().getNumLabels());
			AvailableLabels al = new AvailableLabels();

			BitmapLabelSet bl = new BitmapLabelSet();
			bl.setBytesBitmap(((BitmapLabelSet)availableLabels.getLabelSet()).getBytesBitMap());
			bl.setNumLabels(availableLabels.getLabelSet().getNumLabels());
			bl.setDwdmWavelengthLabel(((BitmapLabelSet)availableLabels.getLabelSet()).getDwdmWavelengthLabel());

			bl.setBytesBitmapReserved(((BitmapLabelSet)availableLabels.getLabelSet()).getBytesBitmapReserved());

			al.setLabelSet(bl);

			log.debug("Campo BytesBitmap: "+Integer.toHexString(((int)bl.getBytesBitMap()[0])&0xFF));
			log.debug("Campo DwdmWavelengthLabel: "+bl.getDwdmWavelengthLabel());
			if (bl.getBytesBitmapReserved()!=null){
				log.debug("Campo BytesBitmapReserved: "+bl.getBytesBitmapReserved()[0]);
			}
			linkStateAttribute.setAvailableLabels(al);

			linkStateNeeded=true;
		}

		//1.2.5 metric
		if (te_metric != 0){
			MetricLinkAttribTLV metricx = new MetricLinkAttribTLV();
			metricx.setMetric(te_metric);
			metricx.setMetric_type(1);
			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + te_metric);
			linkStateAttribute.setMetricTLV(metricx);
			linkStateNeeded=true;
		}
		
		if (defmetric != 0){
			DefaultTEMetricLinkAttribTLV defaultMetric = new DefaultTEMetricLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			defaultMetric.setLinkMetric(defmetric);

			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + defmetric);
			linkStateAttribute.setTEMetricTLV(defaultMetric);
			linkStateNeeded=true;
		}
		if (adminGrouppresent){
			AdministrativeGroupLinkAttribTLV admGroup = new AdministrativeGroupLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			admGroup.setAdministrativeGroup(administrativeGroup);

			log.debug("Administrative Group set: " + administrativeGroup);
			linkStateAttribute.setAdministrativeGroupTLV(admGroup);
			linkStateNeeded=true;
		}

		//1.2.6 MF_OPT
		if (mfOTP != null){
			MF_OTPAttribTLV mfOTPTLV = mfOTP.duplicate();
			log.debug("SENDING MFOTP OSCAR");
			linkStateAttribute.setMF_OTPAttribTLV(mfOTPTLV);
			linkStateNeeded=true;
		}


		//new TE metrics
		//2.2.3 LinkDelay
		if (te_info != null){
			if(te_info.getUndirLinkDelay() != null){
				int undirLinkDelay = te_info.getUndirLinkDelay().getDelay();
				UndirectionalLinkDelayDescriptorSubTLV uSTLV =new UndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setDelay(undirLinkDelay);
				linkStateAttribute.setUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirDelayVar() != null){
				int undirDelayVar = te_info.getUndirDelayVar().getDelayVar();
				UndirectionalDelayVariationDescriptorSubTLV uSTLV =new UndirectionalDelayVariationDescriptorSubTLV();
				uSTLV.setDelayVar(undirDelayVar);
				linkStateAttribute.setUndirectionalDelayVariationTLV(uSTLV);
			}
			if(te_info.getMinMaxUndirLinkDelay() != null){
				int minDelay = te_info.getMinMaxUndirLinkDelay().getLowDelay();
				int maxDelay = te_info.getMinMaxUndirLinkDelay().getHighDelay();
				MinMaxUndirectionalLinkDelayDescriptorSubTLV uSTLV =new MinMaxUndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setHighDelay(maxDelay);
				uSTLV.setLowDelay(minDelay);
				linkStateAttribute.setMinMaxUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirLinkLoss() != null){
				int linkLoss = te_info.getUndirLinkLoss().getLinkLoss();
				UndirectionalLinkLossDescriptorSubTLV uSTLV =new UndirectionalLinkLossDescriptorSubTLV();
				uSTLV.setLinkLoss(linkLoss);
				linkStateAttribute.setUndirectionalLinkLossTLV(uSTLV);
			}
			if(te_info.getUndirResidualBw() != null){
				float resBw = te_info.getUndirResidualBw().getResidualBw();
				UndirectionalResidualBandwidthDescriptorSubTLV uSTLV =new UndirectionalResidualBandwidthDescriptorSubTLV();
				uSTLV.setResidualBw(resBw);
				linkStateAttribute.setUndirectionalResidualBwTLV(uSTLV);
			}
			if(te_info.getUndirAvailableBw() != null){
				float availableBw = te_info.getUndirAvailableBw().getAvailableBw();
				UndirectionalAvailableBandwidthDescriptorSubTLV uSTLV =new UndirectionalAvailableBandwidthDescriptorSubTLV();
				uSTLV.setAvailableBw(availableBw);
				linkStateAttribute.setUndirectionalAvailableBwTLV(uSTLV);
			}
			if(te_info.getUndirUtilizedBw() != null){
				float utilizedBw = te_info.getUndirUtilizedBw().getUtilizedBw();
				UndirectionalUtilizedBandwidthDescriptorSubTLV uSTLV =new UndirectionalUtilizedBandwidthDescriptorSubTLV();
				uSTLV.setUtilizedBw(utilizedBw);
				linkStateAttribute.setUndirectionalUtilizedBwTLV(uSTLV);
			}

		}




		if (linkStateNeeded){
			//log.debug("Link state needed");
			pathAttributes.add(linkStateAttribute);
		}
		//2. NLRI
		LinkNLRI linkNLRI = new LinkNLRI();
		linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
		/*
		if (intradomain){
			linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
		}
		else{
			//linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
			linkNLRI.setProtocolID(ProtocolIDCodes.Direct_Protocol_ID);
		}*/

		linkNLRI.setIdentifier(layer);
		//2.1. Local Y Remote Descriptors
		LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();
		RemoteNodeDescriptorsTLV remoteNodeDescriptors = new RemoteNodeDescriptorsTLV();

		//2.1.1. IPv4
		IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		igpRouterIDLNSubTLV.setIpv4AddressOSPF(addressList.get(0));	
		igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
		localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);
		//Complete Dummy TLVs
		//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV =new BGPLSIdentifierNodeDescriptorSubTLV();
		//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
		//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
		areaID.setAREA_ID(this.localAreaID);
		//commented for compliance with ODL
		// localNodeDescriptors.setAreaID(areaID);

		IGPRouterIDNodeDescriptorSubTLV igpRouterIDDNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		igpRouterIDDNSubTLV.setIpv4AddressOSPF(addressList.get(1));	
		igpRouterIDDNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
		remoteNodeDescriptors.setIGPRouterID(igpRouterIDDNSubTLV);
		//2.1.2. AS
		if (domainList != null){
			AutonomousSystemNodeDescriptorSubTLV as_local = new AutonomousSystemNodeDescriptorSubTLV();
			try {
				as_local.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(0)));
				localNodeDescriptors.setAutonomousSystemSubTLV(as_local);
				AutonomousSystemNodeDescriptorSubTLV as_remote = new AutonomousSystemNodeDescriptorSubTLV();
				as_remote.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(1)));
				remoteNodeDescriptors.setAutonomousSystemSubTLV(as_remote);	
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		//Complete Dummy TLVs
		//remoteNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		//commented for compliance with ODL
		// remoteNodeDescriptors.setAreaID(areaID);

		linkNLRI.setLocalNodeDescriptors(localNodeDescriptors);
		linkNLRI.setRemoteNodeDescriptorsTLV(remoteNodeDescriptors);

		//2.2. Link NLRI TLVs 
		//2.2.1. Ipv4 interface and neighbour address
		//IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
		//IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
		//ipv4InterfaceAddressTLV.setIpv4Address(addressList.get(0));
		//ipv4NeighborAddressTLV.setIpv4Address(addressList.get(1));
		//linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
		//linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
		if ((edgex!=null)&&(edgex.getLocalInterfaceIPv4()!=null)) {
			IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
			ipv4InterfaceAddressTLV.setIpv4Address(edgex.getLocalInterfaceIPv4());
			linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
			log.debug("Added interface ip link descriptior->"+ipv4InterfaceAddressTLV.toString());
			if ((edgex!=null)&&(edgex.getNeighborIPv4()!=null)) {
				IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
				ipv4NeighborAddressTLV.setIpv4Address(edgex.getNeighborIPv4());
				linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
				log.debug("Added remote ip link descriptior->"+ipv4NeighborAddressTLV.toString());
			}
		}
		if (interfacesList!=null) {
			IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
			ipv4InterfaceAddressTLV.setIpv4Address(interfacesList.get(0));
			linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
			log.debug("Added interface ip link descriptior->"+ipv4InterfaceAddressTLV.toString());
			IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
			ipv4NeighborAddressTLV.setIpv4Address(interfacesList.get(1));
			linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
			log.debug("Added remote ip link descriptior->"+ipv4NeighborAddressTLV.toString());
		}
		//2.2.2. Link Local/Remote identifiers TLV
		/*
		Commented Andrea aftre adding the interface ip address
		if (localRemoteIfList !=  null){
			LinkLocalRemoteIdentifiersLinkDescriptorSubTLV linkIdentifiersTLV = new LinkLocalRemoteIdentifiersLinkDescriptorSubTLV();
			linkIdentifiersTLV.setLinkLocalIdentifier(localRemoteIfList.get(0));
			linkIdentifiersTLV.setLinkRemoteIdentifier(localRemoteIfList.get(1));
			linkNLRI.setLinkIdentifiersTLV(linkIdentifiersTLV);
		}
		 */

		//2.2.3 LinkDelay
		/*
		if (te_info != null){
			if(te_info.getUndirLinkDelay() != null){
				int undirLinkDelay = te_info.getUndirLinkDelay().getDelay();
				UndirectionalLinkDelayDescriptorSubTLV uSTLV =new UndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setDelay(undirLinkDelay);
				linkNLRI.setUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirDelayVar() != null){
				int undirDelayVar = te_info.getUndirDelayVar().getDelayVar();
				UndirectionalDelayVariationDescriptorSubTLV uSTLV =new UndirectionalDelayVariationDescriptorSubTLV();
				uSTLV.setDelayVar(undirDelayVar);
				linkNLRI.setUndirectionalDelayVariationTLV(uSTLV);
			}
			if(te_info.getMinMaxUndirLinkDelay() != null){
				int minDelay = te_info.getMinMaxUndirLinkDelay().getLowDelay();
				int maxDelay = te_info.getMinMaxUndirLinkDelay().getHighDelay();
				MinMaxUndirectionalLinkDelayDescriptorSubTLV uSTLV =new MinMaxUndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setHighDelay(maxDelay);
				uSTLV.setLowDelay(minDelay);
				linkNLRI.setMinMaxUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirLinkLoss() != null){
				int linkLoss = te_info.getUndirLinkLoss().getLinkLoss();
				UndirectionalLinkLossDescriptorSubTLV uSTLV =new UndirectionalLinkLossDescriptorSubTLV();
				uSTLV.setLinkLoss(linkLoss);
				linkNLRI.setUndirectionalLinkLossTLV(uSTLV);
			}
			if(te_info.getUndirResidualBw() != null){
				int resBw = te_info.getUndirResidualBw().getResidualBw();
				UndirectionalResidualBandwidthDescriptorSubTLV uSTLV =new UndirectionalResidualBandwidthDescriptorSubTLV();
				uSTLV.setResidualBw(resBw);
				linkNLRI.setUndirectionalResidualBwTLV(uSTLV);
			}
			if(te_info.getUndirAvailableBw() != null){
				int availableBw = te_info.getUndirAvailableBw().getAvailableBw();
				UndirectionalAvailableBandwidthDescriptorSubTLV uSTLV =new UndirectionalAvailableBandwidthDescriptorSubTLV();
				uSTLV.setAvailableBw(availableBw);
				linkNLRI.setUndirectionalAvailableBwTLV(uSTLV);
			}
			if(te_info.getUndirUtilizedBw() != null){
				int utilizedBw = te_info.getUndirUtilizedBw().getUtilizedBw();
				UndirectionalUtilizedBandwidthDescriptorSubTLV uSTLV =new UndirectionalUtilizedBandwidthDescriptorSubTLV();
				uSTLV.setUtilizedBw(utilizedBw);
				linkNLRI.setUndirectionalUtilizedBwTLV(uSTLV);
			}
			
		}
		 */
		linkNLRI.setIdentifier(this.identifier);
		BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
		ra.setLsNLRI(linkNLRI);
		if ((learntFrom!="local")&&(learntFrom!=null)){
			try {
				ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		else
			log.info("local or null");
		pathAttributes.add(ra);

		return update;
	}


	//ANDREAINTERDOMAIN
	private BGP4Update createMsgUpdateLinkNLRI2(IntraDomainEdge edgex, ArrayList<Object> addressList, ArrayList<Long> localRemoteIfList, int lanID, ArrayList<String> domainList, boolean intradomain, TE_Information te_info, String learntFrom, ArrayList<Inet4Address> interfacesList) {
		BGP4Update update = new BGP4Update();
		//1. Path Attributes
		ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
		//1.1. Origin
		OriginAttribute or = new OriginAttribute();
		if (intradomain)
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
		else
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
		pathAttributes.add(or);
		///Andrea
		//update.setLearntFrom("192.168.0.1");
		//1.2. AS-PATH

/*
if(multiDomainTEDB.getAsInfo_DB().containsKey(learntFrom))
{
	log.info("AsInfo Key: " + learntFrom);
	for(AsInfo As : multiDomainTEDB.getAsInfo_DB().get(learntFrom))
		log.info("SegmentType: " + As.getType() + "SegmentNumber" + As.getsegmentNumbers() + "SegmentValue " + As.getsegmentValue());
}
*/

		if (send4AS == true) {

			AS4_Path_Attribute as_path = new AS4_Path_Attribute();
			AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
			long[] segs = new long[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));
		} else {
			AS_Path_Attribute as_path = new AS_Path_Attribute();
			AS_Path_Segment as_path_seg = new AS_Path_Segment();
			int[] segs = new int[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));

		}

		//LOCAL PREF Attribute
		LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
		as_local_pref.setValue(LocalPref);
		pathAttributes.add(as_local_pref);
		//1.2. LINK-STATE
		//MPLS
		float maximumBandwidth = 0;
		float[] unreservedBandwidth = null;
		float maximumReservableBandwidth = 0;

		//GMPLS
		AvailableLabels availableLabels = null;
		MF_OTPAttribTLV mfOTP = null;

		int defmetric = 0;
		int te_metric = 0;
		int administrativeGroup = 0;
		boolean adminGrouppresent = false;

		if (te_info != null) {

			//MPLS
			if (te_info.getMaximumBandwidth() != null) {
				maximumBandwidth = te_info.getMaximumBandwidth().getMaximumBandwidth();
			}
			if (te_info.getUnreservedBandwidth() != null)
				unreservedBandwidth = te_info.getUnreservedBandwidth().getUnreservedBandwidth();
			if (te_info.getMaximumReservableBandwidth() != null)
				maximumReservableBandwidth = te_info.getMaximumReservableBandwidth().getMaximumReservableBandwidth();
			//GMPLS
			if (te_info.getAvailableLabels() != null)
				availableLabels = te_info.getAvailableLabels();
			if (te_info.getDefaultTEMetric() != null) {
				defmetric = te_info.getDefaultTEMetric().getLinkMetric();
				log.debug("Default Metric: " + defmetric);
			}
			if (te_info.getMetric() != null) {
				te_metric = te_info.getMetric().getMetric();
				log.debug("Metric en el metodo sendLinkNLRI es: " + te_metric);
			}
			if (te_info.getAdministrativeGroup() != null) {
				adminGrouppresent = true;
				administrativeGroup = te_info.getAdministrativeGroup().getAdministrativeGroup();
				log.debug("Administrative group : " + administrativeGroup);
			}
			if (te_info.getMfOTF() != null) {
				mfOTP = te_info.getMfOTF();
			}

		} else {
			log.info("TE_Info is Null");
		}


		boolean linkStateNeeded = false;
		LinkStateAttribute linkStateAttribute = new LinkStateAttribute();
		//1.2.1. MaxReservableBandwidth
		if (maximumReservableBandwidth != 0) {
			MaxReservableBandwidthLinkAttribTLV maxReservableBandwidthTLV = new MaxReservableBandwidthLinkAttribTLV();
			maxReservableBandwidthTLV.setMaximumReservableBandwidth(maximumReservableBandwidth);
			linkStateAttribute.setMaxReservableBandwidthTLV(maxReservableBandwidthTLV);
			linkStateNeeded = true;
		}
		//1.2.2. maxBandwidth
		if (maximumBandwidth != 0) {
			MaximumLinkBandwidthLinkAttribTLV maximumLinkBandwidthTLV = new MaximumLinkBandwidthLinkAttribTLV();
			maximumLinkBandwidthTLV.setMaximumBandwidth(maximumBandwidth);
			linkStateAttribute.setMaximumLinkBandwidthTLV(maximumLinkBandwidthTLV);
			linkStateNeeded = true;
		}
		//1.2.3. unreservedBandwidth
		if (unreservedBandwidth != null) {
			UnreservedBandwidthLinkAttribTLV unreservedBandwidthTLV = new UnreservedBandwidthLinkAttribTLV();
			unreservedBandwidthTLV.setUnreservedBandwidth(unreservedBandwidth);
			linkStateAttribute.setUnreservedBandwidthTLV(unreservedBandwidthTLV);
			linkStateNeeded = true;
		}
		//1.2.4. AvailableLabels
		if (availableLabels != null) {
			log.debug("Available labels fields: " + availableLabels.getLabelSet().getNumLabels());
			AvailableLabels al = new AvailableLabels();

			BitmapLabelSet bl = new BitmapLabelSet();
			bl.setBytesBitmap(((BitmapLabelSet) availableLabels.getLabelSet()).getBytesBitMap());
			bl.setNumLabels(availableLabels.getLabelSet().getNumLabels());
			bl.setDwdmWavelengthLabel(((BitmapLabelSet) availableLabels.getLabelSet()).getDwdmWavelengthLabel());

			bl.setBytesBitmapReserved(((BitmapLabelSet) availableLabels.getLabelSet()).getBytesBitmapReserved());

			al.setLabelSet(bl);

			log.debug("Campo BytesBitmap: " + Integer.toHexString(((int) bl.getBytesBitMap()[0]) & 0xFF));
			log.debug("Campo DwdmWavelengthLabel: " + bl.getDwdmWavelengthLabel());
			if (bl.getBytesBitmapReserved() != null) {
				log.debug("Campo BytesBitmapReserved: " + bl.getBytesBitmapReserved()[0]);
			}
			linkStateAttribute.setAvailableLabels(al);

			linkStateNeeded = true;
		}

		//1.2.5 metric
		if (te_metric != 0) {
			MetricLinkAttribTLV metricx = new MetricLinkAttribTLV();
			metricx.setMetric(te_metric);
			metricx.setMetric_type(1);
			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + te_metric);
			linkStateAttribute.setMetricTLV(metricx);
			linkStateNeeded = true;
		}

		if (defmetric != 0) {
			DefaultTEMetricLinkAttribTLV defaultMetric = new DefaultTEMetricLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			defaultMetric.setLinkMetric(defmetric);

			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + defmetric);
			linkStateAttribute.setTEMetricTLV(defaultMetric);
			linkStateNeeded = true;
		}
		if (adminGrouppresent) {
			AdministrativeGroupLinkAttribTLV admGroup = new AdministrativeGroupLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			admGroup.setAdministrativeGroup(administrativeGroup);

			log.debug("Administrative Group set: " + administrativeGroup);
			linkStateAttribute.setAdministrativeGroupTLV(admGroup);
			linkStateNeeded = true;
		}

		//1.2.6 MF_OPT
		if (mfOTP != null) {
			MF_OTPAttribTLV mfOTPTLV = mfOTP.duplicate();
			log.debug("SENDING MFOTP OSCAR");
			linkStateAttribute.setMF_OTPAttribTLV(mfOTPTLV);
			linkStateNeeded = true;
		}


		//new TE metrics
		//2.2.3 LinkDelay
		if (te_info != null) {
			if (te_info.getUndirLinkDelay() != null) {
				int undirLinkDelay = te_info.getUndirLinkDelay().getDelay();
				UndirectionalLinkDelayDescriptorSubTLV uSTLV = new UndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setDelay(undirLinkDelay);
				linkStateAttribute.setUndirectionalLinkDelayTLV(uSTLV);
			}
			if (te_info.getUndirDelayVar() != null) {
				int undirDelayVar = te_info.getUndirDelayVar().getDelayVar();
				UndirectionalDelayVariationDescriptorSubTLV uSTLV = new UndirectionalDelayVariationDescriptorSubTLV();
				uSTLV.setDelayVar(undirDelayVar);
				linkStateAttribute.setUndirectionalDelayVariationTLV(uSTLV);
			}
			if (te_info.getMinMaxUndirLinkDelay() != null) {
				int minDelay = te_info.getMinMaxUndirLinkDelay().getLowDelay();
				int maxDelay = te_info.getMinMaxUndirLinkDelay().getHighDelay();
				MinMaxUndirectionalLinkDelayDescriptorSubTLV uSTLV = new MinMaxUndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setHighDelay(maxDelay);
				uSTLV.setLowDelay(minDelay);
				linkStateAttribute.setMinMaxUndirectionalLinkDelayTLV(uSTLV);
			}
			if (te_info.getUndirLinkLoss() != null) {
				int linkLoss = te_info.getUndirLinkLoss().getLinkLoss();
				UndirectionalLinkLossDescriptorSubTLV uSTLV = new UndirectionalLinkLossDescriptorSubTLV();
				uSTLV.setLinkLoss(linkLoss);
				linkStateAttribute.setUndirectionalLinkLossTLV(uSTLV);
			}
			if (te_info.getUndirResidualBw() != null) {
				float resBw = te_info.getUndirResidualBw().getResidualBw();
				UndirectionalResidualBandwidthDescriptorSubTLV uSTLV = new UndirectionalResidualBandwidthDescriptorSubTLV();
				uSTLV.setResidualBw(resBw);
				linkStateAttribute.setUndirectionalResidualBwTLV(uSTLV);
			}
			if (te_info.getUndirAvailableBw() != null) {
				float availableBw = te_info.getUndirAvailableBw().getAvailableBw();
				UndirectionalAvailableBandwidthDescriptorSubTLV uSTLV = new UndirectionalAvailableBandwidthDescriptorSubTLV();
				uSTLV.setAvailableBw(availableBw);
				linkStateAttribute.setUndirectionalAvailableBwTLV(uSTLV);
			}
			if (te_info.getUndirUtilizedBw() != null) {
				float utilizedBw = te_info.getUndirUtilizedBw().getUtilizedBw();
				UndirectionalUtilizedBandwidthDescriptorSubTLV uSTLV = new UndirectionalUtilizedBandwidthDescriptorSubTLV();
				uSTLV.setUtilizedBw(utilizedBw);
				linkStateAttribute.setUndirectionalUtilizedBwTLV(uSTLV);
			}

		}


		if (linkStateNeeded) {
			//log.debug("Link state needed");
			pathAttributes.add(linkStateAttribute);
		}
		//2. NLRI
		LinkNLRI linkNLRI = new LinkNLRI();
		/*
		if (intradomain) {
			linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
		} else {
			//linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
			linkNLRI.setProtocolID(ProtocolIDCodes.IS_IS_Level2_Protocol_ID);
		}
		*/
		linkNLRI.setIdentifier(layer);
		//2.1. Local Y Remote Descriptors
		LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();
		RemoteNodeDescriptorsTLV remoteNodeDescriptors = new RemoteNodeDescriptorsTLV();

		//2.1.1. IPv4
		IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		if (addressList.get(0) instanceof Inet4Address){
			igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
			igpRouterIDLNSubTLV.setIpv4AddressOSPF((Inet4Address) addressList.get(0));
			localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);
			linkNLRI.setProtocolID(ProtocolIDCodes.OSPF_Protocol_ID);
			log.debug("src node is ipv4-> protocol set to OSPF: "+((Inet4Address) addressList.get(0)).getHostAddress());
		}
		if (addressList.get(0) instanceof Long) {
			igpRouterIDLNSubTLV.setISIS_ISO_NODE_ID((long) addressList.get(0));
			igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO);
			localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);
			linkNLRI.setProtocolID(ProtocolIDCodes.IS_IS_Level2_Protocol_ID);
			log.debug("src node is long-> protocol set to ISIS");
		}
		//Complete Dummy TLVs
		//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV =new BGPLSIdentifierNodeDescriptorSubTLV();
		//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
		//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
		areaID.setAREA_ID(this.localAreaID);
		//commented for compliance with ODL
		// localNodeDescriptors.setAreaID(areaID);

		IGPRouterIDNodeDescriptorSubTLV igpRouterIDDNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		if (addressList.get(1) instanceof Inet4Address) {
			igpRouterIDDNSubTLV.setIpv4AddressOSPF((Inet4Address) addressList.get(1));
			igpRouterIDDNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_OSPF_NON_PSEUDO);
			remoteNodeDescriptors.setIGPRouterID(igpRouterIDDNSubTLV);
			log.debug("dst node is ipv4-> protocol set to OSPF: "+((Inet4Address) addressList.get(1)).getHostAddress());
		}
		if (addressList.get(1) instanceof Long) {
			igpRouterIDDNSubTLV.setISIS_ISO_NODE_ID((long) addressList.get(1));
			igpRouterIDDNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO);
			remoteNodeDescriptors.setIGPRouterID(igpRouterIDDNSubTLV);
			log.debug("dst node is long-> protocol set to ISIS");
		}//2.1.2. AS
		if (domainList != null){
			AutonomousSystemNodeDescriptorSubTLV as_local = new AutonomousSystemNodeDescriptorSubTLV();
			try {
				as_local.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(0)));
				localNodeDescriptors.setAutonomousSystemSubTLV(as_local);
				AutonomousSystemNodeDescriptorSubTLV as_remote = new AutonomousSystemNodeDescriptorSubTLV();
				as_remote.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(1)));
				remoteNodeDescriptors.setAutonomousSystemSubTLV(as_remote);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//Complete Dummy TLVs
		//remoteNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		//commented for compliance with ODL
		// remoteNodeDescriptors.setAreaID(areaID);

		linkNLRI.setLocalNodeDescriptors(localNodeDescriptors);
		linkNLRI.setRemoteNodeDescriptorsTLV(remoteNodeDescriptors);

		//2.2. Link NLRI TLVs
		//2.2.1. Ipv4 interface and neighbour address
		//IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
		//IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
		//ipv4InterfaceAddressTLV.setIpv4Address(addressList.get(0));
		//ipv4NeighborAddressTLV.setIpv4Address(addressList.get(1));
		//linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
		//linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
		if ((edgex!=null)&&(edgex.getLocalInterfaceIPv4()!=null)) {
			IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
			ipv4InterfaceAddressTLV.setIpv4Address(edgex.getLocalInterfaceIPv4());
			linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
			log.debug("Added interface ip link descriptior->"+ipv4InterfaceAddressTLV.toString());
			if ((edgex!=null)&&(edgex.getNeighborIPv4()!=null)) {
				IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
				ipv4NeighborAddressTLV.setIpv4Address(edgex.getNeighborIPv4());
				linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
				log.debug("Added remote ip link descriptior->"+ipv4NeighborAddressTLV.toString());
			}
		}
		if (interfacesList!=null) {
			IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
			ipv4InterfaceAddressTLV.setIpv4Address(interfacesList.get(0));
			linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
			if (linkNLRI.getLocalNodeDescriptors()!=null)
				linkNLRI.getLocalNodeDescriptors().getIGPRouterID().setIpv4Address_ospf_dr_address(interfacesList.get(0));
			log.debug("222Added interface ip link descriptior->"+ipv4InterfaceAddressTLV.toString());
			IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
			ipv4NeighborAddressTLV.setIpv4Address(interfacesList.get(1));
			linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
			log.debug("22222Added remote ip link descriptior->"+ipv4NeighborAddressTLV.toString());
			if (linkNLRI.getRemoteNodeDescriptorsTLV()!=null)
				linkNLRI.getRemoteNodeDescriptorsTLV().getIGPRouterID().setIpv4Address_ospf_dr_address(interfacesList.get(1));

		}

		linkNLRI.setIdentifier(this.identifier);
		BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
		ra.setLsNLRI(linkNLRI);
		if ((learntFrom!="local")&&(learntFrom!=null)){
			try {
				if (learntFrom.contains("/"))
					ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
				else
					ra.setNextHop(InetAddress.getByName(learntFrom));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		else {
			log.info("local or null");
		}
		pathAttributes.add(ra);
		log.debug(update.toString());
		return update;
	}

















	private BGP4Update createMsgUpdateLinkNLRIISIS(IntraDomainEdge edgex, long src, long dst, int lanID, ArrayList<String> domainList, boolean intradomain, TE_Information te_info, String learntFrom, Inet4Address local, Inet4Address neighbor){
		log.debug("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXAndrea Sending link NLRI ISIS ");
		BGP4Update update= new BGP4Update();
		//1. Path Attributes
		ArrayList<PathAttribute> pathAttributes = update.getPathAttributes();
		//1.1. Origin
		OriginAttribute or = new OriginAttribute();
		if (intradomain)
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_IGP);
		else
			or.setValue(PathAttributesTypeCode.PATH_ATTRIBUTE_ORIGIN_EGP);
		pathAttributes.add(or);
		///Andrea
		//update.setLearntFrom("192.168.0.1");
		//1.2. AS-PATH

/*
if(multiDomainTEDB.getAsInfo_DB().containsKey(learntFrom))
{
	log.info("AsInfo Key: " + learntFrom);
	for(AsInfo As : multiDomainTEDB.getAsInfo_DB().get(learntFrom))
		log.info("SegmentType: " + As.getType() + "SegmentNumber" + As.getsegmentNumbers() + "SegmentValue " + As.getsegmentValue());
}
*/


		if (send4AS==true) {


			AS4_Path_Attribute as_path = new AS4_Path_Attribute();
			AS4_Path_Segment as_path_seg = new AS4_Path_Segment();
			long[] segs = new long[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			//as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));
		}
		else {
			AS_Path_Attribute as_path = new AS_Path_Attribute();
			AS_Path_Segment as_path_seg = new AS_Path_Segment();
			int[] segs = new int[1];
			segs[0] = ASnumber;
			as_path_seg.setSegments(segs);
			//as_path.getAsPathSegments().add(as_path_seg);
			pathAttributes.add(as_path);
			//log.info("Learnt From: " +learntFrom   +  " SegmentValue: " + String.valueOf(as_path_seg.getSegments()));

		}



		//LOCAL PREF Attribute
		LOCAL_PREF_Attribute as_local_pref = new LOCAL_PREF_Attribute();
		as_local_pref.setValue(LocalPref);
		pathAttributes.add(as_local_pref);
		//1.2. LINK-STATE
		//MPLS
		float maximumBandwidth = 0;
		float[] unreservedBandwidth = null;
		float maximumReservableBandwidth = 0;

		//GMPLS
		AvailableLabels availableLabels = null;
		MF_OTPAttribTLV mfOTP = null;

		int defmetric = 0;
		int te_metric = 0;
		int administrativeGroup=0;
		boolean adminGroupPresent= false;
		if (te_info != null){
			log.debug("Sending TE info link NLRI ISIS not null");


			//MPLS
			if (te_info.getMaximumBandwidth() != null) {
				maximumBandwidth = te_info.getMaximumBandwidth().getMaximumBandwidth();
			}
			if (te_info.getUnreservedBandwidth() != null)
				unreservedBandwidth = te_info.getUnreservedBandwidth().getUnreservedBandwidth();
			if (te_info.getMaximumReservableBandwidth() != null)
				maximumReservableBandwidth = te_info.getMaximumReservableBandwidth().getMaximumReservableBandwidth();
			//GMPLS
			if (te_info.getAvailableLabels() != null)
				availableLabels = te_info.getAvailableLabels();
			if(te_info.getDefaultTEMetric()!=null){
				defmetric = te_info.getDefaultTEMetric().getLinkMetric();
				log.debug("Default Metric: " + defmetric);
			}
			if(te_info.getMetric()!=null){
				te_metric = te_info.getMetric().getMetric() ;
				log.debug("Metric en el metodo sendLinkNLRI es: " + te_metric);
			}
			if(te_info.getMfOTF()!=null){
				mfOTP =  te_info.getMfOTF();
			}
			if (te_info.getAdministrativeGroup() != null) {
				adminGroupPresent=true;
				administrativeGroup = te_info.getAdministrativeGroup().getAdministrativeGroup();
			}

		}else{
			log.info("TE_Info is Null");
		}




		boolean linkStateNeeded = false;
		LinkStateAttribute  linkStateAttribute = new LinkStateAttribute();
		//1.2.1. MaxReservableBandwidth
		if (maximumReservableBandwidth != 0){
			MaxReservableBandwidthLinkAttribTLV maxReservableBandwidthTLV = new MaxReservableBandwidthLinkAttribTLV();
			maxReservableBandwidthTLV.setMaximumReservableBandwidth(maximumReservableBandwidth);
			linkStateAttribute.setMaxReservableBandwidthTLV(maxReservableBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.2. maxBandwidth
		if (maximumBandwidth != 0){
			MaximumLinkBandwidthLinkAttribTLV maximumLinkBandwidthTLV = new MaximumLinkBandwidthLinkAttribTLV();
			maximumLinkBandwidthTLV.setMaximumBandwidth(maximumBandwidth);
			linkStateAttribute.setMaximumLinkBandwidthTLV(maximumLinkBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.3. unreservedBandwidth
		if (unreservedBandwidth != null){
			UnreservedBandwidthLinkAttribTLV unreservedBandwidthTLV = new UnreservedBandwidthLinkAttribTLV();
			unreservedBandwidthTLV.setUnreservedBandwidth(unreservedBandwidth);
			linkStateAttribute.setUnreservedBandwidthTLV(unreservedBandwidthTLV);
			linkStateNeeded=true;
		}
		//1.2.4. AvailableLabels
		if (availableLabels != null){
			log.debug("Available labels fields: "+availableLabels.getLabelSet().getNumLabels());
			AvailableLabels al = new AvailableLabels();

			BitmapLabelSet bl = new BitmapLabelSet();
			bl.setBytesBitmap(((BitmapLabelSet)availableLabels.getLabelSet()).getBytesBitMap());
			bl.setNumLabels(availableLabels.getLabelSet().getNumLabels());
			bl.setDwdmWavelengthLabel(((BitmapLabelSet)availableLabels.getLabelSet()).getDwdmWavelengthLabel());

			bl.setBytesBitmapReserved(((BitmapLabelSet)availableLabels.getLabelSet()).getBytesBitmapReserved());

			al.setLabelSet(bl);

			log.debug("Campo BytesBitmap: "+Integer.toHexString(((int)bl.getBytesBitMap()[0])&0xFF));
			log.debug("Campo DwdmWavelengthLabel: "+bl.getDwdmWavelengthLabel());
			if (bl.getBytesBitmapReserved()!=null){
				log.debug("Campo BytesBitmapReserved: "+bl.getBytesBitmapReserved()[0]);
			}
			linkStateAttribute.setAvailableLabels(al);

			linkStateNeeded=true;
		}

		//1.2.5 metric
		if (te_metric != 0){
			MetricLinkAttribTLV metricx = new MetricLinkAttribTLV();
			metricx.setMetric(te_metric);
			metricx.setMetric_type(3);
			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + te_metric);
			linkStateAttribute.setMetricTLV(metricx);
			linkStateNeeded=true;
		}

		if (defmetric != 0){
			DefaultTEMetricLinkAttribTLV defaultMetric = new DefaultTEMetricLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			defaultMetric.setLinkMetric(defmetric);

			log.debug("Metric en el metodo createMsgUpdateLinkNLRI es: " + defmetric);
			linkStateAttribute.setTEMetricTLV(defaultMetric);
			linkStateNeeded=true;
		}
		if (adminGroupPresent){
			AdministrativeGroupLinkAttribTLV admGroup = new AdministrativeGroupLinkAttribTLV();
			//defaultMetric.setLinkMetric(metric);
			admGroup.setAdministrativeGroup(administrativeGroup);

			log.debug("Administrative Group set: " + administrativeGroup);
			linkStateAttribute.setAdministrativeGroupTLV(admGroup);
			linkStateNeeded=true;
		}

		//1.2.6 MF_OPT
		if (mfOTP != null){
			MF_OTPAttribTLV mfOTPTLV = mfOTP.duplicate();
			log.debug("SENDING MFOTP OSCAR");
			linkStateAttribute.setMF_OTPAttribTLV(mfOTPTLV);
			linkStateNeeded=true;
		}


		//new TE metrics
		//2.2.3 LinkDelay
		if (te_info != null){
			if(te_info.getUndirLinkDelay() != null){
				int undirLinkDelay = te_info.getUndirLinkDelay().getDelay();
				UndirectionalLinkDelayDescriptorSubTLV uSTLV =new UndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setDelay(undirLinkDelay);
				linkStateAttribute.setUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirDelayVar() != null){
				int undirDelayVar = te_info.getUndirDelayVar().getDelayVar();
				UndirectionalDelayVariationDescriptorSubTLV uSTLV =new UndirectionalDelayVariationDescriptorSubTLV();
				uSTLV.setDelayVar(undirDelayVar);
				linkStateAttribute.setUndirectionalDelayVariationTLV(uSTLV);
			}
			if(te_info.getMinMaxUndirLinkDelay() != null){
				int minDelay = te_info.getMinMaxUndirLinkDelay().getLowDelay();
				int maxDelay = te_info.getMinMaxUndirLinkDelay().getHighDelay();
				MinMaxUndirectionalLinkDelayDescriptorSubTLV uSTLV =new MinMaxUndirectionalLinkDelayDescriptorSubTLV();
				uSTLV.setHighDelay(maxDelay);
				uSTLV.setLowDelay(minDelay);
				linkStateAttribute.setMinMaxUndirectionalLinkDelayTLV(uSTLV);
			}
			if(te_info.getUndirLinkLoss() != null){
				int linkLoss = te_info.getUndirLinkLoss().getLinkLoss();
				UndirectionalLinkLossDescriptorSubTLV uSTLV =new UndirectionalLinkLossDescriptorSubTLV();
				uSTLV.setLinkLoss(linkLoss);
				linkStateAttribute.setUndirectionalLinkLossTLV(uSTLV);
			}
			if(te_info.getUndirResidualBw() != null){
				float resBw = te_info.getUndirResidualBw().getResidualBw();
				UndirectionalResidualBandwidthDescriptorSubTLV uSTLV =new UndirectionalResidualBandwidthDescriptorSubTLV();
				uSTLV.setResidualBw(resBw);
				linkStateAttribute.setUndirectionalResidualBwTLV(uSTLV);
			}
			if(te_info.getUndirAvailableBw() != null){
				float availableBw = te_info.getUndirAvailableBw().getAvailableBw();
				UndirectionalAvailableBandwidthDescriptorSubTLV uSTLV =new UndirectionalAvailableBandwidthDescriptorSubTLV();
				uSTLV.setAvailableBw(availableBw);
				linkStateAttribute.setUndirectionalAvailableBwTLV(uSTLV);
			}
			if(te_info.getUndirUtilizedBw() != null){
				float utilizedBw = te_info.getUndirUtilizedBw().getUtilizedBw();
				UndirectionalUtilizedBandwidthDescriptorSubTLV uSTLV =new UndirectionalUtilizedBandwidthDescriptorSubTLV();
				uSTLV.setUtilizedBw(utilizedBw);
				linkStateAttribute.setUndirectionalUtilizedBwTLV(uSTLV);
			}

		}




		if (linkStateNeeded){
			//log.debug("Link state needed");
			log.debug("Link NLRI ISIS link state needed");
			log.debug(linkStateAttribute.toString());
			pathAttributes.add(linkStateAttribute);

		}
		//2. NLRI
		LinkNLRI linkNLRI = new LinkNLRI();
		linkNLRI.setProtocolID(ProtocolIDCodes.IS_IS_Level2_Protocol_ID);
		linkNLRI.setIdentifier(0);

		//2.1. Local Y Remote Descriptors
		LocalNodeDescriptorsTLV localNodeDescriptors = new LocalNodeDescriptorsTLV();
		RemoteNodeDescriptorsTLV remoteNodeDescriptors = new RemoteNodeDescriptorsTLV();

		//2.1.1. IPv4
		IGPRouterIDNodeDescriptorSubTLV igpRouterIDLNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		//igpRouterIDLNSubTLV.setIpv4AddressOSPF(addressList.get(0));
		igpRouterIDLNSubTLV.setISIS_ISO_NODE_ID(src);
		igpRouterIDLNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO);
		localNodeDescriptors.setIGPRouterID(igpRouterIDLNSubTLV);
		log.debug("Local node link descriptior->"+localNodeDescriptors.toString());
		//Complete Dummy TLVs
		//BGPLSIdentifierNodeDescriptorSubTLV bGPLSIDSubTLV =new BGPLSIdentifierNodeDescriptorSubTLV();
		//bGPLSIDSubTLV.setBGPLS_ID(this.localBGPLSIdentifer);
		//localNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		AreaIDNodeDescriptorSubTLV areaID = new AreaIDNodeDescriptorSubTLV();
		areaID.setAREA_ID(this.localAreaID);
		//commented for compliance with ODL
		// localNodeDescriptors.setAreaID(areaID);

		IGPRouterIDNodeDescriptorSubTLV igpRouterIDDNSubTLV = new IGPRouterIDNodeDescriptorSubTLV();
		igpRouterIDDNSubTLV.setISIS_ISO_NODE_ID(dst);
		igpRouterIDDNSubTLV.setIGP_router_id_type(IGPRouterIDNodeDescriptorSubTLV.IGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO);
		remoteNodeDescriptors.setIGPRouterID(igpRouterIDDNSubTLV);
		log.debug("Remote node link descriptior->"+remoteNodeDescriptors.toString());
		//2.1.2. AS
		if (domainList != null){
			AutonomousSystemNodeDescriptorSubTLV as_local = new AutonomousSystemNodeDescriptorSubTLV();
			try {
				as_local.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(0)));
				localNodeDescriptors.setAutonomousSystemSubTLV(as_local);
				AutonomousSystemNodeDescriptorSubTLV as_remote = new AutonomousSystemNodeDescriptorSubTLV();
				as_remote.setAS_ID((Inet4Address) Inet4Address.getByName(domainList.get(1)));
				remoteNodeDescriptors.setAutonomousSystemSubTLV(as_remote);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//Complete Dummy TLVs
		//remoteNodeDescriptors.setBGPLSIDSubTLV(bGPLSIDSubTLV);
		//commented for compliance with ODL
		// remoteNodeDescriptors.setAreaID(areaID);

		linkNLRI.setLocalNodeDescriptors(localNodeDescriptors);
		linkNLRI.setRemoteNodeDescriptorsTLV(remoteNodeDescriptors);
		//2.2. Link NLRI TLVs
		//2.2.1. Ipv4 interface and neighbour address
		if (edgex.getLocalInterfaceIPv4()!=null) {
			IPv4InterfaceAddressLinkDescriptorsSubTLV ipv4InterfaceAddressTLV = new IPv4InterfaceAddressLinkDescriptorsSubTLV();
			ipv4InterfaceAddressTLV.setIpv4Address(edgex.getLocalInterfaceIPv4());
			linkNLRI.setIpv4InterfaceAddressTLV(ipv4InterfaceAddressTLV);
			log.debug("Added interface ip link descriptior->"+ipv4InterfaceAddressTLV.toString());
			if (edgex.getNeighborIPv4()!=null) {
				IPv4NeighborAddressLinkDescriptorSubTLV ipv4NeighborAddressTLV = new IPv4NeighborAddressLinkDescriptorSubTLV();
				ipv4NeighborAddressTLV.setIpv4Address(edgex.getNeighborIPv4());
				linkNLRI.setIpv4NeighborAddressTLV(ipv4NeighborAddressTLV);
				log.debug("Added remote ip link descriptior->"+ipv4NeighborAddressTLV.toString());
			}
		}
		linkNLRI.setIdentifier(this.identifier);
		log.debug("The Link NLRI is:"+linkNLRI.toString());
		BGP_LS_MP_Reach_Attribute ra= new BGP_LS_MP_Reach_Attribute();
		ra.setLsNLRI(linkNLRI);
		if (learntFrom!="local"){
			try {
				ra.setNextHop(InetAddress.getByName(learntFrom.replaceAll("/","")));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		else
			log.info("local");
		pathAttributes.add(ra);
		log.debug("The final update is :"+ update.toString());
		return update;
	}



	/**
	 * Funcion que crea un mensaje OSPF inventado desde cero.
	 * Solo se meten en el mensaje los campos:
	 * - source
	 * - destino
	 * - maximun bandwithd
	 * @return OSPFv2 Link State Update Packet
	 */
	public static OSPFv2LinkStateUpdatePacket createMsgOSPF(){
		Inet4Address src = null;
		Inet4Address dst = null;
		MaximumBandwidth maximumBandwidth = new MaximumBandwidth();
		maximumBandwidth.setMaximumBandwidth(100);
		UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
		float[] unReservedB = new float[8];
		unReservedB[0]=18309;
		unReservedB[1]=130;
		unreservedBandwidth.setUnreservedBandwidth(unReservedB);
		try {
			src = (Inet4Address) Inet4Address.getByName( "179.123.123.123");
			dst = (Inet4Address) Inet4Address.getByName( "179.123.123.111");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		OSPFv2LinkStateUpdatePacket ospfv2Packet = new OSPFv2LinkStateUpdatePacket();
		ospfv2Packet.setRouterID(src);
		LinkedList<LSA> lsaList = new LinkedList<LSA>();
		OSPFTEv2LSA lsa = new OSPFTEv2LSA();
		LinkTLV linkTLV=new LinkTLV();
		lsa.setLinkTLV(linkTLV);

		linkTLV.setMaximumBandwidth(maximumBandwidth);
		linkTLV.setUnreservedBandwidth(unreservedBandwidth);
		LocalInterfaceIPAddress localInterfaceIPAddress= new LocalInterfaceIPAddress();
		LinkedList<Inet4Address> lista =localInterfaceIPAddress.getLocalInterfaceIPAddressList();
		lista.add(src);
		linkTLV.setLocalInterfaceIPAddress(localInterfaceIPAddress);
		RemoteInterfaceIPAddress remoteInterfaceIPAddress= new RemoteInterfaceIPAddress();
		LinkedList<Inet4Address> listar = remoteInterfaceIPAddress.getRemoteInterfaceIPAddressList();
		listar.add(dst);
		linkTLV.setRemoteInterfaceIPAddress(remoteInterfaceIPAddress);
		LinkID linkID = new LinkID();
		linkID.setLinkID(dst);
		linkTLV.setLinkID(linkID);
		//		if (edge.getTE_info().getAvailableLabels() != null){
		//			linkTLV.setAvailableLabels(edge.getTE_info().getAvailableLabels());			
		//		}
		lsaList.add(lsa);

		ospfv2Packet.setLSAlist(lsaList);
		return ospfv2Packet;
	}

	//* Funcion que decodifica un mensaje OSPFv2LinkStateUpdatePacket creando con los campos extraidos un mensaje BGP4 update.
	public BGP4Update decodificarMsgOSPF(OSPFv2LinkStateUpdatePacket ospfv2Packet){
		boolean intradomain = true;
		Inet4Address localIPAddress = ospfv2Packet.getRouterID();
		Inet4Address remoteIPAddress = null;
		long localInterfaceIPAddress = -1;
		long remoteInterfaceIPAddress = -1;
		Inet4Address remoteASNumber = null;
		LinkedList<LSA> lsaList;
		OSPFTEv2LSA lsa;
		//GMPLS Parameter
		AvailableLabels al = null;
		//MPLS Parameter
		float maxBandwidth = 0;
		float[] unBandwidth = null;
		float maximumReservableBandwidth=0;

		lsaList = ((OSPFv2LinkStateUpdatePacket)ospfv2Packet).getLSAlist();
		for (int i =0;i< lsaList.size();i++){
			if (lsaList.get(i).getLStype() == LSATypes.TYPE_10_OPAQUE_LSA){
				lsa=(OSPFTEv2LSA)lsaList.get(i);
				log.debug("Starting to process LSA");

				LinkTLV linkTLV = lsa.getLinkTLV();
				if (linkTLV!=null){
					//Local and Remote interface IP address
					remoteIPAddress = linkTLV.getLinkID().getLinkID();					
					log.debug("Remote IP Address: "+remoteIPAddress);	
					localInterfaceIPAddress = linkTLV.getLinkLocalRemoteIdentifiers().getLinkLocalIdentifier();
					log.debug("Local Interface: "+localInterfaceIPAddress);
					remoteInterfaceIPAddress =linkTLV.getLinkLocalRemoteIdentifiers().getLinkRemoteIdentifier();					
					log.debug("Remote Interface: "+remoteInterfaceIPAddress);

					//MPLS fields
					if (linkTLV.getMaximumBandwidth() != null)
						maxBandwidth = linkTLV.getMaximumBandwidth().getMaximumBandwidth();					
					if (linkTLV.getUnreservedBandwidth() != null)
						unBandwidth = linkTLV.getUnreservedBandwidth().getUnreservedBandwidth();					
					if (linkTLV.getMaximumReservableBandwidth()!= null)
						maximumReservableBandwidth = linkTLV.getMaximumReservableBandwidth().getMaximumReservableBandwidth();

					//GMPLS
					al = linkTLV.getAvailableLabels(); 
					//FIXME: Como ver si es inter o intra domain
					if (linkTLV.getRemoteASNumber() != null)
						remoteASNumber = linkTLV.getRemoteASNumber().getRemoteASNumber();

				}
			}

		}
		//Create the address list
		ArrayList<Inet4Address> addressList = new ArrayList<Inet4Address>();
		addressList.add(localIPAddress);
		addressList.add(remoteIPAddress);
		//Create the interface list
		ArrayList<Long> localRemoteIfList = new ArrayList<Long>();
		localRemoteIfList.add(localInterfaceIPAddress);
		localRemoteIfList.add(remoteInterfaceIPAddress);


		//Create the domain List
		ArrayList<Inet4Address> domainList = new ArrayList<Inet4Address>(2);
		//FIXME CHECK IF THIS METHOD IS USED
		//return createMsgUpdateLinkNLRI(addressList,localRemoteIfList,23,maxBandwidth,unBandwidth,maximumReservableBandwidth,al, 0,0, domainList, intradomain, null);
		return null;
	}

	public boolean isSendTopology() {
		return sendTopology;
	}


	public void setSendTopology(boolean sendTopology) {
		this.sendTopology = sendTopology;
	}

	public void setisTest(boolean test) {
		this.isTest = test;
	}

	public boolean getisTest() {
		return this.isTest;
	}


	public BGP4SessionsInformation getBgp4SessionsInformation() {
		return bgp4SessionsInformation;
	}


	public void setBgp4SessionsInformation(
			BGP4SessionsInformation bgp4SessionsInformation) {
		this.bgp4SessionsInformation = bgp4SessionsInformation;
	}


	public void setInstanceId(int instanceId) {
		this.instanceId = instanceId;
	}


}

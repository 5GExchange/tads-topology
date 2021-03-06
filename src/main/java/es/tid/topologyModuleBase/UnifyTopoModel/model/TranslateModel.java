package es.tid.topologyModuleBase.UnifyTopoModel.model;

import es.tid.tedb.*;

import java.net.Inet4Address;
import java.util.*;

//import es.tid.tedb.elements.Link;


public class TranslateModel {
	

	

	public static Port translateNode(DomainTEDB db, es.tid.tedb.elements.Node n){
		Port port = new Port();
		port.setId( ((es.tid.tedb.elements.Node) n).getNodeID() );
		return port;
	}
	
	public static Port translateNodeIp(DomainTEDB ted, Inet4Address n, Node_Info nodeInfo) {
		Port port = new Port();
		port.setId(((java.net.Inet4Address) n).getHostAddress());
		return port;
	}

	public static Port translateNodeLong(DomainTEDB ted, long n, Node_Info nodeInfo) {
		Port port = new Port();

		if(nodeInfo!=null) {
			if (nodeInfo.getIpv4AddressLocalNode()!=null) {
				//System.out.println(nodeInfo.toString());
				port.setId(nodeInfo.getIpv4AddressLocalNode().getHostAddress());
			}
			else {
				//System.out.println("null");
				port.setId(String.valueOf(n));
			}
		}
		else {
			//System.out.println("null");
			port.setId(String.valueOf(n));
		}
		return port;
	}
	
	
	public static Node translateTopology(String domainID, DomainTEDB ted){
		  Node node = new Node();
		  node.setId(domainID);
		  
		  //Translating nodes from TEDB to Unify Model
		  PortsSchema ports = new PortsSchema();
		  List<Port> portlist = new ArrayList<Port>();
		  for(Object nodeFromTed : ted.getIntraDomainLinksvertexSet()){			  
			  if(nodeFromTed instanceof es.tid.tedb.elements.Node){
				  portlist.add(translateNode(ted, (es.tid.tedb.elements.Node)nodeFromTed));
			  } else if(nodeFromTed instanceof java.net.Inet4Address){
				  portlist.add(translateNodeIp(ted, (java.net.Inet4Address)nodeFromTed, ted.getNodeTable().get(nodeFromTed)));
			  }
			  else if(nodeFromTed instanceof Long){
				  //portlist.add(translateNodeIp(ted, (java.net.Inet4Address)nodeFromTed, ted.getNodeTable().get(node)));
				  //portlist.add(translateNodeLong(ted, (long) nodeFromTed, ted.getNodeTable().get(node)));
				  portlist.add(translateNodeLong(ted, (long) nodeFromTed, ted.getNodeTable().get((long)nodeFromTed)));
			  }
			  
		  }
		  
		  node.setLinks(translateIntraDomainLinks(domainID, ted));
		  if (ted.getItResources()!=null){
			  SoftwareResource resources = new SoftwareResource();		  
			  resources.setCpu(ted.getItResources().getCpu());
			  resources.setMem(ted.getItResources().getMem());
			  resources.setStorage(ted.getItResources().getStorage());
			  node.setResources(resources);
			  Metadata metadata = new Metadata();
			  MetadataMetadata e= new MetadataMetadata();
			  e.setKey("unify-slor");
			  e.setValue(ted.getItResources().getControllerIT());
			  metadata.getMetadata().add(e);
			  node.metadata.add(e);
		  }
				  
		  ports.setPort(portlist);
		  node.setPorts(ports);
		  
		  return node;
	  }
	
	public static LinksSchema translateInterDomainLinks(String domainID, MultiDomainTEDB ted, Hashtable<String,TEDB> all){
			//Translating InterDomain links from TEDB to Unify Model
			LinksSchema links = new LinksSchema();
			List<Link> linklist = new ArrayList<Link>();
			
			for(InterDomainEdge linkTed : ted.getInterDomainLinks()){
				Link link = new Link();
				if (linkTed.getSrc_router_id() instanceof java.net.Inet4Address){
					String src = ((java.net.Inet4Address) linkTed.getSrc_router_id()).getHostAddress();
					String srcDomain = ((java.net.Inet4Address) linkTed.getDomain_src_router()).getHostAddress();
					String srcUnify = "/nodes/node[id="+srcDomain+"]/ports/port[id="+src+"]";
					link.setSrc(srcUnify);		
				}
				else if (linkTed.getSrc_router_id() instanceof Long){
					//String src = "/nodes/node[id="+domainID+"]/ports/port[id="+String.valueOf(linkTed.getSource())+"]";
					String src=null;
					String srcDomain = ((java.net.Inet4Address) linkTed.getDomain_src_router()).getHostAddress();
					for(Map.Entry<String, TEDB>entry : all.entrySet() ){
						if (entry.getValue() instanceof DomainTEDB) {
							Node_Info node_info=((DomainTEDB) entry.getValue()).getNodeTable().get(linkTed.getSrc_router_id());
							if (node_info!=null){
								if (node_info.getIpv4AddressLocalNode()!=null) {
									//System.out.println(node.toString());
									src = "/nodes/node[id="+srcDomain+"]/ports/port[id="+node_info.getIpv4AddressLocalNode().getHostAddress()+"]";
									//port.setId(node.getIpv4AddressLocalNode().getHostAddress());
								}
								else {
									src = "/nodes/node[id="+srcDomain+"]/ports/port[id="+String.valueOf(linkTed.getSource())+"]";
									//System.out.println("null");
								}
								link.setSrc(src);

							}
						}
					}

				}

				else{

					System.out.println("SRC Type of InterDomain link not implemented for UnifyModel");
				}
				
				if (linkTed.getDst_router_id() instanceof java.net.Inet4Address){
					String dst = ((java.net.Inet4Address) linkTed.getDst_router_id()).getHostAddress();
					String dstDomain = ((java.net.Inet4Address) linkTed.getDomain_dst_router()).getHostAddress();
					String dstUnify = "/nodes/node[id="+dstDomain+"]/ports/port[id="+dst+"]";
					link.setDst(dstUnify);					
				}
				else if (linkTed.getDst_router_id() instanceof Long){
					//String src = "/nodes/node[id="+domainID+"]/ports/port[id="+String.valueOf(linkTed.getSource())+"]";
					String dst=null;
					//String dstDomain = ((java.net.Inet4Address) linkTed.getDomain_dst_router()).getHostAddress();
					//Cialissimo
					String dstDomain =((Inet4Address) linkTed.getDomain_dst_router()).getHostAddress();
					for(Map.Entry<String, TEDB>entry : all.entrySet() ){
						if (entry.getValue() instanceof DomainTEDB) {
							Node_Info node_info=((DomainTEDB) entry.getValue()).getNodeTable().get(linkTed.getDst_router_id());
							if (node_info!=null){
								if (node_info.getIpv4AddressLocalNode()!=null) {
									//System.out.println(node.toString());
									dst = "/nodes/node[id="+dstDomain+"]/ports/port[id="+node_info.getIpv4AddressLocalNode().getHostAddress()+"]";
									//port.setId(node.getIpv4AddressLocalNode().getHostAddress());
								}
								else {
									dst = "/nodes/node[id="+dstDomain+"]/ports/port[id="+String.valueOf(linkTed.getTarget())+"]";
									//System.out.println("null");
								}
								link.setDst(dst);

							}
						}
					}

				}
				else{
					System.out.println("DST Type of InterDomain link not implemented for UnifyModel"+linkTed.getDst_router_id().toString());
				}
				linklist.add(link);
			}
			links.setLink(linklist);
		  
			return links;
		}
	
	public static LinksSchema translateIntraDomainLinks(String domainID, DomainTEDB ted){
		//Translating InterDomain links from TEDB to Unify Model
		LinksSchema links = new LinksSchema();
		List<Link> linklist = new ArrayList<Link>();
		
	  
		for(IntraDomainEdge linkTed : ted.getIntraDomainLinks()){
			Link link = new Link();
			link.setId(linkTed.getLinkID());
			
			if( linkTed.getSource() instanceof  java.net.Inet4Address){
				String src = "/nodes/node[id="+domainID+"]/ports/port[id="+((java.net.Inet4Address)linkTed.getSource()).getHostAddress()+"]";
				link.setSrc(src);
			//} else if( linkTed.getSource( instanceof es.tid.tedb.elements.EndPoint ){
				
			}
			else if (linkTed.getSource() instanceof Long){
				//String src = "/nodes/node[id="+domainID+"]/ports/port[id="+String.valueOf(linkTed.getSource())+"]";
				String src=null;
				Node_Info node=ted.getNodeTable().get(linkTed.getSource());
				if (node.getIpv4AddressLocalNode()!=null) {
					//System.out.println(node.toString());
					src = "/nodes/node[id="+domainID+"]/ports/port[id="+node.getIpv4AddressLocalNode().getHostAddress()+"]";
					//port.setId(node.getIpv4AddressLocalNode().getHostAddress());
				}
				else {
					src = "/nodes/node[id="+domainID+"]/ports/port[id="+String.valueOf(linkTed.getSource())+"]";
					//System.out.println("null");
				}
				link.setSrc(src);
			}
			else {
				System.out.println("Type of link not implemented for UnifyModel");
			}
			
			
			if( linkTed.getTarget() instanceof  java.net.Inet4Address) {
				String dst = "/nodes/node[id=" + domainID + "]/ports/port[id=" + ((java.net.Inet4Address) linkTed.getTarget()).getHostAddress() + "]";
				link.setDst(dst);
				//} else if( linkTed.getTarget( instanceof es.tid.tedb.elements.EndPoint ){

			}
			else if( linkTed.getTarget() instanceof  Long) {
				String dst=null;
				Node_Info node=ted.getNodeTable().get(linkTed.getTarget());
				if (node.getIpv4AddressLocalNode()!=null) {
					//System.out.println(node.toString());
					dst = "/nodes/node[id=" + domainID + "]/ports/port[id=" +node.getIpv4AddressLocalNode().getHostAddress()+"]";
					//port.setId(node.getIpv4AddressLocalNode().getHostAddress());
				}
				else {
					dst = "/nodes/node[id=" + domainID + "]/ports/port[id=" +String.valueOf(linkTed.getSource())+"]";
					//System.out.println("null");
				}
				//String dst = "/nodes/node[id=" + domainID + "]/ports/port[id=" + String.valueOf(linkTed.getTarget()) + "]";
				link.setDst(dst);
			}else {
				System.out.println("Type of link not implemented for UnifyModel");
			}
			LinkResource linkResource = new LinkResource();
			if (linkTed.getTE_info().getUndirLinkDelay() != null) linkResource.setDelay(String.valueOf(linkTed.getTE_info().getUndirLinkDelay().getDelay()));
			if (linkTed.getTE_info().getUndirAvailableBw() != null) linkResource.setBandwidth(String.valueOf(linkTed.getTE_info().getUndirAvailableBw().getAvailableBw()));
			if (linkResource != null) link.setResources(linkResource);
			linklist.add(link);
		}
		links.setLink(linklist);
	  
		return links;
	}




}

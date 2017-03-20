package es.tid.tedb;


import es.tid.bgp.bgp4.update.tlv.PCEv4ScopeTLV;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;


public interface DomainTEDB extends TEDB {
	
	public Inet4Address getDomainID();
	 
	
	public boolean belongsToDomain(Object addr);

	//public Object getDomainId();
	public ReachabilityEntry getReachabilityEntry();

	//public byte[]  getDomainReachabilityIPv4Prefix();

	public LinkedList<InterDomainEdge> getInterDomainLinks();
	public Set<IntraDomainEdge> getIntraDomainLinks();
	public String printInterDomainLinks();

	public boolean containsVertex(Object vertex);
	public WSONInformation getWSONinfo();
	public void setWSONinfo(WSONInformation wSONinfo);
	public SSONInformation getSSONinfo(); 
	public void setSSONinfo(SSONInformation sSONinfo);
	public void notifyWavelengthReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength, boolean bidirectional);
	public void notifyWavelengthReservationSSON (LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength, boolean bidirectional, int m);
	public void notifyWavelengthEndReservation(LinkedList<Object> sourceVertexList, LinkedList<Object> targetVertexList, int wavelength, boolean bidirectional);
	public void notifyWavelengthChange(Object localInterfaceIPAddress,Object remoteInterfaceIPAddress,BitmapLabelSet previousBitmapLabelSet,BitmapLabelSet newBitmapLabelSet );
	public void notifyNewEdgeIP(Object source, Object destination, TE_Information informationTEDB);
	public void register(TEDListener compAlgPreComp);
	public void registerSSON(SSONListener compAlgPreComp);
	
	public void notifyNewVertex(Object vertex);
	
	public void notifyNewEdge (Object source, Object destination);
	
	public void clearAllReservations();

	public void notifyWavelengthEndReservationSSON(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, int wavelength,
			boolean bidirectional, int m);
	public void notifyWavelengthReservationWLAN(
			LinkedList<Object> sourceVertexList,
			LinkedList<Object> targetVertexList, LinkedList<Integer> wlans,
			boolean bidirectional);


	public void createGraph();


	public Set<Object> getIntraDomainLinksvertexSet();


	public Hashtable<Object, Node_Info> getNodeTable();

	public IT_Resources getItResources();
	public void setItResources(IT_Resources itResources);

	public PCEInfo getMDPCE();

	public void setMDPCE(PCEInfo IP);

	public ArrayList<Inet4Address> getLocalDomains();

	public void setLocalDomains(ArrayList<Inet4Address> list);

	public ArrayList<Inet4Address> getLocalASs();

	public void setLocalASs(ArrayList<Inet4Address> list);

	public ArrayList<Inet4Address> getNeighDomains();

	public void setNeighDomains(ArrayList<Inet4Address> list);
	public ArrayList<Inet4Address> getNeighASs();

	public void setNeighASs(ArrayList<Inet4Address> list);

	public void setPCEScope(PCEv4ScopeTLV pcescope);
	public PCEv4ScopeTLV getPCEscope();

}






package es.tid.tedb;

import es.tid.of.DataPathID;

import java.net.Inet4Address;
import java.util.LinkedList;

public class Node_Info {

	//we specify we we learn the topology from

	private String learntFrom = null;
	// for the moment only ospf-non pseudo supported
	private  Inet4Address igp_ident = null;

	//caracterizacion
	private Inet4Address as_number, area_id, bgpls_ident, ipv4AddressLocalNode= null;
	private DataPathID dataPathIDLocalNode;
	private boolean overload_bit = false;
	private boolean attached_bit = false;
	private boolean external_bit = false;
	private boolean abr_bit = false;
	private byte[] name;
	private LinkedList <Inet4Address> ipv4areaIDs = new LinkedList <Inet4Address>();
	private int sid;
	private long ISIS_ISO_NODE_ID=0L;
	private int valid_len=0;



	public Node_Info() {
		// TODO Auto-generated constructor stub
	}

	public Node_Info(Inet4Address ipv4Address_ospf, Inet4Address as_number,
			boolean overload_bit, boolean attached_bit, boolean external_bit,
			boolean abr_bit, byte[] name, LinkedList<Inet4Address> ipv4areaIDs, Inet4Address ipv4AdddressLN) {
		super();
		this.igp_ident = ipv4Address_ospf;
		this.as_number = as_number;
		this.overload_bit = overload_bit;
		this.attached_bit = attached_bit;
		this.external_bit = external_bit;
		this.abr_bit = abr_bit;
		this.name = name;
		this.ipv4areaIDs = ipv4areaIDs;
		this.ipv4AddressLocalNode = ipv4AdddressLN;
	}

	public LinkedList<Inet4Address> getIpv4areaIDs() {
		return ipv4areaIDs;
	}

	public void setIpv4areaIDs(LinkedList<Inet4Address> ipv4areaIDs) {
		this.ipv4areaIDs = ipv4areaIDs;
	}

	public byte[] getName() {
		return name;
	}


	public void setName(byte[] name) {
		this.name = name;
	}

	public boolean isOverload_bit() {
		return overload_bit;
	}


	public void setOverload_bit(boolean overload_bit) {
		this.overload_bit = overload_bit;
	}


	public boolean isAttached_bit() {
		return attached_bit;
	}


	public void setAttached_bit(boolean attached_bit) {
		this.attached_bit = attached_bit;
	}


	public boolean isExternal_bit() {
		return external_bit;
	}


	public void setExternal_bit(boolean external_bit) {
		this.external_bit = external_bit;
	}


	public Inet4Address getArea_id() {
		return area_id;
	}

	public void setArea_id(Inet4Address area_id) {
		this.area_id = area_id;
	}

	public boolean isAbr_bit() {
		return abr_bit;
	}


	public void setAbr_bit(boolean abr_bit) {
		this.abr_bit = abr_bit;
	}


	public Inet4Address getIpv4Address() {
		return igp_ident;
	}


	public void setIpv4Address(Inet4Address ipv4Address_ospf) {
		this.igp_ident = ipv4Address_ospf;
	}


	public Inet4Address getIpv4AddressLocalNode() {
		return ipv4AddressLocalNode;
	}
	
	public void setIpv4AddressLocalNode(Inet4Address ipv4AddressLocalNode) {
		this.ipv4AddressLocalNode = ipv4AddressLocalNode;
	}

	public DataPathID getDataPathLocalNode() {
		return dataPathIDLocalNode;
	}
	
	public void setDataPathLocalNode(DataPathID dataPathLocalNode) {
		this.dataPathIDLocalNode = dataPathLocalNode;
	}
	
	public Inet4Address getAs_number() {
		return as_number;
	}


	public void setAs_number(Inet4Address as_number) {
		this.as_number = as_number;
	}

	public Inet4Address getBgpls_ident() {
		return bgpls_ident;
	}

	public void setBgpls_ident(Inet4Address bgpls_ident) {
		this.bgpls_ident = bgpls_ident;
	}

	public String getLearntFrom() {
		return learntFrom;
	}

	public void setLearntFrom(String learntFrom) {
		this.learntFrom = learntFrom;
	}

	public void setSID(int sid) {
		this.sid = sid;
	}

	public int getValid_len(){
		return valid_len;
	}

	public void setValid_len(int len) {
		this.valid_len = len;
	}

	public int getSid(){
		return sid;
	}
	public void setISISid(long id) {
		this.ISIS_ISO_NODE_ID = id;
	}

	public long getISISid(){
		return this.ISIS_ISO_NODE_ID;
	}

	public String toString(){
		StringBuffer sb=new StringBuffer(10000);;

		if(igp_ident!=null)
			sb.append("IGP-ID:" + this.getIpv4Address() +"\t");
		if(as_number!=null)
			sb.append("as_num:" + this.getAs_number() +"\t");
		if(bgpls_ident != null)
			sb.append("BGPLS-identifer:" + this.getBgpls_ident() +"\t");
		if(name != null)
			sb.append("Name :" + new String (this.getName()) +"\t");
		if(ipv4AddressLocalNode!=null)
			sb.append("Local Node IP Address: " + this.getIpv4AddressLocalNode() + "\t");
		if(this.getIpv4areaIDs()!=null) {
			if (this.getIpv4areaIDs().size()>0) {
				sb.append("ISIS Area ID: " + this.getIpv4areaIDs().get(0).getHostAddress() + "\t");
				sb.append("ISIS  len: " + String.valueOf(this.valid_len)+ "\t");
			}
		}
		if(sid!=0)
			sb.append("SID:" + this.getSid() +"\t");
		if(ISIS_ISO_NODE_ID!=0L)
			sb.append("IS-IS id:" + this.getISISid() +"\t");

		return sb.toString();

	}


}

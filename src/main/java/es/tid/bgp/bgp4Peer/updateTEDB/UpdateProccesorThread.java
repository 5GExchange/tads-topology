package es.tid.bgp.bgp4Peer.updateTEDB;

import es.tid.bgp.bgp4.messages.BGP4Update;
import es.tid.bgp.bgp4.update.fields.*;
import es.tid.bgp.bgp4.update.fields.pathAttributes.*;
import es.tid.bgp.bgp4.update.tlv.PCEv4DescriptorsTLV;
import es.tid.bgp.bgp4.update.tlv.PCEv4DomainTLV;
import es.tid.bgp.bgp4.update.tlv.PCEv4NeighboursTLV;
import es.tid.bgp.bgp4.update.tlv.PCEv4ScopeTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.*;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.*;
import es.tid.bgp.bgp4Peer.peer.DomainUpdateTime;
import es.tid.bgp.bgp4Peer.peer.MDPCEinfoUpdateTime;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.*;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import es.tid.tedb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

//import java.util.logging.Logger;

/**
 * This class process the update messages updating the TEDB.
 * 
 *  WARNING: it is suppose to be a SimpleTEDB!!! It is not finished yet.
 * @author pac
 *
 */
public class UpdateProccesorThread extends Thread {
	/**
	 * Parameter to run the class if it is true
	 */
	private boolean running;
	/**
	 * Queue which stores the BGP4 update messages to be read and process
	 */
	private LinkedBlockingQueue<BGP4Update> updateList;


	private ArrayList<AsInfo> asInfo;

	private Hashtable<String, ArrayList<AsInfo>> AsInfo_DB= new Hashtable<String, ArrayList<AsInfo>>();

	private Hashtable<DomainUpdateTime, Long> DomainUpdate;

	private Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate;


	private Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate;
	private Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate;
	private Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate;
	private Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate;




	Enumeration domains = AsInfo_DB.keys();



	/** LINK ATTRIBUTE TLVs */
	MaximumLinkBandwidthLinkAttribTLV maximumLinkBandwidthTLV;
	MaxReservableBandwidthLinkAttribTLV maxReservableBandwidthTLV;
	UnreservedBandwidthLinkAttribTLV unreservedBandwidthTLV;
	AdministrativeGroupLinkAttribTLV administrativeGroupTLV;
	LinkProtectionTypeLinkAttribTLV linkProtectionTLV;
	MetricLinkAttribTLV metricTLV;
	IPv4RouterIDLocalNodeLinkAttribTLV iPv4RouterIDLocalNodeLATLV;
	IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RouterIDRemoteNodeLATLV;
	DefaultTEMetricLinkAttribTLV TEMetricTLV;	
	TransceiverClassAndAppAttribTLV transceiverClassAndAppATLV;
	MF_OTPAttribTLV mF_OTP_ATLV;
	int linkDelay;
	int linkDelayVar;
	int minDelay;
	int maxDelay;
	int linkLoss;
	float residualBw;
	float availableBw;
	float utilizedBw;
	/** NODE ATTRIBUTE TLVs 
	 * Ipv4 of local node link attribute TLV also used
	 * 
	 * */
	NodeFlagBitsNodeAttribTLV nodeFlagBitsTLV = new NodeFlagBitsNodeAttribTLV();
	NodeNameNodeAttribTLV nodeNameTLV = new NodeNameNodeAttribTLV();
	IS_IS_AreaIdentifierNodeAttribTLV areaIDTLV = new IS_IS_AreaIdentifierNodeAttribTLV();
	SidLabelNodeAttribTLV sidTLV = new SidLabelNodeAttribTLV();

	/**PREFIX ATTRIBUTE TLVs */
	IGPFlagBitsPrefixAttribTLV igpFlagBitsTLV = new IGPFlagBitsPrefixAttribTLV();
	RouteTagPrefixAttribTLV routeTagTLV = new RouteTagPrefixAttribTLV();
	PrefixMetricPrefixAttribTLV prefixMetricTLV = new PrefixMetricPrefixAttribTLV();
	OSPFForwardingAddressPrefixAttribTLV OSPFForwardingAddrTLV = new OSPFForwardingAddressPrefixAttribTLV();


	//----AS PATH Attributes----//
	List<AS_Path_Segment> asPathSegments = new LinkedList<AS_Path_Segment>();

	private AvailableLabels availableLabels;
	/**
	 * Logger
	 */
	private Logger log;
	/**
	 * Topology database for interDomain Links which will be updated.
	 */
	private MultiDomainTEDB multiTedb;
	/**
	 * Topology database for intradomain Links. It owns several domains and.
	 */
	private Hashtable<String,TEDB> intraTEDBs;

	private LinkedList<UpdateLink> updateLinks;

	private TE_Information te_info;
	private ScheduledThreadPoolExecutor executor;


	public UpdateProccesorThread(LinkedBlockingQueue<BGP4Update> updateList,
								 MultiDomainTEDB multiTedb, Hashtable<String, TEDB> intraTEDBs, Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate, Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate, Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate, Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate, Hashtable<DomainUpdateTime, Long> domainUpdate)  {

			log = LoggerFactory.getLogger("BGP4Peer");

		this.executor=executor;
		running=true;
		this.updateList=updateList;
		this.multiTedb= multiTedb;
		this.intraTEDBs=intraTEDBs;
		this.availableLabels=new AvailableLabels();
		this.updateLinks=new LinkedList<UpdateLink>();
		this.DomainUpdate=domainUpdate;
		this.intraDomainLinkUpdate=intraDomainLinkUpdate;
		this.interDomainLinkUpdate= interDomainLinkUpdate;
		this.nodeITinfoUpdate=nodeITinfoUpdate;
		this.nodeinfoUpdate=nodeinfoUpdate;
		this.MDPCEinfoUpdate=MDPCEinfoUpdate;

	}

	/**
	 * Starts processing updates
	 */
	public void run(){	
		BGP4Update updateMsg;
		while (running) {
			try {
				clearAttributes();
				PathAttribute att_ls = null;
				PathAttribute att_aspath = null;
				PathAttribute att_localpref = null;
				PathAttribute att_mpreach = null;
				PathAttribute att = null;
				updateMsg= updateList.take();
				//log.info("Update Processor Thread Reading the Message: \n"+ updateMsg.toString());
				String learntFrom = updateMsg.getLearntFrom();
				log.info("Update Msg Received from "+learntFrom +"  Queue size: " +updateList.size());
				ArrayList<PathAttribute> pathAttributeList = updateMsg.getPathAttributes();
				ArrayList<PathAttribute> pathAttributeListUtil = new ArrayList<PathAttribute>();			

									// buscamos los dos atributos que nos interesan...
				for (int i=0;i<pathAttributeList.size();i++)
				{
					att = pathAttributeList.get(i);
					int typeCode = att.getTypeCode();
					switch (typeCode){
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_BGP_LS_ATTRIBUTE:
						att_ls = att;
						break;
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_MP_REACH_NLRI:
						att_mpreach = att;
						break;
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_ASPATH:
						//log.info("We don't use ASPATH");
						att_aspath= att;
						break;	
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_ORIGIN:
						//log.info("We don't use ORIGIN");
						break;
						case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_LOCAL_PREF:
							att_localpref= att;
							break;
						default:
						//log.info("Attribute typecode " + typeCode +"unknown");
						break;
					}
				}

									//los situamos en el orden correcto para nuestra beloved ted...
				if(att_ls!=null)
					pathAttributeListUtil.add(att_ls);
				if(att_mpreach!=null)
					pathAttributeListUtil.add(att_mpreach);
				if(att_localpref!=null)
					pathAttributeListUtil.add(att_localpref);
				if(att_aspath!=null)
					pathAttributeListUtil.add(att_aspath);




				if (pathAttributeListUtil != null){
					for (int i=0;i<pathAttributeListUtil.size();i++){
						att = pathAttributeListUtil.get(i);
						int typeCode = att.getTypeCode();
						switch (typeCode){	

							case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_LOCAL_PREF:
								processAttributeLocalPref((LOCAL_PREF_Attribute) att);
								continue;

							case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_BGP_LS_ATTRIBUTE:
							processAttributeLinkState((LinkStateAttribute) att);
							continue;

							case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_ASPATH:
								processAttributeAsPath((AS_Path_Attribute) att, learntFrom);
								continue;
						case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_MP_REACH_NLRI:
							int afi;
							afi = ((MP_Reach_Attribute)att).getAddressFamilyIdentifier();
							if (afi == AFICodes.AFI_BGP_LS){
								LinkStateNLRI nlri = (LinkStateNLRI) ((BGP_LS_MP_Reach_Attribute)att).getLsNLRI();
								int nlriType =  nlri.getNLRIType();
								switch (nlriType){					
								case NLRITypes.Link_NLRI:
									processLinkNLRI((LinkNLRI)(nlri), learntFrom);
									//log.debug("Link NLRI Learnt From: "+learntFrom +">-----<" +nlri.toString());
									continue;
								case NLRITypes.Node_NLRI:
									fillNodeInformation((NodeNLRI)(nlri), learntFrom);
									log.info("Node Information Learnt From: "+learntFrom +">-----<" +nlri.toString());
									continue;
								case NLRITypes.Prefix_v4_NLRI://POR HACER...
									fillPrefixNLRI((PrefixNLRI)nlri, igpFlagBitsTLV, OSPFForwardingAddrTLV, prefixMetricTLV, routeTagTLV);
									continue;
								case NLRITypes.IT_Node_NLRI:
									fillITNodeInformation((ITNodeNLRI)(nlri), learntFrom);
									//log.debug("IT Node Information Learnt From: "+learntFrom +">-----<" +nlri.toString());
									continue;
									case NLRITypes.PCE_NLRI:
										log.info("........................Received PCE_NLRI........................");
										fillMDPCEInformation((PCENLRI)(nlri), learntFrom);
										continue;
								default:
									log.debug("Attribute Code Unknown");
								}
							}
							continue;
						default:
							log.debug("Attribute Code Unknown");
						}
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}


	private void fillPrefixNLRI(PrefixNLRI nlri, IGPFlagBitsPrefixAttribTLV igpFlagBitsTLV, OSPFForwardingAddressPrefixAttribTLV oSPFForwardingAddrTLV, PrefixMetricPrefixAttribTLV prefixMetricTLV, RouteTagPrefixAttribTLV routeTagTLV) {// TODO Auto-generated method stub
	}

	/**
	 * Function which process the attribute link State. It updates the fields passed by argument. 
	 */



	private void processAttributeLocalPref(LOCAL_PREF_Attribute localprefAtt) {

		int localPref =0;
		localPref = localprefAtt.getValue();
		//log.info("Received local preference= "+ String.valueOf(localPref));

	}

	private void processAttributeAsPath(AS_Path_Attribute asPathAtt, String learntFrom) {
		int type=0;
		int numberOfSegments=0;
		asInfo = new ArrayList<AsInfo>();

		//private Hashtable<String, ArrayList<AsInfo>> AsInfo_DB= new Hashtable<String, ArrayList<AsInfo>>();
		//Enumeration domains = AsInfo_DB.keys();


if (AsInfo_DB.containsKey(learntFrom))
	AsInfo_DB.remove(learntFrom); // Remove previously Stored AS Path Information for this Domain
		for(int i = 0; i < asPathAtt.getAsPathSegments().size();  i++)
		asPathSegments.add(asPathAtt.getAsPathSegments().get(i));

		for(int i = 0; i < asPathSegments.size(); i++)
		{
			type= asPathSegments.get(i).getType(); //T---Ordered or Unordered
			numberOfSegments=asPathSegments.get(i).getNumberOfSegments(); //L--- Number of AS segments
			int[] temp_segments = new int [numberOfSegments];
			temp_segments= asPathSegments.get(i).getSegments(); //V---- Value of the Segment
			asInfo.add(new AsInfo(asPathSegments.get(i),type, numberOfSegments,temp_segments, learntFrom));

			//log.info("Learnt From: " +learntFrom   + "SegmentType:  " + asPathSegments.get(i).getType() + " SegmentNumber: " + asPathSegments.get(i).getNumberOfSegments() + " SegmentValue: " + asPathSegments.get(i).get4Segments());
		}
		AsInfo_DB.put(learntFrom,asInfo);
		//this.multiTedb.setASInfo_DB(AsInfo_DB);


							//-------------------Not completed Yet--------------------//
	}


	private void processAttributeLinkState(LinkStateAttribute lsAtt){

		if (lsAtt.getMaximumLinkBandwidthTLV() != null){
			maximumLinkBandwidthTLV = lsAtt.getMaximumLinkBandwidthTLV();
		}

		if (lsAtt.getMaxReservableBandwidthTLV() != null){
			maxReservableBandwidthTLV = lsAtt.getMaxReservableBandwidthTLV();
		}
		if (lsAtt.getUnreservedBandwidthTLV() != null){
			unreservedBandwidthTLV = lsAtt.getUnreservedBandwidthTLV();
		}
		if(lsAtt.getAdministrativeGroupTLV() != null){
			administrativeGroupTLV = lsAtt.getAdministrativeGroupTLV();
		}
		if(lsAtt.getLinkProtectionTLV() != null){
			linkProtectionTLV = lsAtt.getLinkProtectionTLV();
		}
		if(lsAtt.getIPv4RouterIDLocalNodeLATLV()!= null){
			iPv4RouterIDLocalNodeLATLV = lsAtt.getIPv4RouterIDLocalNodeLATLV();
		}
		if(lsAtt.getIPv4RouterIDRemoteNodeLATLV()!=null){
			iPv4RouterIDRemoteNodeLATLV = lsAtt.getIPv4RouterIDRemoteNodeLATLV();
		}
		if(lsAtt.getMetricTLV() != null){
			metricTLV = lsAtt.getMetricTLV();
		}
		if(lsAtt.getTEMetricTLV()!=null){
			TEMetricTLV = lsAtt.getTEMetricTLV();
		}
		if(lsAtt.getNodeFlagBitsTLV()!= null){
			nodeFlagBitsTLV = lsAtt.getNodeFlagBitsTLV();
		}
		if(lsAtt.getNodeNameTLV() != null){
			nodeNameTLV = lsAtt.getNodeNameTLV();
		}
		if(lsAtt.getAreaIDTLV() != null){
			areaIDTLV = lsAtt.getAreaIDTLV();
		}
		if(lsAtt.getIgpFlagBitsTLV() != null){
			igpFlagBitsTLV= lsAtt.getIgpFlagBitsTLV();
		}
		if(lsAtt.getRouteTagTLV() != null){
			routeTagTLV = lsAtt.getRouteTagTLV();
		}
		if(lsAtt.getOSPFForwardingAddrTLV() != null){
			OSPFForwardingAddrTLV = lsAtt.getOSPFForwardingAddrTLV();
		}
		if(lsAtt.getSidLabelTLV()!=null){
			sidTLV = lsAtt.getSidLabelTLV();
		}

		if (lsAtt.getAvailableLabels() != null){
			this.availableLabels =lsAtt.getAvailableLabels();
		}
		if (lsAtt.getMF_OTP() != null){
			this.mF_OTP_ATLV =lsAtt.getMF_OTP();
		}

		if (lsAtt.getTransceiverClassAndApp() != null){
			this.transceiverClassAndAppATLV =lsAtt.getTransceiverClassAndApp();
		}

		//New TE metrics
		if(lsAtt.getUndirectionalLinkDelayTLV()!=null){
			linkDelay = lsAtt.getUndirectionalLinkDelayTLV().getDelay();
		}
		if(lsAtt.getUndirectionalDelayVariationTLV()!=null){
			linkDelayVar = lsAtt.getUndirectionalDelayVariationTLV().getDelayVar();
		}
		if(lsAtt.getMinMaxUndirectionalLinkDelayTLV()!=null){
			maxDelay = lsAtt.getMinMaxUndirectionalLinkDelayTLV().getHighDelay();
			minDelay = lsAtt.getMinMaxUndirectionalLinkDelayTLV().getLowDelay();
		}
		if(lsAtt.getUndirectionalLinkLossTLV()!=null){
			linkLoss = lsAtt.getUndirectionalLinkLossTLV().getLinkLoss();
		}
		if(lsAtt.getUndirectionalResidualBwTLV()!=null){
			residualBw = lsAtt.getUndirectionalResidualBwTLV().getResidualBw();
		}
		if(lsAtt.getUndirectionalAvailableBwTLV()!=null){
			availableBw = lsAtt.getUndirectionalAvailableBwTLV().getAvailableBw();
		}
		if(lsAtt.getUndirectionalUtilizedBwTLV()!=null){
			utilizedBw = lsAtt.getUndirectionalUtilizedBwTLV().getUtilizedBw();
		}
	}
	/**
	 * Function which process the link NLRI. It updates the fields passed by argument.
	 * @param linkNLRI
	 * @param maximumLinkBandwidthTLV
	 * @param maxReservableBandwidthTLV
	 * @param unreservedBandwidthTLV
	 * @param availableLabels
	 */


	/** Procesar un link significa:
	 * crear los vertices si no existen ya
	 * crear la edge si no existe ya
	 * crear la te_info o actualizarla
	 * @param linkNLRI
	 * @param learntFrom 
	 */
	private void processLinkNLRI(LinkNLRI linkNLRI, String learntFrom){
		ArrayList<NodeDescriptorsSubTLV> nodeDescriptorsSubTLV;
		//Domains
		Inet4Address localDomainID= null ;
		Inet4Address remoteDomainID = null ;
		int IGP_type;
		int localISISid=0;
		int remoteISISid=0;
		Inet4Address areaID= null ;
		Inet4Address bgplsID = null;

		Inet4Address LocalNodeIGPId = null;
		Inet4Address RemoteNodeIGPId = null;

		IGP_type = linkNLRI.getLocalNodeDescriptors().getIGPRouterID().getIGP_router_id_type();

		//Local Node Descriptors
		if (linkNLRI.getLocalNodeDescriptors().getAutonomousSystemSubTLV()!=null){
			localDomainID=linkNLRI.getLocalNodeDescriptors().getAutonomousSystemSubTLV().getAS_ID();
		}
		if (linkNLRI.getLocalNodeDescriptors().getAreaID()!=null) {
			areaID=linkNLRI.getLocalNodeDescriptors().getAreaID().getAREA_ID();
		}
		if (linkNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV()!=null) {
			bgplsID=linkNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV().getBGPLS_ID();
		}

		if (IGP_type==3){
			if (linkNLRI.getLocalNodeDescriptors().getIGPRouterID()!=null){
				LocalNodeIGPId=linkNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF();
			}
		}
		if (IGP_type==1) {
			if (linkNLRI.getLocalNodeDescriptors().getIGPRouterID() != null) {
				localISISid = linkNLRI.getLocalNodeDescriptors().getIGPRouterID().getISIS_ISO_NODE_ID();
			}
		}
		if (linkNLRI.getRemoteNodeDescriptorsTLV().getAutonomousSystemSubTLV() != null) {
			remoteDomainID = linkNLRI.getRemoteNodeDescriptorsTLV().getAutonomousSystemSubTLV().getAS_ID();
		}
		if (linkNLRI.getRemoteNodeDescriptorsTLV().getAreaID() != null) {
			areaID = linkNLRI.getRemoteNodeDescriptorsTLV().getAreaID().getAREA_ID();
		}
		if (linkNLRI.getRemoteNodeDescriptorsTLV().getBGPLSIDSubTLV() != null) {
			bgplsID = linkNLRI.getRemoteNodeDescriptorsTLV().getBGPLSIDSubTLV().getBGPLS_ID();
		}
		if (IGP_type == 3) {
			if (linkNLRI.getRemoteNodeDescriptorsTLV().getIGPRouterID() != null) {
				RemoteNodeIGPId = linkNLRI.getRemoteNodeDescriptorsTLV().getIGPRouterID().getIpv4AddressOSPF();
			}
		}
		if (IGP_type == 1) {
			if (linkNLRI.getRemoteNodeDescriptorsTLV().getIGPRouterID() != null) {
				remoteISISid = linkNLRI.getRemoteNodeDescriptorsTLV().getIGPRouterID().getISIS_ISO_NODE_ID();
			}
		}
		if (linkNLRI.getUndirectionalLinkDelayTLV() != null) {
			linkDelay = linkNLRI.getUndirectionalLinkDelayTLV().getDelay();
		}
		if (linkNLRI.getUndirectionalDelayVariationTLV() != null) {
			linkDelayVar = linkNLRI.getUndirectionalDelayVariationTLV().getDelayVar();
		}
		if (linkNLRI.getMinMaxUndirectionalLinkDelayTLV() != null) {
			maxDelay = linkNLRI.getMinMaxUndirectionalLinkDelayTLV().getHighDelay();
			minDelay = linkNLRI.getMinMaxUndirectionalLinkDelayTLV().getLowDelay();
		}
		if (linkNLRI.getUndirectionalLinkLossTLV() != null) {
			linkLoss = linkNLRI.getUndirectionalLinkLossTLV().getLinkLoss();
		}
		if (linkNLRI.getUndirectionalResidualBwTLV() != null) {
			residualBw = linkNLRI.getUndirectionalResidualBwTLV().getResidualBw();
		}
		if (linkNLRI.getUndirectionalAvailableBwTLV() != null) {
			availableBw = linkNLRI.getUndirectionalAvailableBwTLV().getAvailableBw();
		}
		if (linkNLRI.getUndirectionalUtilizedBwTLV() != null) {
			utilizedBw = linkNLRI.getUndirectionalUtilizedBwTLV().getUtilizedBw();
		}
		/**Creamos el grafo*/
		//Let's see if our link is intradomain or interdomain...
		//log.info("as_local "+localDomainID);
		//log.info("as_remote "+remoteDomainID);

		if (localDomainID.equals(remoteDomainID)) {
			log.debug("........IntraDomain......for domain:  " + localDomainID.getCanonicalHostName());
			IntraDomainEdge intraEdge = null;
			if (IGP_type == 3) {
				log.debug("LocalIP:  " + LocalNodeIGPId + "   RemoteIP:  " + RemoteNodeIGPId);
			}
			else if (IGP_type == 1){
				log.debug("LocalISISid:  " + localISISid + "   RemoteISISid:  " + remoteISISid);
			}

			DomainTEDB domainTEDB=(DomainTEDB)intraTEDBs.get(localDomainID.getHostAddress());
			SimpleTEDB simpleTEDBxx=null;
			if (domainTEDB instanceof SimpleTEDB){
				simpleTEDBxx = (SimpleTEDB) domainTEDB;
			}else if (domainTEDB==null){
				simpleTEDBxx = new SimpleTEDB();
				simpleTEDBxx.createGraph();
				simpleTEDBxx.setDomainID(localDomainID);
				this.intraTEDBs.put(localDomainID.getHostAddress(), simpleTEDBxx);
			}else {
				log.debug("PROBLEM: TEDB not Compatible");
				return;
			}
			if (IGP_type == 3) {
				if(simpleTEDBxx.getNetworkGraph().containsEdge(LocalNodeIGPId, RemoteNodeIGPId)) {
					intraEdge = simpleTEDBxx.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId);
					log.debug("IntraDomain Edge Already Exist in the TEDB");
				}
				else {
						intraEdge = new IntraDomainEdge();
						log.debug("Graph does not contain IntraDomain Edge");
				}
			}
			if (IGP_type == 1) {
				simpleTEDBxx.setIGPType(1);
				if(simpleTEDBxx.getNetworkGraph().containsEdge(localISISid, remoteISISid)) {
					intraEdge = simpleTEDBxx.getNetworkGraph().getEdge(localISISid, remoteISISid);
					log.debug("IntraDomain Edge Already Exist in the TEDB");
				}
				else {
					intraEdge = new IntraDomainEdge();
					log.debug("Graph does not contain IntraDomain Edge");
				}
			}

			if (linkNLRI.getLinkIdentifiersTLV() != null) {
				intraEdge.setSrc_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier());
				intraEdge.setDst_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier());
			}
			if(intraEdge.getLearntFrom()==null || intraEdge.getLearntFrom().equals(learntFrom)) {
				log.debug("Existing IntraDomain Edge LearntFrom: " + intraEdge.getLearntFrom() + "  New LearntFrom:  " + learntFrom);
				te_info = createTE_Info(simpleTEDBxx);
				intraEdge.setTE_info(te_info);
				intraEdge.setLearntFrom(learntFrom);
				if (IGP_type == 3) {
					setIntraDomainEdgeUpdateTime (localDomainID, LocalNodeIGPId,RemoteNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(),linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(),System.currentTimeMillis());
				}
				if (IGP_type == 1) {
					//setIntraDomainEdgeUpdateTime (localDomainID, localISISid,remoteISISid, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(),linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(),System.currentTimeMillis());
				}

				//log.info(" After IntraDomian Edge LearntFrom: " +intraEdge.getLearntFrom());



				/*Adding Local and Remote Nodes to TED*/
				//OSPF IGP
				if (IGP_type == 3) {
					if (!(simpleTEDBxx.getNetworkGraph().containsVertex(LocalNodeIGPId))) {
					simpleTEDBxx.getNetworkGraph().addVertex(LocalNodeIGPId);//add vertex ya comprueba si existe el nodo en la ted-->se puede hacer mas limpio
					simpleTEDBxx.notifyNewVertex(LocalNodeIGPId);
					//log.info("Source Vertex :" +simpleTEDBxx.getNetworkGraph().containsVertex(LocalNodeIGPId) +"is Just Added");

					}
					//			else{
					//				log.info("Local Vertex: "+LocalNodeIGPId.toString() +" already present in TED...");
					//			}

					if (!(simpleTEDBxx.getNetworkGraph().containsVertex(RemoteNodeIGPId))) {
						//log.info("Not containing dst vertex");
						simpleTEDBxx.getNetworkGraph().addVertex(RemoteNodeIGPId);
						simpleTEDBxx.notifyNewVertex(RemoteNodeIGPId);
						//log.info("Destination Vertex :" +simpleTEDBxx.getNetworkGraph().containsVertex(RemoteNodeIGPId) +"is Just Added");

					}
					//			else {
					//				log.info("Remote Vertex: "+RemoteNodeIGPId.toString() +" already present in TED...");
					//			}

					if (!(simpleTEDBxx.getNetworkGraph().containsEdge(LocalNodeIGPId, RemoteNodeIGPId))) {
						log.debug("Graph does not contain intra-edge");
						//log.info("Adding information of local node to edge..." + simpleTEDBxx.getNodeTable().get(LocalNodeIGPId));
						intraEdge.setLocal_Node_Info(simpleTEDBxx.getNodeTable().get(LocalNodeIGPId));
						//log.info("Adding information of remote node to edge..." + simpleTEDBxx.getNodeTable().get(RemoteNodeIGPId));
						intraEdge.setRemote_Node_Info(simpleTEDBxx.getNodeTable().get(RemoteNodeIGPId));
						//log.info("Adding Edge from Origin Vertex" + LocalNodeIGPId.toString() + " to Destination Vertex" + RemoteNodeIGPId.toString());
						simpleTEDBxx.getNetworkGraph().addEdge(LocalNodeIGPId, RemoteNodeIGPId, intraEdge);
						simpleTEDBxx.notifyNewEdge(LocalNodeIGPId, RemoteNodeIGPId);
						simpleTEDBxx.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId).setNumberFibers(1);
						IntraDomainEdge edge = simpleTEDBxx.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId);
						if (intraEdge.getTE_info().getAvailableLabels() != null)
							((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).initializeReservation(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
					} else {
						log.debug("Graph contains Intra-edge");
						IntraDomainEdge edge;
						edge = simpleTEDBxx.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId);
						if (this.availableLabels != null) {
							if (((BitmapLabelSet) this.availableLabels.getLabelSet()).getDwdmWavelengthLabel() != null) {
								((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());

								if (((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved() != null) {
									((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyReservedBytesBitMap(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved());
								}
							}
						}
					}
				}
				//ISIS IGP
				if (IGP_type == 1) {
					if (!(simpleTEDBxx.getNetworkGraph().containsVertex(localISISid))) {
						simpleTEDBxx.getNetworkGraph().addVertex(localISISid);//add vertex ya comprueba si existe el nodo en la ted-->se puede hacer mas limpio
						simpleTEDBxx.notifyNewVertex(localISISid);
						//log.info("Source Vertex :" +simpleTEDBxx.getNetworkGraph().containsVertex(LocalNodeIGPId) +"is Just Added");

					}
					//			else{
					//				log.info("Local Vertex: "+LocalNodeIGPId.toString() +" already present in TED...");
					//			}

					if (!(simpleTEDBxx.getNetworkGraph().containsVertex(remoteISISid))) {
						//log.info("Not containing dst vertex");
						simpleTEDBxx.getNetworkGraph().addVertex(remoteISISid);
						simpleTEDBxx.notifyNewVertex(remoteISISid);
						//log.info("Destination Vertex :" +simpleTEDBxx.getNetworkGraph().containsVertex(remoteISISid) +"is Just Added");

					}
					//			else {
					//				log.info("Remote Vertex: "+remoteISISid.toString() +" already present in TED...");
					//			}

					if (!(simpleTEDBxx.getNetworkGraph().containsEdge(localISISid, remoteISISid))) {
						log.debug("Graph does not contain intra-edge");
						//log.info("Adding information of local node to edge..." + simpleTEDBxx.getNodeTable().get(LocalNodeIGPId));
						intraEdge.setLocal_Node_Info(simpleTEDBxx.getNodeTable().get(localISISid));
						//log.info("Adding information of remote node to edge..." + simpleTEDBxx.getNodeTable().get(RemoteNodeIGPId));
						intraEdge.setRemote_Node_Info(simpleTEDBxx.getNodeTable().get(remoteISISid));
						//log.info("Adding Edge from Origin Vertex" + LocalNodeIGPId.toString() + " to Destination Vertex" + RemoteNodeIGPId.toString());
						//temporary commented Andrea ISIS
						// simpleTEDBxx.getNetworkGraph().addEdge(localISISid, remoteISISid, intraEdge);
						simpleTEDBxx.notifyNewEdge(localISISid, remoteISISid);
						simpleTEDBxx.getNetworkGraph().getEdge(localISISid, remoteISISid).setNumberFibers(1);
						IntraDomainEdge edge = simpleTEDBxx.getNetworkGraph().getEdge(localISISid, remoteISISid);
						if (intraEdge.getTE_info().getAvailableLabels() != null)
							((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).initializeReservation(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
					} else {
						log.debug("Graph contains Intra-edge");
						IntraDomainEdge edge;
						edge = simpleTEDBxx.getNetworkGraph().getEdge(localISISid, remoteISISid);
						if (this.availableLabels != null) {
							if (((BitmapLabelSet) this.availableLabels.getLabelSet()).getDwdmWavelengthLabel() != null) {
								((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());

								if (((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved() != null) {
									((BitmapLabelSet) edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyReservedBytesBitMap(((BitmapLabelSet) intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved());
								}
							}
						}
					}
				}
			}
			/*
			Enumeration el3 = intraTEDBs.keys();
			while (el3.hasMoreElements()) {
				String key = (String) el3.nextElement();
				log.info("Intra-before.....the key is: "+key);
			}
			*/
		}

		else {
			log.debug(".........InterDomain Link..........");
			log.debug("Source: " + LocalNodeIGPId + "  Destination:  " + RemoteNodeIGPId);
			InterDomainEdge interEdge = null;

			//check Source Domain
			DomainTEDB domain = (DomainTEDB) intraTEDBs.get(localDomainID.getHostAddress());
			SimpleTEDB simpleTEDBs = null;
			if (domain instanceof SimpleTEDB) {
				//log.info("is instance sssss");
				simpleTEDBs = (SimpleTEDB) domain;
			} else if (domain == null) {
				//log.info("nullssss");
				simpleTEDBs = new SimpleTEDB();
				simpleTEDBs.createGraph();
				simpleTEDBs.setDomainID(localDomainID);
				this.intraTEDBs.put(localDomainID.getHostAddress(), simpleTEDBs);
			} else {
				log.debug("Problem: TEDBs Not Compatible");
				return;
			}

			//Check Destination Domain
			DomainTEDB domaind = (DomainTEDB) intraTEDBs.get(remoteDomainID.getHostAddress());
			SimpleTEDB simpleTEDBd = null;
			if (domaind instanceof SimpleTEDB) {
				//log.info("is instancedddddd");
				simpleTEDBd = (SimpleTEDB) domaind;
			} else if (domaind == null) {
				//log.info("nulldddd");
				simpleTEDBd = new SimpleTEDB();
				simpleTEDBd.createGraph();
				simpleTEDBd.setDomainID(remoteDomainID);
				this.intraTEDBs.put(remoteDomainID.getHostAddress(), simpleTEDBd);

			} else {
				log.debug("Problem: TEDBs Not Compatible");
				return;
			}


			if (multiTedb.getNetworkDomainGraph().containsVertex(localDomainID) && multiTedb.getNetworkDomainGraph().containsVertex(remoteDomainID)) {

				Set<InterDomainEdge> edgeset = multiTedb.getNetworkDomainGraph().edgesOf(localDomainID);
				Iterator<InterDomainEdge> iterador = edgeset.iterator();
				if (edgeset.size() == 0)
					log.debug("InterDomain Edge Set Size = 0");
				while (iterador.hasNext()) {
					InterDomainEdge interDomainEdge = iterador.next();
					log.debug("Existing Edge: " + interDomainEdge.toString());
					if (interDomainEdge.getSrc_router_id().equals(LocalNodeIGPId)) {
						//log.info("Local Router is the same!!!");
						if (interDomainEdge.getDst_router_id().equals(RemoteNodeIGPId)) {
							//log.info("Destination Router is the same!!!");
							interEdge = interDomainEdge;
						} else {
							log.debug("Destination Router is Different!!!");
						}
					} else {
						log.debug("Local router is Different!!!");
					}
				}
			}

			if (interEdge == null) {
				interEdge = new InterDomainEdge();
				log.debug("New Inter-Domain Edge");
				if (linkNLRI.getLinkIdentifiersTLV() != null) {
					interEdge.setSrc_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier());
					interEdge.setDst_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier());
				}
				interEdge.setSrc_router_id(LocalNodeIGPId);
				interEdge.setDst_router_id(RemoteNodeIGPId);
				interEdge.setDomain_dst_router(remoteDomainID);
				interEdge.setDomain_src_router(localDomainID);
				//log.info("Src if id: " + interEdge.getSrc_if_id() + "  Dst if id:  " + interEdge.getDst_if_id());
				//log.info("Src Router id: " + interEdge.getSrc_router_id() + "  Dst Router id:  " + interEdge.getDst_router_id());
				//log.info("Domain Src Router: " + interEdge.getDomain_src_router() + "  Domain Dst Router:  " + interEdge.getDomain_dst_router());
			}


			if (interEdge.getLearntFrom() == null || interEdge.getLearntFrom().equals(learntFrom)) {
				log.debug("Existing InterDomain Edge LearntFrom: " + interEdge.getLearntFrom() + "  New LearntFrom:  " + learntFrom);
				DomainTEDB simpleTEDB = new SimpleTEDB();
				te_info = createTE_Info(simpleTEDB);
				interEdge.setTE_info(te_info);
				interEdge.setLearntFrom(learntFrom);
				setInterDomainEdgeUpdateTime (localDomainID,LocalNodeIGPId,linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(), remoteDomainID, RemoteNodeIGPId,linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(),System.currentTimeMillis());
				//log.info("Checking new LearntFrom: " + interEdge.getLearntFrom());
				//FIXME: ADD I-D links to the Simple TEDBs
				/**
				 if(simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId) == null){
				 simpleTEDB.getInterDomainLinks().add(interEdge);
				 InterDomainEdge edge = simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId);
				 ((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).initializeReservation(((BitmapLabelSet)interEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
				 }
				 */
				multiTedb.addInterdomainLink(localDomainID, LocalNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(), remoteDomainID, RemoteNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(), te_info);
			}
		}
	}
	private TE_Information createTE_Info(DomainTEDB domainTEDB){
		TE_Information te_info = new TE_Information();
		if(linkDelay>0){
			UndirectionalLinkDelayDescriptorSubTLV uSTLV = new UndirectionalLinkDelayDescriptorSubTLV();
			uSTLV.setDelay(linkDelay);
			te_info.setUndirLinkDelay(uSTLV);
		}
		if(linkDelayVar>0){
			UndirectionalDelayVariationDescriptorSubTLV uSTLV = new UndirectionalDelayVariationDescriptorSubTLV();
			uSTLV.setDelayVar(linkDelayVar);
			te_info.setUndirDelayVar(uSTLV);
		}
		if(minDelay>0 && maxDelay>0){
			MinMaxUndirectionalLinkDelayDescriptorSubTLV uSTLV = new MinMaxUndirectionalLinkDelayDescriptorSubTLV();
			uSTLV.setHighDelay(maxDelay);
			uSTLV.setLowDelay(minDelay);
			te_info.setMinMaxUndirLinkDelay(uSTLV);
		}
		if(linkLoss>0){
			UndirectionalLinkLossDescriptorSubTLV uSTLV = new UndirectionalLinkLossDescriptorSubTLV();
			uSTLV.setLinkLoss(linkLoss);
			te_info.setUndirLinkLoss(uSTLV);
		}
		if(residualBw>0){
			UndirectionalResidualBandwidthDescriptorSubTLV uSTLV = new UndirectionalResidualBandwidthDescriptorSubTLV();
			uSTLV.setResidualBw(residualBw);
			te_info.setUndirResidualBw(uSTLV);
		}
		if(availableBw>0){
			UndirectionalAvailableBandwidthDescriptorSubTLV uSTLV = new UndirectionalAvailableBandwidthDescriptorSubTLV();
			uSTLV.setAvailableBw(availableBw);
			te_info.setUndirAvailableBw(uSTLV);
		}
		if(utilizedBw>0){
			UndirectionalUtilizedBandwidthDescriptorSubTLV uSTLV = new UndirectionalUtilizedBandwidthDescriptorSubTLV();
			uSTLV.setUtilizedBw(utilizedBw);
			te_info.setUndirUtilizedBw(uSTLV);
		}
		if (maximumLinkBandwidthTLV!=null){
			MaximumBandwidth maximumBandwidth = new MaximumBandwidth();
			maximumBandwidth.setMaximumBandwidth(maximumLinkBandwidthTLV.getMaximumBandwidth());
			te_info.setMaximumBandwidth(maximumBandwidth);
		}
		if (maxReservableBandwidthTLV!=null){
			MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
			maximumReservableBandwidth.setMaximumReservableBandwidth(maxReservableBandwidthTLV.getMaximumReservableBandwidth());
			te_info.setMaximumReservableBandwidth(maximumReservableBandwidth);
		}
		if (unreservedBandwidthTLV!=null){
			UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
			unreservedBandwidth.setUnreservedBandwidth(unreservedBandwidthTLV.getUnreservedBandwidth());
			te_info.setUnreservedBandwidth(unreservedBandwidth);
		}
		if(iPv4RouterIDLocalNodeLATLV!=null){
			IPv4RouterIDLocalNodeLinkAttribTLV iPv4RouterIDLocalNode = new IPv4RouterIDLocalNodeLinkAttribTLV();
			iPv4RouterIDLocalNode.setIpv4Address(iPv4RouterIDLocalNodeLATLV.getIpv4Address());
			te_info.setiPv4LocalNode(iPv4RouterIDLocalNode);
		}
		if(iPv4RouterIDRemoteNodeLATLV!=null){
			IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RouterIDRemoteNode = new IPv4RouterIDRemoteNodeLinkAttribTLV();
			iPv4RouterIDRemoteNode.setIpv4Address(iPv4RouterIDRemoteNodeLATLV.getIpv4Address());
			te_info.setiPv4RemoteNode(iPv4RouterIDRemoteNode);
		}
		if(metricTLV!=null){
			MetricLinkAttribTLV metric = new MetricLinkAttribTLV();
			metric.setMetric(metricTLV.getMetric());
			te_info.setMetric(metric);
		}
		if(TEMetricTLV!=null){
			TrafficEngineeringMetric teMetric = new TrafficEngineeringMetric();
			teMetric.setLinkMetric((long)TEMetricTLV.getLinkMetric());
			te_info.setTrafficEngineeringMetric(teMetric);
		}
		if(administrativeGroupTLV!=null){
			AdministrativeGroup adminGroup = new AdministrativeGroup();
			adminGroup.setAdministrativeGroup(administrativeGroupTLV.getAdministrativeGroup());
			te_info.setAdministrativeGroup(adminGroup);
		}
		if(linkProtectionTLV!=null){
			LinkProtectionTypeLinkAttribTLV linkProtection = new LinkProtectionTypeLinkAttribTLV();
			linkProtection.setProtection_type(linkProtectionTLV.getProtection_type());
			te_info.setLinkProtectionBGPLS(linkProtection);
		}
		if(this.mF_OTP_ATLV!=null){
			MF_OTPAttribTLV mF_OTP_ATLV = this.mF_OTP_ATLV.duplicate();
			te_info.setMfOTF(mF_OTP_ATLV);
		}

		if(this.transceiverClassAndAppATLV!=null){
			TransceiverClassAndAppAttribTLV tap = new TransceiverClassAndAppAttribTLV();
			tap.setTrans_class(transceiverClassAndAppATLV.getTrans_class());
			tap.setTrans_app_code(transceiverClassAndAppATLV.getTrans_app_code());	
			te_info.setTrans(tap);
		}

		if (availableLabels!= null){
			if(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel()!=null){
				if(domainTEDB.getSSONinfo()==null){
					//log.debug("NEW SSON INFO");
					SSONInformation ssonInfo = new SSONInformation();
					ssonInfo.setCs(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getChannelSpacing());
					ssonInfo.setGrid(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getGrid());
					ssonInfo.setNumLambdas(((BitmapLabelSet)this.availableLabels.getLabelSet()).getNumLabels());
					ssonInfo.setCommonAvailableLabels(this.availableLabels.dublicate());
					ssonInfo.setnMin(0);
					domainTEDB.setSSONinfo(ssonInfo);
				}
				if(domainTEDB.getWSONinfo()==null){
					//log.debug("NEW WSON INFO");
					WSONInformation wsonInfo = new WSONInformation();
					wsonInfo.setCs(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getChannelSpacing());
					wsonInfo.setGrid(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getGrid());
					wsonInfo.setNumLambdas(((BitmapLabelSet)this.availableLabels.getLabelSet()).getNumLabels());
					wsonInfo.setCommonAvailableLabels(this.availableLabels.dublicate());
					wsonInfo.setnMin(0);
					domainTEDB.setWSONinfo(wsonInfo);
				}
			}
			te_info.setAvailableLabels(availableLabels.dublicate());
		}
		return te_info;
	}

	private void fillMDPCEInformation(PCENLRI pceNLRI, String learntFrom){
/*		try {
			Thread.sleep(2000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	*/
		DomainTEDB domainTEDB= null;
		PCEInfo MDPCE= new PCEInfo();
		PCEv4ScopeTLV pceScope= new PCEv4ScopeTLV();
		Inet4Address PCEip = null;
		Inet4Address domainID = null;
		SimpleTEDB simpleTEDB=null;
		int preR=0;
		int preL=0;
		int preS=0;
		int preY=0;
		ArrayList<Inet4Address> localDomains = new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> localASs = new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> NeighDomains = new ArrayList<Inet4Address>();
		ArrayList<Inet4Address> NeighASs = new ArrayList<Inet4Address>();
		StringBuffer sb=new StringBuffer(1000);



		if (pceNLRI.getPCEv4ScopeTLV()!=null){
			pceScope=pceNLRI.getPCEv4ScopeTLV();
			preR= pceScope.getPre_R();
			preL= pceScope.getPre_L();
			preS= pceScope.getPre_S();
			preY= pceScope.getPre_Y();
			log.info("PCE Scope [PreR:" +preR +"  PreL:" +preL + "  PreS:" +preS + "  PreY:" +preY +" ]");
		}

		if (pceNLRI.getPCEv4DomainID()!=null){
			PCEv4DomainTLV domTLV= pceNLRI.getPCEv4DomainID();
			//ArrayList<AreaIDNodeDescriptorSubTLV> arealist = ;
			for (AreaIDNodeDescriptorSubTLV area: domTLV.getAreaIDSubTLVs()){
				log.info("Area ID Received: "+area.getAREA_ID().getHostAddress());
				if (!localDomains.contains(area.getAREA_ID())){
					log.info("Not Present, Added");
					sb.append("Local Area: "+area.toString());
					localDomains.add(area.getAREA_ID());
				}
			}
			for (AutonomousSystemNodeDescriptorSubTLV as: domTLV.getASSubTLVs()){
				if (!localASs.contains(as.getAS_ID())){
					localASs.add(as.getAS_ID());
					sb.append("Local AS: " +as.toString());
				}
			}
		}

		if (pceNLRI.getPCEv4NeighbourID()!=null){
			PCEv4NeighboursTLV NdomTLV= pceNLRI.getPCEv4NeighbourID();
			//ArrayList<AreaIDNodeDescriptorSubTLV> arealist = ;
			for (AreaIDNodeDescriptorSubTLV area: NdomTLV.getAreaIDSubTLVs()){
				if (!NeighDomains.contains(area.getAREA_ID())){
					NeighDomains.add(area.getAREA_ID());
				}
			}
			for (AutonomousSystemNodeDescriptorSubTLV as: NdomTLV.getASSubTLVs()){
				if (!NeighASs.contains(as.getAS_ID())){
					NeighASs.add(as.getAS_ID());
				}
			}

		}

		if (pceNLRI.getPCEv4Descriptors()!=null){
			PCEv4DescriptorsTLV pceTLV= pceNLRI.getPCEv4Descriptors();
			PCEip = pceTLV.getPCEv4Address();
			log.info("   PCE IP  :   "+PCEip);
			MDPCE.setPCEipv4(PCEip);
		}


		for (Inet4Address domain: localDomains){
			domainTEDB=(DomainTEDB)intraTEDBs.get(domain.getHostAddress());
			if (domainTEDB instanceof SimpleTEDB) {
				simpleTEDB = (SimpleTEDB) domainTEDB;
				if (simpleTEDB.getMDPCE() != null)
					MDPCE = simpleTEDB.getMDPCE();
				if(simpleTEDB.getMDPCE().getLearntFrom()==null || simpleTEDB.getMDPCE().getLearntFrom().equals(learntFrom))
				{
					simpleTEDB.setMDPCE(MDPCE);
					simpleTEDB.getMDPCE().setLearntFrom(learntFrom);
					simpleTEDB.setLocalDomains(localDomains);
					simpleTEDB.setLocalASs(localASs);
					simpleTEDB.setNeighASs(NeighASs);
					simpleTEDB.setNeighDomains(NeighDomains);
					simpleTEDB.setDomainID(domain);
					simpleTEDB.setPCEScope(pceScope);
					log.info("Received PCE info for domain/AS "+sb.toString()+" from peer "+learntFrom+": "+simpleTEDB.getMDPCE().getPCEipv4().getHostAddress());
					setMDPCEupdateTime (localDomains, PCEip, learntFrom);

				}
			}

			else if (domainTEDB==null) {
				simpleTEDB = new SimpleTEDB();
				simpleTEDB.createGraph();
				this.intraTEDBs.put(domain.getHostAddress(), simpleTEDB);
				if(simpleTEDB.getMDPCE().getLearntFrom()==null || simpleTEDB.getMDPCE().getLearntFrom().equals(learntFrom))
				{
					simpleTEDB.setMDPCE(MDPCE);
					simpleTEDB.getMDPCE().setLearntFrom(learntFrom);
					simpleTEDB.setLocalDomains(localDomains);
					simpleTEDB.setLocalASs(localASs);
					simpleTEDB.setNeighASs(NeighASs);
					simpleTEDB.setNeighDomains(NeighDomains);
					simpleTEDB.setDomainID(domain);
					simpleTEDB.setPCEScope(pceScope);
					log.info("Received PCE info for domain/AS "+sb.toString()+" from peer "+learntFrom+": "+simpleTEDB.getMDPCE().getPCEipv4().getHostAddress());
					setMDPCEupdateTime (localDomains, PCEip, learntFrom);
				}
			}
			else {
				log.info("PROBLEM: TEDB not compatible");
				return;
			}

		}
	}


	private void fillITNodeInformation(ITNodeNLRI itNodeNLRI, String learntFrom){
/*		try {
			Thread.sleep(2000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	*/
		IT_Resources itResources = null;
		DomainTEDB domainTEDB= null;
		domainTEDB=(DomainTEDB)intraTEDBs.get(itNodeNLRI.getNodeId());
		SimpleTEDB simpleTEDB=null;
		if (domainTEDB instanceof SimpleTEDB){
			simpleTEDB = (SimpleTEDB) domainTEDB;
			if(simpleTEDB.getItResources()!=null)
				itResources= simpleTEDB.getItResources();
			log.info("IT Resource:  " +simpleTEDB.getItResources());

		}else if (domainTEDB==null){
			simpleTEDB = new SimpleTEDB();
			simpleTEDB.createGraph();
			try {
				simpleTEDB.setDomainID((Inet4Address) InetAddress.getByName(itNodeNLRI.getNodeId()));
				log.info("Domain ID set to :  " +simpleTEDB.getDomainID());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			this.intraTEDBs.put(itNodeNLRI.getNodeId(), simpleTEDB);

		}
		else {
			log.info("Problem: TEDB not Compatible");
			return;
		}

		log.info("Received IT info for Domain "+itNodeNLRI.getNodeId()+" From Peer "+learntFrom);

		if(simpleTEDB.getItResources()==null)
		 itResources= new IT_Resources();

			if(itResources.getLearntFrom()==null || itResources.getLearntFrom().equals(learntFrom)) {
			log.info("Existing IT Resource LearntFrom:  " + itResources.getLearntFrom() + "  New LearntFrom:  " + learntFrom);
			itResources.setControllerIT(itNodeNLRI.getControllerIT());
			itResources.setCpu(itNodeNLRI.getCpu());
			itResources.setMem(itNodeNLRI.getMem());
			itResources.setStorage(itNodeNLRI.getStorage());
			itResources.setLearntFrom(learntFrom);
			itResources.setITdomainID(itNodeNLRI.getNodeId());
			simpleTEDB.setItResources(itResources);
			SetnodeITinfoUpdate(simpleTEDB.getDomainID(), itNodeNLRI.getNodeId(), learntFrom, System.currentTimeMillis());
			}
	}
	private void fillNodeInformation(NodeNLRI nodeNLRI, String learntFrom) throws UnknownHostException {

		Inet4Address as_number = null;
		Inet4Address areaID= null ;
		Inet4Address bgplsID = null;
		int IGP_type = 0;
		Inet4Address IGPID = null;
		int IGPIDint=0;
		Node_Info node_info = null;
		Hashtable<Object, Node_Info> NodeTable;

		log.info(".........................Fill Node Information.........................");

		if (nodeNLRI.getLocalNodeDescriptors().getAutonomousSystemSubTLV()!=null){
			as_number=nodeNLRI.getLocalNodeDescriptors().getAutonomousSystemSubTLV().getAS_ID();
			if (as_number == null) {
				log.info(" As_Number is Null");
			}
			else
				log.info("AS Number:  "+as_number.getHostAddress());
		}
		DomainTEDB domainTEDB=(DomainTEDB)intraTEDBs.get(as_number.getHostAddress());
		SimpleTEDB simpleTEDB=null;
		if (domainTEDB instanceof SimpleTEDB){
			simpleTEDB = (SimpleTEDB) domainTEDB;
		}
		else if (domainTEDB==null){
			simpleTEDB = new SimpleTEDB();
			simpleTEDB.createGraph();
			this.intraTEDBs.put(as_number.getHostAddress(), simpleTEDB);
			simpleTEDB.setDomainID(as_number);
		}
		else {
			log.info("Problem: TEDB not Compatible");
			return;
		}


		NodeTable =simpleTEDB.getNodeTable();
		//if OSPF
		if( nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF() !=null  ){

			if(simpleTEDB.getNodeTable().containsKey(nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF()))
			{
				node_info= simpleTEDB.getNodeTable().get(nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF());
				log.info("Node_info object for AS Already Exist:  " +nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF());
			}
			else {

				log.info("........New Node Info Object........");
				node_info = new Node_Info();
				node_info.setAs_number(as_number);
			}

			if(node_info.getLearntFrom()==null || node_info.getLearntFrom().equals(learntFrom)) {
				log.info("Existing Learnt From: " + node_info.getLearntFrom() + "  New Learnt From: " + learntFrom);

				if (nodeNLRI.getLocalNodeDescriptors().getAreaID() != null) {
					areaID = nodeNLRI.getLocalNodeDescriptors().getAreaID().getAREA_ID();
				}
				if (nodeNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV() != null) {
					bgplsID = nodeNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV().getBGPLS_ID();
				}
				if (nodeNLRI.getLocalNodeDescriptors().getIGPRouterID() != null) {
					IGP_type = nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIGP_router_id_type();
					switch (IGP_type) {
						case 3:
							IGPID = nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIpv4AddressOSPF();

							node_info.setIpv4Address(IGPID);
							break;
						default:
							log.info("IGP Identifier ");
					}
				}

				if (iPv4RouterIDLocalNodeLATLV != null) {
					//log.debug("........Adding IPv4 of Local Node to Table........");
					node_info.setIpv4AddressLocalNode(iPv4RouterIDLocalNodeLATLV.getIpv4Address());
				}
				if (nodeFlagBitsTLV != null) {
					//log.debug("Adding flags of Local Node to table...");
					node_info.setAbr_bit(nodeFlagBitsTLV.isAbr_bit());
					node_info.setAttached_bit(nodeFlagBitsTLV.isAttached_bit());
					node_info.setExternal_bit(nodeFlagBitsTLV.isExternal_bit());
					node_info.setOverload_bit(nodeFlagBitsTLV.isOverload_bit());
				}

				if (nodeNameTLV != null) {
					//log.debug("Adding name of Local Node to Table....");
					node_info.setName(nodeNameTLV.getName());
				}

				if (areaIDTLV != null) {
					//log.debug("Adding AreaID of Local node to table....");
					node_info.setIpv4areaIDs(areaIDTLV.getIpv4areaIDs());
				}

				if (sidTLV != null) {
					//log.debug("Adding SID of Local node to table....");
					node_info.setSID(sidTLV.getSid());
				}

										//.... finally we set the 'learnt from' Attribute
				node_info.setLearntFrom(learntFrom);
				log.info("Value for Setting Learnt from: " + node_info.getLearntFrom());



				if (NodeTable != null) {
					if (!NodeTable.containsKey(IGPID)) {
						NodeTable.remove(IGPID);
						NodeTable.put(IGPID, node_info);
					}
				}

									//NodeTable.put(IGPID, node_info);

				simpleTEDB.setNodeTable(NodeTable);
				if (this.multiTedb != null) {
					if (node_info.getIpv4Address() != null) {
						this.multiTedb.addReachabilityIPv4(as_number, node_info.getIpv4Address(), 32);
					}

				}
				log.info("Node Table:" + NodeTable.toString());
				log.info("Node Information Table Updated....");


			setNodeInfoUpdateTime (as_number, IGPID, learntFrom, System.currentTimeMillis());
			}
		}
		//if ISIS
		else {
			if(simpleTEDB.getNodeTable().containsKey(nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getISIS_ISO_NODE_ID()))
			{
				node_info= simpleTEDB.getNodeTable().get(nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getISIS_ISO_NODE_ID());
				log.info("Node_info object for AS Already Exist:  " +nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getISIS_ISO_NODE_ID());
			}
			else {

				log.info("........New Node Info Object........");
				node_info = new Node_Info();
				node_info.setAs_number(as_number);
			}

			if(node_info.getLearntFrom()==null || node_info.getLearntFrom().equals(learntFrom)) {
				log.info("Existing Learnt From: " + node_info.getLearntFrom() + "  New Learnt From: " + learntFrom);

				if (nodeNLRI.getLocalNodeDescriptors().getAreaID() != null) {
					areaID = nodeNLRI.getLocalNodeDescriptors().getAreaID().getAREA_ID();
				}
				if (nodeNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV() != null) {
					bgplsID = nodeNLRI.getLocalNodeDescriptors().getBGPLSIDSubTLV().getBGPLS_ID();
				}
				if (nodeNLRI.getLocalNodeDescriptors().getIGPRouterID() != null) {
					IGP_type = nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getIGP_router_id_type();
					switch (IGP_type) {
						case 1: //IIGP_ROUTER_ID_TYPE_IS_IS_NON_PSEUDO
							IGPIDint = nodeNLRI.getLocalNodeDescriptors().getIGPRouterID().getISIS_ISO_NODE_ID();

							node_info.setISISID(IGPIDint);

						default:
							log.info("IGP Identifier ");
					}
				}

				if (iPv4RouterIDLocalNodeLATLV != null) {
					//log.debug("........Adding IPv4 of Local Node to Table........");
					node_info.setIpv4AddressLocalNode(iPv4RouterIDLocalNodeLATLV.getIpv4Address());
				}
				if (nodeFlagBitsTLV != null) {
					//log.debug("Adding flags of Local Node to table...");
					node_info.setAbr_bit(nodeFlagBitsTLV.isAbr_bit());
					node_info.setAttached_bit(nodeFlagBitsTLV.isAttached_bit());
					node_info.setExternal_bit(nodeFlagBitsTLV.isExternal_bit());
					node_info.setOverload_bit(nodeFlagBitsTLV.isOverload_bit());
				}

				if (nodeNameTLV != null) {
					//log.debug("Adding name of Local Node to Table....");
					node_info.setName(nodeNameTLV.getName());
				}

				if (areaIDTLV != null) {
					//log.debug("Adding AreaID of Local node to table....");
					node_info.setIpv4areaIDs(areaIDTLV.getIpv4areaIDs());
				}

				if (sidTLV != null) {
					//log.debug("Adding SID of Local node to table....");
					node_info.setSID(sidTLV.getSid());
				}

				//.... finally we set the 'learnt from' Attribute
				node_info.setLearntFrom(learntFrom);
				log.info("Value for Setting Learnt from: " + node_info.getLearntFrom());



				if (NodeTable != null) {
					if (IGPIDint!=0){
						if (!NodeTable.containsKey(IGPIDint)) {
							NodeTable.remove(IGPIDint);
							NodeTable.put(IGPIDint, node_info);
						}
					}
					else log.info("IGPint==0");
				}

				//NodeTable.put(IGPID, node_info);

				simpleTEDB.setNodeTable(NodeTable);
				simpleTEDB.setIGPType(1);
				if (this.multiTedb != null) {
					if (node_info.getIpv4Address() != null) {
						this.multiTedb.addReachabilityIPv4(as_number, node_info.getIpv4Address(), 32);
					}

				}
				log.info("Node Table:" + NodeTable.toString());
				log.info("Node Information Table Updated....");


				setNodeInfoUpdateTime (as_number, IGPIDint, learntFrom, System.currentTimeMillis());
			}
		}
	}

	private void clearAttributes(){
		maximumLinkBandwidthTLV= null;
		maxReservableBandwidthTLV= null;
		unreservedBandwidthTLV= null;
		administrativeGroupTLV = null;
		linkProtectionTLV =null;
		metricTLV = null;
		iPv4RouterIDLocalNodeLATLV = null;
		iPv4RouterIDRemoteNodeLATLV = null;
		TEMetricTLV = null;				
		transceiverClassAndAppATLV = null;
		mF_OTP_ATLV = null;
		availableLabels=null;
		linkDelay=0;
		linkDelayVar=0;
		minDelay=0;
		maxDelay=0;
		linkLoss=0;
		residualBw=0;
		availableBw=0;
		utilizedBw=0;

	}

	public void setIntraDomainEdgeUpdateTime(Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Inet4Address RemoteNodeIGPId, long LocalIdentifier, long RemoteIdentifier, long LinkUpdateTime) {
		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, localDomainID, LinkUpdateTime);
		IntraDomainLinkUpdateTime intraDom_linkUpdate= new IntraDomainLinkUpdateTime(intraDomainLinkUpdate, localDomainID, LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId,RemoteIdentifier, LinkUpdateTime);
		//log.info("Domain ID : " +String.valueOf(localDomainID)  +"DomainTEDS Size:  " +DomainUpdate.size() + "   Time of Update:  " + LinkUpdateTime);
		//log.info("..................Added Intra-Domain Link : " +intraDom_linkUpdate   + "   Time of Update:  " + LinkUpdateTime);
	}

	public void setIntraDomainEdgeUpdateTime(Inet4Address localDomainID, int LocalNodeIGPId, int RemoteNodeIGPId, long LocalIdentifier, long RemoteIdentifier, long LinkUpdateTime) {
		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, localDomainID, LinkUpdateTime);
		IntraDomainLinkUpdateTime intraDom_linkUpdate= new IntraDomainLinkUpdateTime(intraDomainLinkUpdate, localDomainID, LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId,RemoteIdentifier, LinkUpdateTime);
		//log.info("Domain ID : " +String.valueOf(localDomainID)  +"DomainTEDS Size:  " +DomainUpdate.size() + "   Time of Update:  " + LinkUpdateTime);
		//log.info("..................Added Intra-Domain Link : " +intraDom_linkUpdate   + "   Time of Update:  " + LinkUpdateTime);
	}



	public void setInterDomainEdgeUpdateTime(Inet4Address localDomainID,Inet4Address LocalNodeIGPId, Long LinkLocalIdentifier, Inet4Address remoteDomainID, Inet4Address RemoteNodeIGPId, Long LinkRemoteIdentifier, long LinkUpdateTime) {

		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, localDomainID, LinkUpdateTime);
		//log.info("Domain Id : " +String.valueOf(localDomainID) +"DomainTEDS Size:  " +DomainUpdate.size()  + "   Time of Update:  " + LinkUpdateTime);
		InterDomainLinkUpdateTime interDom_linkUpdate= new InterDomainLinkUpdateTime(interDomainLinkUpdate, localDomainID, LocalNodeIGPId,LinkLocalIdentifier,remoteDomainID,RemoteNodeIGPId,LinkRemoteIdentifier, LinkUpdateTime);
		//log.info("..................Added InterDomain Link : " +interDom_linkUpdate.toString()   + "   Time of Update:  " + LinkUpdateTime);
		
	}
	public void setInterDomainEdgeUpdateTime(Inet4Address localDomainID,int LocalNodeIGPId, Long LinkLocalIdentifier, Inet4Address remoteDomainID, int RemoteNodeIGPId, Long LinkRemoteIdentifier, long LinkUpdateTime) {

		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, localDomainID, LinkUpdateTime);
		//log.info("Domain Id : " +String.valueOf(localDomainID) +"DomainTEDS Size:  " +DomainUpdate.size()  + "   Time of Update:  " + LinkUpdateTime);
		InterDomainLinkUpdateTime interDom_linkUpdate= new InterDomainLinkUpdateTime(interDomainLinkUpdate, localDomainID, LocalNodeIGPId,LinkLocalIdentifier,remoteDomainID,RemoteNodeIGPId,LinkRemoteIdentifier, LinkUpdateTime);
		//log.info("..................Added InterDomain Link : " +interDom_linkUpdate.toString()   + "   Time of Update:  " + LinkUpdateTime);

	}
	private void SetnodeITinfoUpdate(Inet4Address DomainID, String nodeId, String learntFrom, long UpdateTime) {

		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, DomainID, UpdateTime);
		NodeITinfoUpdateTime nodeITinfo_UpdateTime = new NodeITinfoUpdateTime (nodeITinfoUpdate, DomainID, nodeId, UpdateTime);
		nodeITinfo_UpdateTime.setlearntfrom(learntFrom);
		//log.info("Domain Id : " +String.valueOf(DomainID) +"DomainTEDS Size:  " +DomainUpdate.size()  + "   Time of Update:  " +UpdateTime);
		//log.info("..................Added Node IT Info : " +nodeITinfo_UpdateTime +" Size: "   +nodeITinfoUpdate.size()  +"  Learnt From: " +nodeITinfo_UpdateTime.getlearntfrom() + "   Time of Update:  " + UpdateTime);

	}

	public void setNodeInfoUpdateTime(Inet4Address DomainID, Inet4Address localnodeID, String learntFrom, long UpdateTime) throws UnknownHostException {

		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, DomainID, UpdateTime);
		//log.info("Domain Id : " +String.valueOf(DomainID) +"DomainTEDS Size:  " +DomainUpdate.size()  + "   Time of Update:  " +UpdateTime);
		NodeinfoUpdateTime node_updateTime= new NodeinfoUpdateTime(nodeinfoUpdate,DomainID,localnodeID,UpdateTime);
		//log.info("Node Information Update Time  DomainID:" +DomainID  +" Local node Address: "  +localnodeID);
		node_updateTime.setlearntfrom(learntFrom);
		//log.info("..................Added Node Info : " +node_updateTime  +"  Learnt From:" +node_updateTime.getlearntfrom() + "   Time of Update:  " + UpdateTime);
	}

	public void setNodeInfoUpdateTime(Inet4Address DomainID, int localnodeID, String learntFrom, long UpdateTime) throws UnknownHostException {

		DomainUpdateTime domain_update = new DomainUpdateTime(DomainUpdate, DomainID, UpdateTime);
		//log.info("Domain Id : " +String.valueOf(DomainID) +"DomainTEDS Size:  " +DomainUpdate.size()  + "   Time of Update:  " +UpdateTime);
		NodeinfoUpdateTime node_updateTime= new NodeinfoUpdateTime(nodeinfoUpdate,DomainID,localnodeID,UpdateTime);
		//log.info("Node Information Update Time  DomainID:" +DomainID  +" Local node Address: "  +localnodeID);
		node_updateTime.setlearntfrom(learntFrom);
		//log.info("..................Added Node Info : " +node_updateTime  +"  Learnt From:" +node_updateTime.getlearntfrom() + "   Time of Update:  " + UpdateTime);
	}


	public void setMDPCEupdateTime(ArrayList<Inet4Address> localDomains, Inet4Address PCEip, String Learntfrom) {
		//log.info("////------------////  PCE IP:  " +PCEip +"  Learnt From: " +Learntfrom);


		MDPCEinfoUpdateTime MDPCE_updateTime= new MDPCEinfoUpdateTime(MDPCEinfoUpdate,localDomains,PCEip,Learntfrom, System.currentTimeMillis());
		MDPCEinfoUpdate.put(MDPCE_updateTime, System.currentTimeMillis());
	}




	public Long getDomainUpdateTime (Inet4Address localDomainID){
		//log.info("Domain ID get : " +String.valueOf(localDomainID)   + "   Stored Time:  " + DomainUpdate.get(String.valueOf(localDomainID)));
		return DomainUpdate.get(String.valueOf(localDomainID));

	}

	public Long getIntraDomainEdgeUpdateTime (IntraDomainLinkUpdateTime intraDom_linkUpdate){
		//log.info("IntraDomain Edge get: " +intraDom_linkUpdate.toString()   + "   Stored Time:  " + intraDomainLinkUpdate.get(intraDom_linkUpdate));
		return intraDomainLinkUpdate.get(intraDom_linkUpdate);
	}

	public Long getInterDomainEdgeUpdateTime (InterDomainLinkUpdateTime interDom_linkUpdate){
		//log.info("InterDomain Edge get : " +interDom_linkUpdate.toString()   + "   Stored Time:  " + interDomainLinkUpdate.get(interDom_linkUpdate));
		return interDomainLinkUpdate.get(interDom_linkUpdate);
	}

	public Long getNodeInfoUpdateTime (NodeinfoUpdateTime node_updateTime){
		//log.info("InterDomain Edge get : " +node_updateTime.toString()   + "   Stored Time:  " + nodeinfoUpdate.get(node_updateTime));
		return nodeinfoUpdate.get(node_updateTime);
	}


	public  Long getMDPCEupdateTime(MDPCEinfoUpdateTime MDPCE_updateTime) {

		return MDPCEinfoUpdate.get(MDPCE_updateTime);
	}









	
	}


package es.tid.bgp.bgp4Peer.updateTEDB;

import es.tid.bgp.bgp4.messages.BGP4Update;
import es.tid.bgp.bgp4Peer.peer.BGP4Parameters;
import es.tid.bgp.bgp4Peer.peer.DomainUpdateTime;
import es.tid.bgp.bgp4Peer.peer.MDPCEinfoUpdateTime;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.TEDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This class is in charge of storing the BGP4 update messages in a queue to be processing 
 * 
 * @author pac
 *
 */
public class UpdateDispatcher {
	
	private Logger log;
	private LinkedBlockingQueue<BGP4Update> updateList;
	private UpdateProccesorThread upt;



	public UpdateDispatcher(MultiDomainTEDB multiTedb, Hashtable<String, TEDB> intraTEDBs, Hashtable<DomainUpdateTime, Long> domainUpdate, Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate, Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate, Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate, Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate, BGP4Parameters params){


		//log=LoggerFactory.getLogger("BGP4Server");
		this.updateList=new LinkedBlockingQueue<BGP4Update>();
		this.upt=new UpdateProccesorThread(updateList, multiTedb,intraTEDBs,intraDomainLinkUpdate,interDomainLinkUpdate,nodeITinfoUpdate,nodeinfoUpdate,MDPCEinfoUpdate,domainUpdate );
		upt.start();
		
	}



	public void dispatchRequests(BGP4Update updateMessage){
		updateList.add(updateMessage);
	}


	public void UpdateMsgQueue(Inet4Address remotePeerIP) {

		log= LoggerFactory.getLogger("BGP4Peer");


		BGP4Update update= null;

		log.info("Length before Removal "+updateList.size());

		for(BGP4Update up: updateList)
		{
			//log.info("Get Learn From "+up.getLearntFrom() +"  Remote IP : " +remotePeerIP.getHostAddress());

			if((up.getLearntFrom()).equals(remotePeerIP.getHostAddress())){
				//update = updateList.take();
				updateList.remove(up);

			}

		}

		log.info("Length After Removal "+updateList.size());

	}


}

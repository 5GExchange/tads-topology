package es.tid.bgp.bgp4Peer.management;

import es.tid.bgp.bgp4Peer.bgp4session.BGP4SessionsInformation;
import es.tid.bgp.bgp4Peer.peer.BGP4LSPeerInfo;
import es.tid.bgp.bgp4Peer.peer.BGP4Parameters;
import es.tid.bgp.bgp4Peer.peer.SendTopology;
import es.tid.bgp.bgp4Peer.updateTEDB.UpdateDispatcher;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.TEDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * To manage the server 
 * 
 * @author mcs
 *
 */
public class BGP4ManagementServer extends Thread {
	private Logger log;
	private int BGP4ManagementPort = 8888;
	private BGP4SessionsInformation bgp4SessionsInformation;
	/**
	 * Topology database for interDomain Links.
	 */
	private MultiDomainTEDB multiTEDB;
	/**
	 * Topology database for intradomain Links. It owns several domains.
	 */
	private Hashtable<String,TEDB> intraTEDBs;

	/**
	 * Class to send the topology. It is needed to set the parameters sendTopology to true or false.
	 */
	private SendTopology sendTopology;

	//private LinkedList<BGP4LSPeerInfo> peersToConnect;

	private BGP4Parameters params;

	private UpdateDispatcher ud;

	private ScheduledThreadPoolExecutor executor;
	//
	//public BGP4ManagementServer(LinkedList<BGP4LSPeerInfo> peersToConnect, int BGP4ManagementPort, MultiDomainTEDB multiTEDB, Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, SendTopology sendTopology){
		public BGP4ManagementServer(ScheduledThreadPoolExecutor executor, UpdateDispatcher ud, BGP4Parameters params, MultiDomainTEDB multiTEDB, Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, SendTopology sendTopology){
			log =LoggerFactory.getLogger("BGP4Server");
			this.BGP4ManagementPort = params.getBGP4ManagementPort();
			this.multiTEDB=multiTEDB;
			this.intraTEDBs=intraTEDBs;
			this.bgp4SessionsInformation =bgp4SessionsInformation;
			this.sendTopology=sendTopology;
			//this.peersToConnect= peersToConnect;
			this.params=params;
			this.ud= ud;
			this.executor= executor;
	}
	/**
	 * RUN
	 */
	public void run(){
	    ServerSocket serverSocket = null;
	    boolean listening=true;
		try {
	      	  log.debug("Listening management on port "+BGP4ManagementPort);
	          serverSocket = new ServerSocket(BGP4ManagementPort);
		  }
		catch (Exception e){
			 log.error("Could not listen management on port "+BGP4ManagementPort);
			e.printStackTrace();
			return;
		}
		
		   try {
	        	while (listening) {
	        		new BGP4ManagementSession(serverSocket.accept(),multiTEDB,intraTEDBs,bgp4SessionsInformation, sendTopology, params, ud,executor).start();
	        	}
	        	serverSocket.close();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }				
	}
}

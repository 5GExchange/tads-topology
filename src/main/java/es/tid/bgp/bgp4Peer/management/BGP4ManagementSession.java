package es.tid.bgp.bgp4Peer.management;

import java.io.*;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import es.tid.bgp.bgp4Peer.peer.*;
import es.tid.bgp.bgp4Peer.updateTEDB.UpdateDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.tid.bgp.bgp4Peer.bgp4session.BGP4SessionsInformation;
import es.tid.bgp.bgp4Peer.tedb.IntraTEDBS;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;

/**
 * 
 * @author mcs
 *
 */
public class BGP4ManagementSession extends Thread {
	/**
	 * The socket of the management session
	 */
	private Socket socket;
	
	/**
	 * Logger
	 */
	private Logger log;
	
	/**
	 * Output Stream of the managament session, to write the answers.
	 */
	private PrintStream out;
	/**
	 * Topology database for interDomain Links.
	 */
	private MultiDomainTEDB multiTEDB;
	/**
	 * Topology database for intradomain Links. It owns several domains.
	 */
	private Hashtable<String,TEDB> intraTEDBs;
	
	/**
	 * The infomation of all the active sessions
	 */
	private BGP4SessionsInformation bgp4SessionsInformation;
	/**
	 * Class to send the topology. It is needes to set the parameters sendTopology to true or false.
	 */
	private SendTopology sendTopology;

	private BGP4LSPeerInfo peerInfo= null;
	private Inet4Address peerBGPIP;
	private int peerPort;
	private String str;
	private boolean send = false;
	private boolean receive = false;
	private LinkedList<BGP4LSPeerInfo> peersToConnect;
	private BGP4Parameters params;
	private UpdateDispatcher ud;
	private  BGP4SessionClientManager  bgp4SessionClientManager;
	private ScheduledThreadPoolExecutor executor;
	private int existing_peers;


	public BGP4ManagementSession(Socket s, MultiDomainTEDB multiTEDB, Hashtable<String, TEDB> intraTEDBs, BGP4SessionsInformation bgp4SessionsInformation, SendTopology sendTopology, BGP4Parameters params, UpdateDispatcher ud, ScheduledThreadPoolExecutor executor){
		this.socket=s;
		log=LoggerFactory.getLogger("BGP4Server");
		this.multiTEDB=multiTEDB;
		this.intraTEDBs=intraTEDBs;
		this.bgp4SessionsInformation= bgp4SessionsInformation;
		this.sendTopology=sendTopology;
		this.params=params;
		this.ud= ud;
		this.peersToConnect= params.getPeersToConnect();
		this.executor= executor;
		existing_peers= peersToConnect.size();
	}
	
	public void run(){
		log.info("Starting Management session");
		boolean running=true;
		try {
			out=new PrintStream(socket.getOutputStream());
		} catch (IOException e) {
			log.warn("Management session cancelled: "+e.getMessage());
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while (running) {
				//out.print("BGP4:>");
				out.print("Available commands:\r\n");
				out.print(" > show BGP Peer configuration\r\n");
				out.print(" > Add BGPPeer\r\n");
				out.print(" > show topology\r\n");
				out.print(" > show sessions\r\n");
				out.print(" > set traces on\r\n");
				out.print(" > set traces off\r\n");
				out.print(" > send topology on\r\n");
				out.print(" > send topology off\r\n");
				out.print(" > quit\r\n");
				
				
				String command = null;
				try {
					command = br.readLine();
				} catch (IOException ioe) {
					log.warn("IO error trying to read your command");
					return;
				}
				if (command.equals("quit")) {
					log.info("Ending Management Session");
					out.println("bye!");
					try {
						out.close();						
					} catch (Exception e){
						e.printStackTrace();
					}
					try {
						br.close();						
					} catch (Exception e){
						e.printStackTrace();
					}					
					return;
				}				
				
//				else if (command.equals("help")){
//					out.print("Available commands:\r\n");
//					out.print(" > show topology\r\n");
//					out.print(" > show sessions\r\n");
//					out.print(" > set traces on\r\n");
//					out.print(" > set traces off\r\n");
//					out.print(" > send topology on\r\n");
//					out.print(" > send topology off\r\n");
//					out.print(" > quit\r\n");
//					
//				}

				else if (command.equals("show BGP Peer configuration")){

					//for (BGP4LSPeerInfo i: peersToConnect)
						for (int i =0;i< peersToConnect.size(); i++)
						{

						out.println(" Peer BGP IP: " + peersToConnect.get(i).getPeerIP());
						out.println(" Export: " + peersToConnect.get(i).isSendToPeer());
						out.println(" Import: " + peersToConnect.get(i).isUpdateFromPeer());
						out.println(" Peer BGP Port: " +peersToConnect.get(i).getPeerPort());
					}
				}



				else if (command.equals("show sessions")){
					//Print intradomain and interDomain links
					out.print(bgp4SessionsInformation.toString());
				}
				else if (command.equals("show topology")){
					//Print intradomain and interDomain links
					if (multiTEDB != null)
						out.println(multiTEDB.printTopology());
					Enumeration<String> domainTedbs=intraTEDBs.keys();
					while (domainTedbs.hasMoreElements()){		
						String domainID=domainTedbs.nextElement();
						TEDB ted=intraTEDBs.get(domainID);
						if (ted instanceof DomainTEDB) {
							out.println("Intradomain TEDB with ID "+domainID);
							out.println(ted.printTopology());
						}
					}
				}

				else if (command.equals("Add BGPPeer")) {


					while (true) {
						peerInfo = new BGP4LSPeerInfo();

						out.print(" Enter the Peer IP address:\r\n");
						try {
							peerBGPIP = (Inet4Address) Inet4Address.getByName(br.readLine());
							out.print("IP address: " + peerBGPIP + "\r\n");
							peerInfo.setPeerIP(peerBGPIP);
							peersToConnect.add(peerInfo);
							out.println("peerInfo: " + peerInfo.getPeerIP());
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}

						while (true) {
							out.print(" Export option (true/false)?\r\n");
							str = (br.readLine());
							if ((str.compareTo("true") == 0) || (str.compareTo("false") == 0)) {

								send = Boolean.valueOf(str);
								out.print("Send:" + send + "\r\n");
								peerInfo.setSendToPeer(send);
								out.println("Send to Peer:" + peerInfo.isSendToPeer());
								break;
							}
						}


						while (true) {
							out.print("Import option (true/false)?\r\n");
							str = (br.readLine());
							if ((str.compareTo("true") == 0) || (str.compareTo("false") == 0)) {
								receive = Boolean.valueOf(str);
								out.print("Receive:" + receive + "\r\n");
								peerInfo.setUpdateFromPeer(receive);
								out.println("Send to Peer:" + peerInfo.isUpdateFromPeer());
								break;
							}
						}


						out.print("Peer Port:\r\n");
						peerPort = Integer.parseInt(br.readLine());
						peerInfo.setPeerPort(peerPort);
						out.println("Set Peer Port:" + peerInfo.getPeerPort());

						while (true) {
							out.print("Add another BGP Peer (yes/no)?\r\n");
							str = (br.readLine());
							if ((str.compareTo("yes") == 0) || (str.compareTo("no") == 0)) {

								out.print("Option:" + str + "\r\n");
								break;
							}
						}
						if (str.compareTo("no") == 0)
							break;

					}
					if (peersToConnect!=null) {
						
						for(int i= existing_peers; i<peersToConnect.size(); i++) {

							bgp4SessionClientManager = new BGP4SessionClientManager(bgp4SessionsInformation, ud, peersToConnect.get(i), params.getBGP4Port(), params.getLocalBGPAddress(), params.getLocalBGPPort(), params.getHoldTime(), (Inet4Address) Inet4Address.getByName(params.getBGPIdentifier()), params.getVersion(), params.getMyAutonomousSystem(), params.getKeepAliveTimer());
							//FIXME: Ver si dejamos delay fijo o variable
							executor.scheduleWithFixedDelay(bgp4SessionClientManager, 0, params.getDelay(), TimeUnit.MILLISECONDS);
						}


						for (BGP4LSPeerInfo i: peersToConnect) {
							out.println(" Peer BGP IP: " + i.getPeerIP());
							out.println(" Export: " + i.isSendToPeer());
							out.println(" Import: " + i.isUpdateFromPeer());
							out.println(" Peer BGP Port: " +i.getPeerPort());
						}

					}
				}

				else if (command.equals("set traces on")) {
					//log.setLevel(Level.ALL);		
					Logger log2=LoggerFactory.getLogger("BGP4Parser");
					//log2.setLevel(Level.ALL);			
					Logger log3=LoggerFactory.getLogger("BGP4Client");
					//log3.setLevel(Level.ALL);
					out.print("traces on!\r\n");
				} 
				else if (command.equals("set traces off")) {
					//log.setLevel(Level.SEVERE);		
					Logger log2=LoggerFactory.getLogger("BGP4Parser");
					//log2.setLevel(Level.SEVERE);
					Logger log3=LoggerFactory.getLogger("BGP4Client");
					//log3.setLevel(Level.SEVERE);
					out.print("traces off!\r\n");
				} 
				else if (command.equals("send topology on")) {
					sendTopology.setSendTopology(true);
				}
				else if (command.equals("send topology off")) {
					sendTopology.setSendTopology(false);
				}
				else{
					out.print("invalid command\n");	
					out.print("\n");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}




}

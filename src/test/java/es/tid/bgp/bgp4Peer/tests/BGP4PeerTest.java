package es.tid.bgp.bgp4Peer.tests;

import es.tid.bgp.bgp4Peer.peer.BGPPeer;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.MDTEDB;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class BGP4PeerTest {
	
	public BGP4PeerTest(){
		
	}
	
	/**
	 * This tests starts a BGL-LS Speaker, reads the topology from a File and Sends it.
	 * A second speaker is started and reads the topology.
	 * The first speaker is configured to read only the multidomain topology and send the 
	 * multidomain topology
	 * The second speaker is configured as Consumer. 
	 * The speakers are launched in separated non-standard ports for testing purposes.
	 * Both speakers talk and the topology is sent from BGP-Speaker 1 to BGP-Speaker 2
	 * It checks after 10 seconds if the topology of BGP-Speaker 2 is the same as BGP
	 * Speaker 1. 
	 */
	@org.junit.Test
	public void testPeer(){
		try {
		//Create BGP4Peer 1 
		BGPPeer bgpPeer = new BGPPeer();
		bgpPeer.configure("src/test/resources/BGP4Parameters_1.xml");
		//Create the TEDB
		//bgpPeer.createTEDB("test"); //did it in configure
		assertTrue("MD Topology has 1 domains",((MDTEDB)bgpPeer.getMultiDomainTEDB()).getNetworkDomainGraph().vertexSet().size()==1);
		bgpPeer.createUpdateDispatcher();
		bgpPeer.startClient();		
		bgpPeer.startServer();
		bgpPeer.startSaveTopology();
		bgpPeer.startManagementServer();
		bgpPeer.startSendTopology();
		String topoOriginal=bgpPeer.getMultiDomainTEDB().printTopology();
		
		System.out.println("Topology1: ");
		System.out.println(topoOriginal);
		
		
		//Launch BGP4Peer
		//bgpPeer.stopPeer();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BGPPeer bgpPeer2 = new BGPPeer();
		bgpPeer2.configure("src/test/resources/BGP4Parameters_2.xml");
		//Create the TEDB
		//bgpPeer2.createTEDB("test"); //did it in configure
		bgpPeer2.createUpdateDispatcher();
		bgpPeer2.startClient();		
		bgpPeer2.startServer();
		bgpPeer2.startSaveTopology();
		bgpPeer2.startManagementServer();
		bgpPeer2.startSendTopology();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String topo2=bgpPeer2.getMultiDomainTEDB().printTopology();
		
		System.out.println("---------------------------------------");
		System.out.println("---------------------------------------");
		System.out.println("---------------------------------------");
		System.out.println("Topology of BGP-LS Speaker 1: ");
		System.out.println("---------------------------------------");
		System.out.println(topoOriginal);
		Set<String> keySet = bgpPeer.getIntraTEDBs().keySet();
		for(String key : keySet){
			System.out.println("---IntraTEDB: domain_id= "+key);
			System.out.println(bgpPeer.getIntraTEDBs().get(key).printTopology());
		}
		System.out.println("---------------------------------------");
		System.out.println("Topology of  BGP-LS Speaker 2: ");
		System.out.println("---------------------------------------");		
		System.out.println(topo2);
		System.out.println("---------------------------------------");
		Set<String> keySet2 = bgpPeer2.getIntraTEDBs().keySet();
		for(String key : keySet2){
			System.out.println("---IntraTEDB: domain_id= "+key);
			System.out.println(bgpPeer2.getIntraTEDBs().get(key).printTopology());
		}
		/*try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		for(String key : keySet){
			assertTrue("Checking if topos are equals, IntraTEDBs, don't have the same domains", bgpPeer2.getIntraTEDBs().keySet().contains(key));

			assertTrue("Checking topo of domain "+key+" has not the same number of links:\n",bgpPeer.getIntraTEDBs().get(key).getInterDomainLinks().size()==bgpPeer2.getIntraTEDBs().get(key).getInterDomainLinks().size() );
			assertTrue("Checking topo of domain "+key+" has not the same number of nodes:\n",((DomainTEDB) bgpPeer.getIntraTEDBs().get(key)).getNodeTable().size()==((DomainTEDB) bgpPeer2.getIntraTEDBs().get(key)).getNodeTable().size() );


			/*assertTrue("->Checking if topos are equals, IntraTEDB (domains="+key+") are not equal:\n" +
					"TED1:\n"+bgpPeer.getIntraTEDBs().get(key).printTopology()+"\n" +
					"TED2:\n"+bgpPeer2.getIntraTEDBs().get(key).printTopology(),
					bgpPeer.getIntraTEDBs().get(key).printTopology().equals(bgpPeer2.getIntraTEDBs().get(key).printTopology()));
			*/
		}
		//assertTrue("Checking if topos are equal",topoOriginal.equals(topo2));
		} catch (Exception exc){
			exc.printStackTrace();
			assertTrue("Exception "+exc.getMessage(),false);
		}
			
		
	}

}

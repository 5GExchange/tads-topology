package es.tid.bgp.bgp4Peer.peer;

import es.tid.bgp.bgp4Peer.updateTEDB.InterDomainLinkUpdateTime;
import es.tid.bgp.bgp4Peer.updateTEDB.IntraDomainLinkUpdateTime;
import es.tid.bgp.bgp4Peer.updateTEDB.NodeITinfoUpdateTime;
import es.tid.bgp.bgp4Peer.updateTEDB.NodeinfoUpdateTime;
import es.tid.tedb.DomainTEDB;
import es.tid.tedb.MultiDomainTEDB;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;


//import org.slf4j.Logger;

/**
 * Created by Ajmal  on 2017-08-03.
 */
public class UpdateDomainStatus implements Runnable {

    private Hashtable<DomainUpdateTime, Long> domainUpdate;
    private Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate;
    private Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate;
    private Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate;
    private Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate;
    private Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate;

    private MultiDomainTEDB multiDomainTEDB;
    private Hashtable<String, TEDB> intraTEDBs;
    private BGP4Parameters param;
    private Logger log;

    public void configure(MultiDomainTEDB multiDomainTEDB, Hashtable<String, TEDB> intraTEDBs, Hashtable<DomainUpdateTime, Long> domainUpdate, Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate, Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate, Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate, Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate, BGP4Parameters param)  {


        log = LoggerFactory.getLogger("BGP4Peer");

        this.domainUpdate= domainUpdate;
        this.intraDomainLinkUpdate=intraDomainLinkUpdate;
        this.interDomainLinkUpdate=interDomainLinkUpdate;
        this.nodeITinfoUpdate=nodeITinfoUpdate;
        this.nodeinfoUpdate=nodeinfoUpdate;
        this.multiDomainTEDB= multiDomainTEDB;
        this.intraTEDBs= intraTEDBs;
        this.MDPCEinfoUpdate=MDPCEinfoUpdate;
        this.param=param;
    }


    @Override
    public void run() {


                         //--------------Checking the Update time for IntraDomain Links-------------/
        
        log.debug("------------- Inside UpdateDomainStatus");
        if(intraDomainLinkUpdate.size()!=0)
        {
            log.debug("------------- Stored IntraDomain Link Size: " +intraDomainLinkUpdate.size());
            Enumeration IntraLinkID = intraDomainLinkUpdate.keys();
            while(IntraLinkID.hasMoreElements())
            {
                IntraDomainLinkUpdateTime str= (IntraDomainLinkUpdateTime) IntraLinkID.nextElement();

                log.debug("-------------IntraDomain Link: " +str.toString());
                Long timeToCheck = intraDomainLinkUpdate.get(str) + param.getDelay();

                try {
                    if(System.currentTimeMillis()>timeToCheck)
                    {
                                    //-----IntraDomain Link Information is Not UpDATED!-----//


                        log.debug("IntraDomainLink:  " +str.toString()   +" Time Stored: " + intraDomainLinkUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+ intraDomainLinkUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                        log.debug("IntraDomain TEDB Size: " +intraDomainLinkUpdate.size()  +" Domain ID: " +str.getlocalDomainID());

                        log.debug("Before IntraDomain TEDB Size: " +intraDomainLinkUpdate.size() );

                            DomainTEDB domainTEDB=(DomainTEDB)intraTEDBs.get(str.getlocalDomainID().getHostAddress());
                            SimpleTEDB simpleTEDBxx=null;
                            simpleTEDBxx = (SimpleTEDB) domainTEDB;
                            if(simpleTEDBxx.getNetworkGraph().containsEdge(str.getLocalNodeIGPId(), str.getRemoteNodeIGPId())) {
                                log.debug("Contain IntraDomain Edge from: " + str.getLocalNodeIGPId() + "<------> to:" + str.getRemoteNodeIGPId());
                                simpleTEDBxx.getNetworkGraph().removeEdge(str.getLocalNodeIGPId(), str.getRemoteNodeIGPId());
                                intraDomainLinkUpdate.remove(str);
                            }
                        log.debug("After IntraDomain TEDB Size: " +intraDomainLinkUpdate.size());
                    }
                    else{
                                         //------The Domain is Recently updated------//
                       // log.debug("------Else Case------    IntraDomainLink:  " +str.toString()   +" Time Stored: " + intraDomainLinkUpdate.get(str) + " Time to refresh : " +(param.getBGPupdateTime()+ intraDomainLinkUpdate.get(str)) + " Current time: " +System.currentTimeMillis());
                    }
                } finally {
                   // log.debug("Finalize!!!");
                }

            }

        }

                            //--------------Check the Update time for InterDomain Links----------------/

        if(interDomainLinkUpdate.size()!=0)
        {

            log.debug("------------- Stored InterDomain Link Size: " +interDomainLinkUpdate.size() );

            Enumeration InterLinkID = interDomainLinkUpdate.keys();

            while(InterLinkID.hasMoreElements())
            {
                InterDomainLinkUpdateTime str= (InterDomainLinkUpdateTime) InterLinkID.nextElement();

                log.debug("-------------InterDomain Link: " +str.toString());

                Long timeToCheck = interDomainLinkUpdate.get(str) + param.getDelay();

                try {
                    if(System.currentTimeMillis()>timeToCheck)
                    {
                                             //-----IntraDomain Link Information is Not UpDATED!-----//

                        log.debug("InterDomainLink:  " +str.toString()   +" Time Stored: " + interDomainLinkUpdate.get(str) + " Time to Refresh : " +(param.getBGPupdateTime()+ interDomainLinkUpdate.get(str)) + " Current time: " +System.currentTimeMillis());
                        log.debug("InterDomain TEDB Size: " +interDomainLinkUpdate.size()  +" Domain ID: " +str.getlocalDomainID());
                        log.debug("Before InterDomain TEDB Size: " +interDomainLinkUpdate.size());


                        if (multiDomainTEDB.getNetworkDomainGraph().containsVertex(str.getlocalDomainID()) && multiDomainTEDB.getNetworkDomainGraph().containsVertex(str.getremoteDomainID())) {
                            log.debug("InterDomain TEDB contains link: " +multiDomainTEDB.getNetworkDomainGraph().edgesOf(str.getlocalDomainID())    +"<---->"    +multiDomainTEDB.getNetworkDomainGraph().edgesOf(str.getlocalDomainID()) );
                            multiDomainTEDB.getNetworkDomainGraph().removeEdge(str.getlocalDomainID(), str.getremoteDomainID());
                            interDomainLinkUpdate.remove(str);
                        }
                        log.debug("After InterDomain TEDB Size: " +interDomainLinkUpdate.size());
                    }
                    else{
                                                     //------The Domain is Recently updated------//
                       // log.debug("------Else Case   InterDomainLink:  " +str.toString()   +" Time Stored: " + interDomainLinkUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+ interDomainLinkUpdate.get(str)) + " Current time: " +System.currentTimeMillis());
                    }
                } finally {
                   // log.debug("Finalize!!!");
                }

            }



        }

                                 //-------------- Check the Node IT Information ----------------//


        if(nodeITinfoUpdate.size()!=0){

            log.debug("...........Node IT Information TEDS Size: " +nodeITinfoUpdate.size());


            Enumeration nodeID = nodeITinfoUpdate.keys();
            while(nodeID.hasMoreElements())
            {
                NodeITinfoUpdateTime str= (NodeITinfoUpdateTime) nodeID.nextElement();
                log.debug("-------------Domain ID: " +str.toString() );
                Long timeToCheck = nodeITinfoUpdate.get(str) + param.getBGPupdateTime();
                try {
                    if(System.currentTimeMillis()>timeToCheck)
                    {
                        //-----The Node IT Information is Not Updated Recently
                        log.debug("Node ID:  " +str.getNodeID()   +" Time Stored: " + nodeITinfoUpdate.get(str) + " Time to Refresh : " +(param.getBGPupdateTime()+nodeITinfoUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                        DomainTEDB domainTEDB=(DomainTEDB)intraTEDBs.get(str.getlocalDomainID().getHostAddress());
                        SimpleTEDB simpleTEDBxx=null;
                        simpleTEDBxx = (SimpleTEDB) domainTEDB;
                        if(!simpleTEDBxx.getItResources().equals(null)) {
                            log.debug("Node IT Information Size: " + nodeITinfoUpdate.size() + " Domain ID: " + str.getlocalDomainID());
                            log.debug("Before Node IT Info TEDB Size: " + nodeITinfoUpdate.size());
                            log.debug("IT Information Size: " + simpleTEDBxx.getItResources());
                            simpleTEDBxx.setItResources(null);
                            nodeITinfoUpdate.remove(str);
                            log.debug("After Node IT Info TEDB Size: " + nodeITinfoUpdate.size());
                        }
                    }
                    else{
                        //------The Domain is Recently updated------//
                       // log.info("------else Case------    Domain ID:  " +str   +" Time Stored: " + nodeITinfoUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+nodeITinfoUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                    }
                } finally {
                   // log.info("Finalize!!!");
                }

            }
        }

                                //----------------- Check the Node Information -------------------//


        if(nodeinfoUpdate.size()!=0)
        {
            log.debug("...........Node Information TEDS Size: " +nodeinfoUpdate.size());


            Enumeration nodeID = nodeinfoUpdate.keys();
            while(nodeID.hasMoreElements())
            {
                NodeinfoUpdateTime str= (NodeinfoUpdateTime) nodeID.nextElement();
                log.debug("Node Information Update Domain ID: " +str.toString() );
                Long timeToCheck = nodeinfoUpdate.get(str) + param.getBGPupdateTime();
                try {
                    if(System.currentTimeMillis()>timeToCheck)
                    {
                                     //-----The Node IT Information is Not Updated Recently
                        log.debug("Node ID:  " +str   +" Time Stored: " + nodeinfoUpdate.get(str) + " Time to Refresh : " +(param.getBGPupdateTime()+ nodeinfoUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                        DomainTEDB domainTEDB=(DomainTEDB)intraTEDBs.get(str.getlocalDomainID().getHostAddress());
                        SimpleTEDB simpleTEDBxx=null;
                        simpleTEDBxx = (SimpleTEDB) domainTEDB;
                        if(!simpleTEDBxx.getNodeTable().equals(null) && simpleTEDBxx.getNodeTable().containsKey(str.getNodeID())) {
                            log.debug("Node Information Size: " + nodeinfoUpdate.size() + " Domain ID: " + nodeinfoUpdate.get(str));
                            log.debug("Node Table Size: " + simpleTEDBxx.getNodeTable() +"Local Node ID: " +str.getNodeID());
                            simpleTEDBxx.getNodeTable().remove(str.getNodeID());
                            nodeinfoUpdate.remove(str);
                            log.debug("After Node Table Size: " + simpleTEDBxx.getNodeTable().size() +"Node information Size: " +nodeinfoUpdate.size());
                        }
                    }
                    else{
                                                     //------The Domain is Recently updated------//
                       // log.info("------else Case------    Local Node:  " +str.getNodeID()   +" Time Stored: " + nodeinfoUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+ nodeinfoUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                    }
                } finally {
                   // log.info("Finalize!!!");
                }

            }
        }




                            //--------------------Check the Updated time for Domains-------------------//

        if(domainUpdate.size()!=0)
        {
            log.debug("------------- Stored Domains Size: " +domainUpdate.size() );

            Enumeration domainID = domainUpdate.keys();
            //Enumeration intraTED = intraTEDBs.keys();

            /*for(Object o: intraTEDBs.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                log.info(" Key for IntraTEDs:  " +entry.getKey());
            }*/
            while(domainID.hasMoreElements())
            {
                DomainUpdateTime str = (DomainUpdateTime) domainID.nextElement();
                log.debug("Domain ID For Checking: " +str.getlocalDomainID().getHostAddress());
                Long timeToCheck= domainUpdate.get(str) + param.getBGPupdateTime();
                try {
                    if(System.currentTimeMillis()>timeToCheck) {
                                     //-----------Domain Information is Not Updated Recently-------------//

                        for (Object o : intraTEDBs.entrySet()) {
                            Map.Entry entry = (Map.Entry) o;
                            log.debug(" Key for IntraTEDs:  " + entry.getKey());
                            if (entry.getKey().equals(str.getlocalDomainID().getHostAddress())) {
                                log.debug("Both Keys Match !!!!");
                                log.debug("DomainID:  " + str.getlocalDomainID() + " Time Stored: " + domainUpdate.get(str) + " Time to Refresh: " + (param.getBGPupdateTime() + domainUpdate.get(str)) + " Current time: " + System.currentTimeMillis());
                                log.debug("Domain TEDB Size: " + intraTEDBs.size() + " Domain ID: " + intraTEDBs.get(entry.getKey()));
                                log.debug("Before IntraDomain TEDB Size: " + intraTEDBs.size());
                                domainUpdate.remove(str);
                                intraTEDBs.remove(entry.getKey());
                                log.debug("After IntraDomain TEDB Size: " + intraTEDBs.size() + "  DomainUpdate Size: " + domainUpdate.size());
                           break;
                            }
                        }
                    }
                    else{
                                                   //------The Domain is Recently updated------//
                       // log.debug("------else case------  DomainID:  " +str   +" Time Stored: " + domainUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+domainUpdate.get(str)) + " Current time: " +System.currentTimeMillis());

                    }
                } finally {
                   // log.debug("Finalize!!!");
                }
            }
        }



    //--------------------Check the Updated time for MDPCE-------------------//

        if(MDPCEinfoUpdate.size()!=0)
    {
        log.debug("------------- Stored Domains Size: " +MDPCEinfoUpdate.size() );

        Enumeration domainID = MDPCEinfoUpdate.keys();

        while(domainID.hasMoreElements())
        {
            MDPCEinfoUpdateTime str = (MDPCEinfoUpdateTime) domainID.nextElement();
            Long timeToCheck= MDPCEinfoUpdate.get(str) + param.getBGPupdateTime();
            try {
                if(System.currentTimeMillis()>timeToCheck) {
                    //-----------Domain Information is Not Updated Recently-------------//

                    if(str.getlocalDomains().size()!=0) {
                        for (Inet4Address Dom : str.getlocalDomains()) {
                            SimpleTEDB simpleTEDB = null;
                            DomainTEDB domainTEDB = null;
                            domainTEDB = (DomainTEDB) intraTEDBs.get(Dom.getHostAddress());
                            if (domainTEDB instanceof SimpleTEDB) {
                                simpleTEDB = (SimpleTEDB) domainTEDB;
                                simpleTEDB.getMDPCE().setPCEipv4(null);
                                simpleTEDB.getMDPCE().setLearntFrom(null);
                                simpleTEDB.setLocalDomains(null);
                                simpleTEDB.setLocalASs(null);
                                simpleTEDB.setNeighASs(null);
                                simpleTEDB.setNeighDomains(null);
                                simpleTEDB.setDomainID(null);
                                simpleTEDB.setPCEScope(null);
                                simpleTEDB.setMDPCE(null);
                            }
                        }
                    }
                MDPCEinfoUpdate.remove(str);
                }

                else{
                    //------The Domain is Recently updated------//
                   // log.debug("------else case------    DomainID:  " +str   +" Time Stored: " + MDPCEinfoUpdate.get(str) + " Time to Refresh: " +(param.getBGPupdateTime()+MDPCEinfoUpdate.get(str)) + " Current time: " +System.currentTimeMillis());
                }
            } finally {
              //  log.debug("Finalize!!!");
            }
        }
    }
}

}





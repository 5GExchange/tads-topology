package es.tid.bgp.bgp4Peer.peer;

import es.tid.bgp.bgp4Peer.updateTEDB.CMS_Domain_Msgs;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;


/**
 * Created by Ajmal on 2017-09-27.
 */
public class CMSPostMsgs implements Runnable {
    private Logger log;
    private LinkedList<CMS_Domain_Msgs> cms_msg;
    private int Domainid;
    private Create_CMS_Post create_post ;
    private Boolean localDomain= false;
    Hashtable<String, TEDB> intraTEDBs;
    Hashtable<String, Boolean> sent=new Hashtable<>();
    String localID;

    String entry="";

    public void configure(LinkedList<CMS_Domain_Msgs> cms_msg, Hashtable<String, TEDB> iTEDBs, String ID)
    {

        log = LoggerFactory.getLogger("BGP4Peer");
        this.cms_msg=cms_msg;
        this.intraTEDBs=iTEDBs;
        this.localID=ID;

    }

    @Override
    public void run() {

        log.info("executing Post of the message to CMS");

        Enumeration names;
        String key;


        names =  this.intraTEDBs.keys();
        while(names.hasMoreElements()) {
            key = (String) names.nextElement();
            if (key!="multidomin"){
                if (!sent.containsKey(key)||(sent.get(key)==false)){

                    SimpleTEDB ted=(SimpleTEDB)intraTEDBs.get(key);
                    entry=ted.getItResources().getControllerIT();
                    log.info("New domain detected: " +entry);
                    if(entry!=null&&!entry.equals("")){
                        log.info("New domain detected: " +key);
                        log.info(localID);
                        log.info(ted.getItResources().getLearntFrom());
                        if(ted.getItResources().getLearntFrom().equals(localID));
                            localDomain=true;
                        create_post= new Create_CMS_Post(entry, key, localDomain);
                        sent.put(key,true);

                    }


                }
            }
            else
                log.info("CMS: it is multidomain intra-domain");
        }



    }
}

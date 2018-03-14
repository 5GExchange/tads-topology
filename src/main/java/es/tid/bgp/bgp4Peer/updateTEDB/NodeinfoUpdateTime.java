package es.tid.bgp.bgp4Peer.updateTEDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;

/**
 * Created by Ajmal on 2017-08-08.
 */
public class NodeinfoUpdateTime {

    Inet4Address localDomainID;
    Inet4Address localnodeID;
    long localISISid=0;
    Long NodeupdateTime;
    String LearntFrom=null;
    private Logger log;


public NodeinfoUpdateTime(Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate, Inet4Address localDomainID, Inet4Address localnodeID, Long NodeupdateTime){

    this.localDomainID=localDomainID;
    this.localnodeID=localnodeID;
    this.NodeupdateTime=NodeupdateTime;
    log= LoggerFactory.getLogger("BGP4Peer");

    if(nodeinfoUpdate.size()==0)
        nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localnodeID),NodeupdateTime);

    else{
        int indicator=0;
        NodeinfoUpdateTime key;
        Enumeration Node_ID =nodeinfoUpdate.keys();
        while(Node_ID.hasMoreElements()) {
            key = (NodeinfoUpdateTime) Node_ID.nextElement();
            if(key.equals(new NodeinfoUpdateTime(localDomainID,localnodeID)))
            {
                nodeinfoUpdate.remove(key);
                nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localnodeID), NodeupdateTime);
                log.info("Node Info Update Match Found " +key.toString() +"   with: " +(new NodeinfoUpdateTime(localDomainID,localnodeID).toString()));
                indicator++;
                break;
            }
        }
        if(indicator==0)
            nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localnodeID),NodeupdateTime);
    }
}

public NodeinfoUpdateTime(Hashtable<NodeinfoUpdateTime, Long> nodeinfoUpdate, Inet4Address localDomainID, long id, Long NodeupdateTime){

        this.localDomainID=localDomainID;
        this.localISISid=id;
        this.NodeupdateTime=NodeupdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");

        if(nodeinfoUpdate.size()==0)
            nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localISISid),NodeupdateTime);

        else{
            int indicator=0;
            NodeinfoUpdateTime key;
            Enumeration Node_ID =nodeinfoUpdate.keys();
            while(Node_ID.hasMoreElements()) {
                key = (NodeinfoUpdateTime) Node_ID.nextElement();
                if(key.equals(new NodeinfoUpdateTime(localDomainID,localISISid)))
                {
                    nodeinfoUpdate.remove(key);
                    nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localISISid), NodeupdateTime);
                    log.info("Node Info Update Match Found " +key.toString() +"   with: " +(new NodeinfoUpdateTime(localDomainID,localISISid).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                nodeinfoUpdate.put(new NodeinfoUpdateTime(localDomainID, localISISid),NodeupdateTime);
        }
    }




public NodeinfoUpdateTime(Inet4Address localDomainID, Inet4Address localnodeID)
{
    this.localDomainID=localDomainID;
    this.localnodeID=localnodeID;
}

public NodeinfoUpdateTime(Inet4Address localDomainID, long id) {
        this.localDomainID=localDomainID;
        this.localISISid=id;
}


public boolean equals (Object o) {


    if (o == this)
        return true;
    if (!(o instanceof NodeinfoUpdateTime)) {

        return false;
    }
    NodeinfoUpdateTime UpdateTime = (NodeinfoUpdateTime) o;

    return Objects.equals(localDomainID, UpdateTime.localDomainID) && Objects.equals(localnodeID, UpdateTime.localnodeID);
}

    public String toString()
    {
        String ret= "Domain ID: "+this.localDomainID +"<--->" +"Node ID: " +this.localnodeID;
        return ret;
    }

    public void setlearntfrom (String learntfrom){

        this.LearntFrom=learntfrom;
    }

    public Inet4Address getlocalDomainID(){

        return this.localDomainID;
    }

    public Inet4Address getlocalnodeID(){

    return this.localnodeID;
    }

    public String getlearntfrom (){

        return this.LearntFrom;
    }

    public Inet4Address getNodeID(){

        return this.localnodeID;
    }

}

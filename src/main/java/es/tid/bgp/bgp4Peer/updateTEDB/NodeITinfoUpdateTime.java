package es.tid.bgp.bgp4Peer.updateTEDB;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by Ajmal on 2017-08-08.
 */
public class NodeITinfoUpdateTime {

    Inet4Address localDomainID;
    String nodeId;
    String LearntFrom=null;
    Long UpdateTime;

    private Logger log;


public NodeITinfoUpdateTime(Hashtable<NodeITinfoUpdateTime, Long> nodeITinfoUpdate, Inet4Address localDomainID, String nodeId, Long UpdateTime){

    this.localDomainID=localDomainID;
    this.nodeId=nodeId;
    this.UpdateTime=UpdateTime;

    log= LoggerFactory.getLogger("BGP4Peer");


    if(nodeITinfoUpdate.size()==0) {
        nodeITinfoUpdate.put(new NodeITinfoUpdateTime(localDomainID, nodeId), UpdateTime);
    }
    else
    {
        int indicator=0;
      NodeITinfoUpdateTime key;
        Enumeration node_ID =nodeITinfoUpdate.keys();
       while(node_ID.hasMoreElements()) {
           key = (NodeITinfoUpdateTime) node_ID.nextElement();
           if(key.equals(new NodeITinfoUpdateTime( localDomainID, nodeId)))
           {
               nodeITinfoUpdate.remove(key);
               nodeITinfoUpdate.put(new NodeITinfoUpdateTime(localDomainID, nodeId), UpdateTime);
               log.debug("Node IT Info update Match Found " +key.toString() +"   with: " +(new NodeITinfoUpdateTime(localDomainID, nodeId).toString()));
               indicator++;
               break;
           }
       }
       if(indicator==0)
           nodeITinfoUpdate.put(new NodeITinfoUpdateTime(localDomainID, nodeId), UpdateTime);
    }
}

    public NodeITinfoUpdateTime( Inet4Address localDomainID, String nodeId){

        this.localDomainID=localDomainID;
        this.nodeId=nodeId;
    }



    public boolean equals (Object o) {


    if (o == this)
        return true;
    if (!(o instanceof NodeITinfoUpdateTime)) {

        return false;
    }
    NodeITinfoUpdateTime UpdateTime = (NodeITinfoUpdateTime) o;

    return new EqualsBuilder().append(localDomainID, UpdateTime.localDomainID).append(nodeId, UpdateTime.nodeId).isEquals();


    //return Objects.equals(localDomainID, UpdateTime.localDomainID) && Objects.equals(nodeId, UpdateTime.nodeId);
}

    public String toString()
    {
        String ret= "Domain ID: "+this.localDomainID +"<--->" +"Node ID: " +this.nodeId;
        return ret;
    }

    public void setlearntfrom (String learntfrom){

        this.LearntFrom=learntfrom;
    }


    public Inet4Address getlocalDomainID(){

    return this.localDomainID;
    }


    public String getlearntfrom (){

        return this.LearntFrom;
    }

    public String getNodeID(){

        return this.nodeId;
    }


}

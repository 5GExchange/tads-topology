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
public class IntraDomainLinkUpdateTime {

    Inet4Address localDomainID;
    Inet4Address LocalNodeIGPId;
    Inet4Address RemoteNodeIGPId;
    int localISISid=0;
    int remoteISISid=0;
    Long LocalIdentifier;
    Long RemoteIdentifier;
    Long linkUpdateTime;
    String LearntFrom;
    private Logger log;


public IntraDomainLinkUpdateTime(Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate, Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Long LocalIdentifier, Inet4Address RemoteNodeIGPId, Long RemoteIdentifier, long linkUpdateTime){

    this.localDomainID=localDomainID;
    this.LocalNodeIGPId=LocalNodeIGPId;
    this.RemoteNodeIGPId=RemoteNodeIGPId;
    this.LocalIdentifier= LocalIdentifier;
    this.RemoteIdentifier=RemoteIdentifier;
    this.linkUpdateTime=linkUpdateTime;
    log= LoggerFactory.getLogger("BGP4Peer");


    if(intraDomainLinkUpdate.size()==0)
        intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier), linkUpdateTime);
    else{

        int indicator=0;
        IntraDomainLinkUpdateTime key;
        Enumeration link_ID =intraDomainLinkUpdate.keys();
        while(link_ID.hasMoreElements()) {
            key = (IntraDomainLinkUpdateTime) link_ID.nextElement();
            if(key.equals(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier)))
            {
                intraDomainLinkUpdate.remove(key);
                intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId,RemoteIdentifier), linkUpdateTime);
                log.info("Intra-Domain Link Match Found " +key.toString() +"   with: " +(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier).toString()));
                indicator++;
                break;
            }
        }
        if(indicator==0)
            intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier), linkUpdateTime);
    }
}

public IntraDomainLinkUpdateTime(Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Long LocalIdentifier, Inet4Address RemoteNodeIGPId, Long RemoteIdentifier)
{
    this.localDomainID=localDomainID;
    this.LocalNodeIGPId=LocalNodeIGPId;
    this.RemoteNodeIGPId=RemoteNodeIGPId;
    this.LocalIdentifier= LocalIdentifier;
    this.RemoteIdentifier=RemoteIdentifier;

}

public IntraDomainLinkUpdateTime(Hashtable<IntraDomainLinkUpdateTime, Long> intraDomainLinkUpdate, Inet4Address localDomainID, int LocalNodeIGPId, Long LocalIdentifier, int RemoteNodeIGPId, Long RemoteIdentifier, long linkUpdateTime){

        this.localDomainID=localDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.LocalIdentifier= LocalIdentifier;
        this.RemoteIdentifier=RemoteIdentifier;
        this.linkUpdateTime=linkUpdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");


        if(intraDomainLinkUpdate.size()==0)
            intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier), linkUpdateTime);
        else{

            int indicator=0;
            IntraDomainLinkUpdateTime key;
            Enumeration link_ID =intraDomainLinkUpdate.keys();
            while(link_ID.hasMoreElements()) {
                key = (IntraDomainLinkUpdateTime) link_ID.nextElement();
                if(key.equals(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier)))
                {
                    intraDomainLinkUpdate.remove(key);
                    intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId,RemoteIdentifier), linkUpdateTime);
                    log.info("Intra-Domain Link Match Found " +key.toString() +"   with: " +(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                intraDomainLinkUpdate.put(new IntraDomainLinkUpdateTime(localDomainID,LocalNodeIGPId,LocalIdentifier,RemoteNodeIGPId ,RemoteIdentifier), linkUpdateTime);
        }
}

public IntraDomainLinkUpdateTime(Inet4Address localDomainID, int LocalNodeIGPId, Long LocalIdentifier, int RemoteNodeIGPId, Long RemoteIdentifier)
{
        this.localDomainID=localDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.LocalIdentifier= LocalIdentifier;
        this.RemoteIdentifier=RemoteIdentifier;

}

public boolean equals (Object o) {


    if (o == this)
        return true;
    if (!(o instanceof IntraDomainLinkUpdateTime)) {

        return false;
    }
    IntraDomainLinkUpdateTime UpdateTime = (IntraDomainLinkUpdateTime) o;

    return Objects.equals(localDomainID, UpdateTime.localDomainID) && Objects.equals(LocalNodeIGPId, UpdateTime.LocalNodeIGPId) && Objects.equals(LocalIdentifier, UpdateTime.LocalIdentifier) && Objects.equals(RemoteNodeIGPId, UpdateTime.RemoteNodeIGPId) && Objects.equals(RemoteIdentifier, UpdateTime.RemoteIdentifier);
}

    public String toString()
    {
        String ret= this.localDomainID.getHostAddress() +"<--->" +this.LocalNodeIGPId.getHostAddress() +"<--->" +this.LocalIdentifier +"<--->"
                +this.RemoteNodeIGPId.getHostAddress() +"<--->"  +RemoteIdentifier;
        return ret;
    }



    public Inet4Address getlocalDomainID(){

    return this.localDomainID;
    }

    public Inet4Address getLocalNodeIGPId(){

        return this.LocalNodeIGPId;
    }

    public Inet4Address getRemoteNodeIGPId(){

        return this.RemoteNodeIGPId;
    }

    public Long getLocalIdentifier(){

        return this.LocalIdentifier;
    }

    public Long getRemoteIdentifier(){

        return this.RemoteIdentifier;
    }


    public void setlearntfrom (String learntfrom){

        this.LearntFrom=learntfrom;
    }


    public String getlearntfrom (){

        return this.LearntFrom;
    }








}

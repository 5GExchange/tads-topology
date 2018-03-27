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
public class InterDomainLinkUpdateTime {

    Inet4Address localDomainID;
    Inet4Address remoteDomainID;
    Inet4Address LocalNodeIGPId;
    Inet4Address RemoteNodeIGPId;
    Inet4Address localIf;
    Inet4Address remoteIf;
    long localISISid=0;
    long remoteISISid=0;
    Long LocalIdentifier;
    Long RemoteIdentifier;
    Long LinkUpdateTime;
    String LearntFrom=null;
    private Logger log;

public InterDomainLinkUpdateTime(Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Long LocalIdentifier, Inet4Address remoteDomainID, Inet4Address RemoteNodeIGPId, Long RemoteIdentifier, Long LinkUpdateTime){

    this.localDomainID=localDomainID;
    this.remoteDomainID=remoteDomainID;
    this.LocalNodeIGPId=LocalNodeIGPId;
    this.RemoteNodeIGPId=RemoteNodeIGPId;
    this.LocalIdentifier= LocalIdentifier;
    this.RemoteIdentifier=RemoteIdentifier;
    this.LinkUpdateTime= LinkUpdateTime;
    log= LoggerFactory.getLogger("BGP4Peer");

    if(interDomainLinkUpdate.size()==0)
        interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
    else
        {
            int indicator=0;
            InterDomainLinkUpdateTime key;
            Enumeration link_ID =interDomainLinkUpdate.keys();
            while(link_ID.hasMoreElements()) {
                key = (InterDomainLinkUpdateTime) link_ID.nextElement();
                if(key.equals(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier)))
                {
                    interDomainLinkUpdate.remove(key);
                    interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
                    log.debug("Inter-Domain Link Match Found " +key.toString() +"   with: " +(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        }
}

    public InterDomainLinkUpdateTime(Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Inet4Address LocalIdentifier, Inet4Address remoteDomainID, Inet4Address RemoteNodeIGPId, Inet4Address RemoteIdentifier, Long LinkUpdateTime){

        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.LocalNodeIGPId=LocalNodeIGPId;
        this.RemoteNodeIGPId=RemoteNodeIGPId;
        this.localIf= LocalIdentifier;
        this.remoteIf=RemoteIdentifier;
        this.LinkUpdateTime= LinkUpdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");

        if(interDomainLinkUpdate.size()==0)
            interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        else
        {
            int indicator=0;
            InterDomainLinkUpdateTime key;
            Enumeration link_ID =interDomainLinkUpdate.keys();
            while(link_ID.hasMoreElements()) {
                key = (InterDomainLinkUpdateTime) link_ID.nextElement();
                if(key.equals(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier)))
                {
                    interDomainLinkUpdate.remove(key);
                    interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
                    log.debug("Inter-Domain Link Match Found " +key.toString() +"   with: " +(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        }
    }



    public InterDomainLinkUpdateTime(Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Inet4Address localDomainID, long LocalNodeIGPId, Long LocalIdentifier, Inet4Address remoteDomainID, long RemoteNodeIGPId, Long RemoteIdentifier, Long LinkUpdateTime){

        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.LocalIdentifier= LocalIdentifier;
        this.RemoteIdentifier=RemoteIdentifier;
        this.LinkUpdateTime= LinkUpdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");

        if(interDomainLinkUpdate.size()==0)
            interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        else
        {
            int indicator=0;
            InterDomainLinkUpdateTime key;
            Enumeration link_ID =interDomainLinkUpdate.keys();
            while(link_ID.hasMoreElements()) {
                key = (InterDomainLinkUpdateTime) link_ID.nextElement();
                if(key.equals(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier)))
                {
                    interDomainLinkUpdate.remove(key);
                    interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
                    log.debug("Inter-Domain Link Match Found " +key.toString() +"   with: " +(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        }
    }


    public InterDomainLinkUpdateTime(Hashtable<InterDomainLinkUpdateTime, Long> interDomainLinkUpdate, Inet4Address localDomainID, long LocalNodeIGPId, Inet4Address LocalIdentifier, Inet4Address remoteDomainID, long RemoteNodeIGPId, Inet4Address RemoteIdentifier, Long LinkUpdateTime){

        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.localIf= LocalIdentifier;
        this.remoteIf=RemoteIdentifier;
        this.LinkUpdateTime= LinkUpdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");

        if(interDomainLinkUpdate.size()==0)
            interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        else
        {
            int indicator=0;
            InterDomainLinkUpdateTime key;
            Enumeration link_ID =interDomainLinkUpdate.keys();
            while(link_ID.hasMoreElements()) {
                key = (InterDomainLinkUpdateTime) link_ID.nextElement();
                if(key.equals(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier)))
                {
                    interDomainLinkUpdate.remove(key);
                    interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
                    log.debug("Inter-Domain Link Match Found " +key.toString() +"   with: " +(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier).toString()));
                    indicator++;
                    break;
                }
            }
            if(indicator==0)
                interDomainLinkUpdate.put(new InterDomainLinkUpdateTime(localDomainID, LocalNodeIGPId, LocalIdentifier, remoteDomainID, RemoteNodeIGPId, RemoteIdentifier), LinkUpdateTime);
        }
    }


    public InterDomainLinkUpdateTime(Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Long LocalIdentifier, Inet4Address remoteDomainID, Inet4Address RemoteNodeIGPId, Long RemoteIdentifier)
{
    this.localDomainID=localDomainID;
    this.remoteDomainID=remoteDomainID;
    this.LocalNodeIGPId=LocalNodeIGPId;
    this.RemoteNodeIGPId=RemoteNodeIGPId;
    this.LocalIdentifier=LocalIdentifier;
    this.RemoteIdentifier=RemoteIdentifier;
}


    public InterDomainLinkUpdateTime(Inet4Address localDomainID, Inet4Address LocalNodeIGPId, Inet4Address LocalIdentifier, Inet4Address remoteDomainID, Inet4Address RemoteNodeIGPId, Inet4Address RemoteIdentifier)
    {
        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.LocalNodeIGPId=LocalNodeIGPId;
        this.RemoteNodeIGPId=RemoteNodeIGPId;
        this.localIf=LocalIdentifier;
        this.remoteIf=RemoteIdentifier;
    }

public InterDomainLinkUpdateTime(Inet4Address localDomainID, long LocalNodeIGPId, Long LocalIdentifier, Inet4Address remoteDomainID, long RemoteNodeIGPId, Long RemoteIdentifier) {
        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.LocalIdentifier=LocalIdentifier;
        this.RemoteIdentifier=RemoteIdentifier;
}

public InterDomainLinkUpdateTime(Inet4Address localDomainID, long LocalNodeIGPId, Inet4Address LocalIdentifier, Inet4Address remoteDomainID, long RemoteNodeIGPId, Inet4Address RemoteIdentifier)
    {
        this.localDomainID=localDomainID;
        this.remoteDomainID=remoteDomainID;
        this.localISISid=LocalNodeIGPId;
        this.remoteISISid=RemoteNodeIGPId;
        this.localIf=LocalIdentifier;
        this.remoteIf=RemoteIdentifier;
    }

public boolean equals (Object o) {


    if (o == this)
        return true;
    if (!(o instanceof InterDomainLinkUpdateTime)) {

        return false;
    }
    InterDomainLinkUpdateTime UpdateTime = (InterDomainLinkUpdateTime) o;

    return (Objects.equals(localDomainID, UpdateTime.localDomainID) && Objects.equals(LocalNodeIGPId, UpdateTime.LocalNodeIGPId) &&
            Objects.equals(LocalIdentifier, UpdateTime.LocalIdentifier) && Objects.equals(remoteDomainID, UpdateTime.remoteDomainID) &&
            Objects.equals(RemoteNodeIGPId, UpdateTime.RemoteNodeIGPId) && Objects.equals(RemoteIdentifier, UpdateTime.RemoteIdentifier)&&
            Objects.equals(localIf, UpdateTime.localIf)&& Objects.equals(remoteIf, UpdateTime.remoteIf));
}

    public String toString()
    {
        String ret=null;
        if (getLocalNodeIGPId()!=null){
            ret= this.localDomainID.getHostAddress() +"<--->" +this.LocalNodeIGPId.getHostAddress() +"<--->" +this.LocalIdentifier +"<--->"
                    +this.remoteDomainID.getHostAddress() +"<--->" +this.RemoteNodeIGPId.getHostAddress() +"<--->"  +RemoteIdentifier;

        }
        else{
            if (localISISid!=0){
                ret= this.localDomainID.getHostAddress() +"<--->" +this.localISISid +"<--->" +this.localIf.getHostAddress() +"<--->"
                        +this.remoteDomainID.getHostAddress() +"<--->" +this.remoteISISid +"<--->"  +this.remoteIf.getHostAddress();

            }

        }
        return ret;
    }



    public Inet4Address getlocalDomainID(){

    return this.localDomainID;
    }

    public Inet4Address getremoteDomainID(){

        return this.remoteDomainID;
    }


    public Inet4Address getLocalNodeIGPId(){

        return this.LocalNodeIGPId;
    }

    public Inet4Address getRemoteNodeIGPId(){

        return this.RemoteNodeIGPId;
    }

    public Long getLinkLocalIdentifier(){

        return this.LocalIdentifier;
    }

    public Long getLinkRemoteIdentifier(){

        return this.RemoteIdentifier;
    }


    public void setlearntfrom (String learntfrom){

        this.LearntFrom=learntfrom;
    }


    public String getlearntfrom (){

        return this.LearntFrom;
    }







}

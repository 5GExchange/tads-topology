package es.tid.bgp.bgp4Peer.peer;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Hashtable;
/**
 * Created by Ajmal on 2017-08-10.
 */
public class DomainUpdateTime {
 Logger log;
 Inet4Address localDomainID;
 Long DomainUpdatetime;
 String LearntFrom=null;



  public DomainUpdateTime(Hashtable<DomainUpdateTime, Long> DomainUpdate, Inet4Address localDomainID, Long DomainUpdatetime)
  {

      this.localDomainID=localDomainID;
      this.DomainUpdatetime=DomainUpdatetime;
      log= LoggerFactory.getLogger("BGP4Peer");

      if(DomainUpdate.size()==0)
          DomainUpdate.put(new DomainUpdateTime(localDomainID),DomainUpdatetime);
      else
      {
          int indicator=0;
          DomainUpdateTime key;
          Enumeration node_ID =DomainUpdate.keys();
          while(node_ID.hasMoreElements()) {
              key = (DomainUpdateTime) node_ID.nextElement();
              if(key.equals(new DomainUpdateTime(localDomainID)))
              {
                  DomainUpdate.remove(key);
                  DomainUpdate.put(new DomainUpdateTime(localDomainID),DomainUpdatetime);
                  log.debug("Match for Domain Found " +key.toString() +"   with: " +(new DomainUpdateTime(localDomainID).toString()));
                  indicator++;
                  break;
              }
          }
          if(indicator==0)
              DomainUpdate.put(new DomainUpdateTime(localDomainID),DomainUpdatetime);
      }
  }

public DomainUpdateTime(Inet4Address localDomainID){
 this.localDomainID=localDomainID;
}

    public boolean equals (Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DomainUpdateTime)) {

            return false;
        }
        DomainUpdateTime UpdateTime=(DomainUpdateTime) o;
        return new EqualsBuilder().append(localDomainID, UpdateTime.localDomainID).isEquals();
    }
    public String toString()
    {
        String ret= "Domain ID: "+this.localDomainID ;
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
}

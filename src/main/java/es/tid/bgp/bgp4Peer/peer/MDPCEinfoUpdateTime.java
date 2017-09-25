package es.tid.bgp.bgp4Peer.peer;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by Ajmal on 2017-08-09.
 */
public class MDPCEinfoUpdateTime {


    ArrayList<Inet4Address> localDomains;
    Inet4Address PCEip;
    String LearntFrom=null;
    Long UpdateTime;
    Logger log;

    public MDPCEinfoUpdateTime(Hashtable<MDPCEinfoUpdateTime, Long> MDPCEinfoUpdate, ArrayList<Inet4Address> localDomains, Inet4Address PCEip, String LearntFrom, Long UpdateTime){

        this.localDomains=localDomains;
        this.PCEip=PCEip;
        this.LearntFrom= LearntFrom;
        this.UpdateTime= UpdateTime;
        log= LoggerFactory.getLogger("BGP4Peer");
    if(MDPCEinfoUpdate.size()==0)
        MDPCEinfoUpdate.put(new MDPCEinfoUpdateTime(localDomains, PCEip), UpdateTime);
    else{
        int indicator=0;
        MDPCEinfoUpdateTime key;
        Enumeration MDPCE_ID =MDPCEinfoUpdate.keys();
        while(MDPCE_ID.hasMoreElements()) {
            key = (MDPCEinfoUpdateTime) MDPCE_ID.nextElement();
            if(key.equals(new MDPCEinfoUpdateTime(localDomains, PCEip)))
            {
                MDPCEinfoUpdate.remove(key);
                MDPCEinfoUpdate.put(new MDPCEinfoUpdateTime(localDomains, PCEip), UpdateTime);
                log.info("Node Info Update Match Found " +key.toString() +"   with: " +(new MDPCEinfoUpdateTime(localDomains, PCEip).toString()));
                indicator++;
                break;
            }
        }
             if(indicator==0)
            MDPCEinfoUpdate.put(new MDPCEinfoUpdateTime(localDomains, PCEip),UpdateTime);
    }


    }

    public MDPCEinfoUpdateTime(ArrayList<Inet4Address> localDomains, Inet4Address PCEip) {
   this.localDomains=localDomains;
   this.PCEip=PCEip;
    }


    public boolean equals (Object o) {


        if (o == this)
            return true;
        if (!(o instanceof MDPCEinfoUpdateTime)) {

            return false;
        }
        MDPCEinfoUpdateTime UpdateTime = (MDPCEinfoUpdateTime) o;

        return Objects.equals(localDomains, UpdateTime.localDomains) && Objects.equals(PCEip, UpdateTime.PCEip);
    }

    public String toString()
    {
        String ret= "Domain ID: "+this.localDomains +"<--->" +"Node ID: " +this.PCEip;
        return ret;
    }






    public void setlearntfrom (String learntfrom){

        this.LearntFrom=learntfrom;
    }



    public ArrayList<Inet4Address> getlocalDomains(){

        return this.localDomains;
    }



    public String getlearntfrom (){

        return this.LearntFrom;
    }




    public Inet4Address getPCEip(){

        return this.PCEip;
    }








}

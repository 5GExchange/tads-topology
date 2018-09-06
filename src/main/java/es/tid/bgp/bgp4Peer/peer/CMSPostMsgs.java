package es.tid.bgp.bgp4Peer.peer;

import es.tid.bgp.bgp4Peer.updateTEDB.CMS_Domain_Msgs;
import es.tid.tedb.SimpleTEDB;
import es.tid.tedb.TEDB;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;


/**
 * Created by Andrea.
 */
public class CMSPostMsgs implements Runnable {
    private Logger log;
    private LinkedList<CMS_Domain_Msgs> cms_msg;
    private Create_CMS_Post create_post ;
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

        log.debug("Executing Post of the message to CMS");


        Set<String> keys = this.intraTEDBs.keySet();
        for(String key: keys){
            log.info(key);
            if (!key.equals("multidomain")){
                if (!sent.containsKey(key)||(sent.get(key)==false)){

                    SimpleTEDB ted=(SimpleTEDB)intraTEDBs.get(key);
                    entry=ted.getItResources().getControllerIT();
                    log.debug("Controller IT is: " +entry);
                    if(entry!=null&&!entry.equals("")){
                        log.info("New domain detected: " +key);
                        log.debug(localID);
                        log.debug(ted.getItResources().getLearntFrom());
                        if(ted.getItResources().getLearntFrom().equals(localID)){
                            //create_post= new Create_CMS_Post(entry, key, true);
                            SendPost(entry, key, true);
                        }else{
                            //create_post= new Create_CMS_Post(entry, key, false);
                            SendPost(entry, key, false);
                        }

                        sent.put(key,true);

                    }


                }
                else
                    log.debug("CMS:Post already sent for domain "+ key);
            }
            else
                log.debug("CMS: it is multidomain intra-domain");
        }


    }

    private void SendPost(String entryPoint, String domain, boolean localDomain) {
        String EntryPoint = "";
        if (entryPoint.contains("http://")){
            String[] parts = entryPoint.split("/");
            EntryPoint=parts[2];
        }else
            EntryPoint=entryPoint;
        try {

            URL url = new URL("http://localhost/mdc/mdc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // String input = "{\"domain\":%$d,\"localDomain\":%b \"entryPoint\":%s}" ;
            JSONObject obj = new JSONObject();

            obj.put("domain", domain);
            obj.put("localDomain", localDomain);
            obj.put("EntryPoint", EntryPoint);
            log.info("CMS:"+obj.toJSONString());

            OutputStream os = conn.getOutputStream();
            os.write(obj.toString().getBytes());
            os.flush();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            log.info("CMS:Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                log.info(output);
            }

            conn.disconnect();

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}

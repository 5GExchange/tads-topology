package es.tid.bgp.bgp4Peer.peer;

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

/**
 * Created by Ajmal on 2017-09-27.
 */


public class Create_CMS_Post {

    private Logger log;
    String EntryPoint;
    String domain;
    boolean localDomain=false;

    public Create_CMS_Post(String entryPoint, String domain, boolean localDomain) {

        this.EntryPoint=EntryPoint;
        this.domain=domain;
        this.localDomain=localDomain;
        log = LoggerFactory.getLogger("BGP4Peer");
        try {

            URL url = new URL("http://localhost/mdc/mdc");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

           // String input = "{\"domain\":%$d,\"localDomain\":%b \"entryPoint\":%s}" ;
            JSONObject obj = new JSONObject();

            obj.put("domain", this.domain);
            obj.put("localDomain", this.localDomain);
            obj.put("EntryPoint", this.EntryPoint);
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


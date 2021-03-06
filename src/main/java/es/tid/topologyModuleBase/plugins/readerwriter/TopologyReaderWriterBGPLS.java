package es.tid.topologyModuleBase.plugins.readerwriter;

import es.tid.bgp.bgp4Peer.peer.BGPPeer;
import es.tid.topologyModuleBase.TopologyModuleParams;
import es.tid.topologyModuleBase.database.TopologiesDataBase;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

/**
 * Created by <a href="mailto:jgm1986@hotmail.com">Javier Gusano Martinez</a> on 23/09/2016.
 */
public class TopologyReaderWriterBGPLS extends TopologyReaderWriter{
    /**
     * This flag is used for analize if the current thread is running.
     */
    private boolean isRunning;

    /**
     * Class constructor.
     * @// TODO: 23/09/2016 Write javadoc constructor fields description.
     * @param ted
     * @param actualLittleParams
     * @param lock
     */
    public TopologyReaderWriterBGPLS(TopologiesDataBase ted, TopologyModuleParams actualLittleParams, Lock lock) {
        // @// TODO: 23/09/2016 Develop current constructor.
        super(ted, actualLittleParams,lock);
    }

    /**
     * Read/Write topology BGPLS
     * @// TODO: 23/09/2016 Write javadoc constructor fields description.
     */
    @Override
    public void readServeTopology() throws IOException {
        // @// TODO: 23/09/2016
        log.info("Acting as BGP Peer");
        BGPPeer bgpPeer = new BGPPeer();
        bgpPeer.configure(params.getBGPSConfigurationFile());

        //log.info("777777777777777777777777777777777777777777777777777num of domain in teds :"+ String.valueOf(ted.getTeds().size()));

        bgpPeer.setIntraTEDBs(ted.getTeds());
        bgpPeer.setMultiDomainTEDB(ted.getMdTed());
        bgpPeer.createUpdateDispatcher();
        log.fine("Testing change");
        bgpPeer.startClient();
        bgpPeer.startServer();
        bgpPeer.startManagementServer();
        Boolean connectedx=Boolean.FALSE;
        while (connectedx!= Boolean.TRUE){
            if (bgpPeer.bgp4SessionsInformation.sessionList.size()>0 || bgpPeer.bgp4SessionsInformation.sessionListByPeerIP.size()>0) {
                bgpPeer.startSendTopology();
                bgpPeer.CMSUpdate();
                connectedx=Boolean.TRUE;
                break;
            }
            else{
                log.info("not yet connected");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Returns true if it's running, false otherwise.
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }


    /**
     * Returns the plugin name.
     */
    @Override
    public String getPluginName() {
        return "BGPLS importer/exporter peer";
    }


    /**
     * Shows the information about the current plugin object.
     */
    @Override
    public String displayInfo() {
        String str=getPluginName()+"\n";
        str+="Status: ";
        if(isRunning())str+="running";
        else str+="stop";
        str+="\nParameters file:"+params.getBGPSConfigurationFile();
        return str;
    }


    /**
     * Launch reader and writer methods.
     */
    @Override
    public void run() {
        try {
            readServeTopology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

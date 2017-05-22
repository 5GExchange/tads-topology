package es.tid.tedb;

import java.net.Inet4Address;

public class PCEInfo {

	private Inet4Address PCEipv4;
	private String learntFrom;
	private String domainID;

	/**
	 * TEDB logger
	 */
	public PCEInfo()
	{
		//initWLANs();
	}

	public Inet4Address getPCEipv4() {
		return PCEipv4;
	}

	public void setPCEipv4(Inet4Address ip) {
		this.PCEipv4 = ip;
	}
	
	

		/**
	 * Funcion que transforma una cantidad de bits en el numero de bytes que necesita 
	 * @param numBit
	 */
	private int getNumberBytes(int numBits){
		int numberBytes = numBits/8;
		if ((numberBytes*8)<numBits){
			numberBytes++;
		}
		return numberBytes;
	}
	
	/*
	public String toString(){
		String ret="";

		if (PCEipv4!=null){
			ret=ret+controllerIT.toString()+"\t";
		}
		if (cpu!=null){
			ret=ret+cpu.toString()+"\t";
		}
		if (mem!=null){
			ret=ret+mem.toString()+"\t";
		}
		if (storage!=null){
			ret=ret+storage.toString()+"\t";
		}


		return ret;
	}*/



	public String getLearntFrom() {
		return learntFrom;
	}
	public void setLearntFrom(String learntFrom) {
		this.learntFrom = learntFrom;
	}

	public String getdomainID() {
		return domainID;
	}

	public void setdomainID(String ID) {
		this.domainID = ID;
	}

	
}

package es.tid.tedb;

import com.google.common.collect.HashBasedTable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * Base Interface for a Generic Traffic Engineering Database
 * @author ogondio
 *
 */
public interface TEDB {

	public void initializeFromFile(String file);

	public void initializeFromFile(String file, String learnFrom);


	public boolean isITtedb(); //FIXME: Remove!
	
	public String printTopology();

	public LinkedList<InterDomainEdge> getInterDomainLinks();





}

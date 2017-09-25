package es.tid.tedb;



import es.tid.bgp.bgp4.update.fields.pathAttributes.AS_Path_Segment;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsInfo {

	private Inet4Address AsDomain;
	private String learntFrom;
	private int type;
	private int numberOfSegments;
	int [] temp_segments;
	AS_Path_Segment asPathSegments;

	//ArrayList<AS4_Path_Attribute> AS_pathAttribute = new ArrayList<AS4_Path_Attribute>();


	public AsInfo()
	{

	}

	public AsInfo(AS_Path_Segment asPathSegments, int type, int numberOfSegments, int[] temp_segments, String learntFrom) {

		this.type=type;
		this.numberOfSegments=numberOfSegments;
		this.temp_segments= temp_segments;
		this.asPathSegments= asPathSegments;
		//this.asPathSegments.setNumberOfSegments(this.numberOfSegments);
		//this.asPathSegments.setType(this.type);
		this.asPathSegments.setSegments(this.temp_segments);
		setLearntFrom (learntFrom);

	}



	public String getLearntFrom() {
		return this.learntFrom;
	}
	public void setLearntFrom(String learntFrom) {
		this.learntFrom = learntFrom;
	}

   public int getType() {
        return asPathSegments.getType();
    }
	public int getsegmentNumbers() {
		return asPathSegments.getNumberOfSegments();
	}

	public String getsegmentValue() {
		return Arrays.toString(asPathSegments.getSegments());
	}

}

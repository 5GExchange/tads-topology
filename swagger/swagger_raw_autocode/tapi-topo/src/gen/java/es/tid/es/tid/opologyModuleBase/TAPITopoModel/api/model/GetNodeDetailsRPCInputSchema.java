package es.tid.es.tid.opologyModuleBase.TAPITopoModel.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;




/**
 * GetNodeDetailsRPCInputSchema
 */
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-02-09T12:33:04.057+01:00")
public class GetNodeDetailsRPCInputSchema   {
  private String topologyIdOrName = null;

  private String nodeIdOrName = null;

  public GetNodeDetailsRPCInputSchema topologyIdOrName(String topologyIdOrName) {
    this.topologyIdOrName = topologyIdOrName;
    return this;
  }

   /**
   * Get topologyIdOrName
   * @return topologyIdOrName
  **/
  @ApiModelProperty(value = "")
  public String getTopologyIdOrName() {
    return topologyIdOrName;
  }

  public void setTopologyIdOrName(String topologyIdOrName) {
    this.topologyIdOrName = topologyIdOrName;
  }

  public GetNodeDetailsRPCInputSchema nodeIdOrName(String nodeIdOrName) {
    this.nodeIdOrName = nodeIdOrName;
    return this;
  }

   /**
   * Get nodeIdOrName
   * @return nodeIdOrName
  **/
  @ApiModelProperty(value = "")
  public String getNodeIdOrName() {
    return nodeIdOrName;
  }

  public void setNodeIdOrName(String nodeIdOrName) {
    this.nodeIdOrName = nodeIdOrName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetNodeDetailsRPCInputSchema getNodeDetailsRPCInputSchema = (GetNodeDetailsRPCInputSchema) o;
    return Objects.equals(this.topologyIdOrName, getNodeDetailsRPCInputSchema.topologyIdOrName) &&
        Objects.equals(this.nodeIdOrName, getNodeDetailsRPCInputSchema.nodeIdOrName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topologyIdOrName, nodeIdOrName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetNodeDetailsRPCInputSchema {\n");
    
    sb.append("    topologyIdOrName: ").append(toIndentedString(topologyIdOrName)).append("\n");
    sb.append("    nodeIdOrName: ").append(toIndentedString(nodeIdOrName)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}


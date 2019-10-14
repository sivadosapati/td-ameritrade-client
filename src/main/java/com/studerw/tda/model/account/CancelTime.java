package com.studerw.tda.model.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelTime implements Serializable {

  private final static long serialVersionUID = -1305424865253107676L;

  @JsonProperty("date")
  private String date;
  @JsonProperty("shortFormat")
  private Boolean shortFormat = false;

  public String getDate() {
    return date;
  }

  public Boolean getShortFormat() {
    return shortFormat;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("date", date)
        .append("shortFormat", shortFormat)
        .toString();
  }
}

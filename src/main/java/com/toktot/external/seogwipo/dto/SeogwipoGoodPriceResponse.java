package com.toktot.external.seogwipo.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "Response")
public class SeogwipoGoodPriceResponse {

        @JacksonXmlProperty(localName = "total")
        private Integer total;

        @JacksonXmlProperty(localName = "page")
        private Integer page;

        @JacksonXmlProperty(localName = "pageSize")
        private Integer pageSize;

        @JacksonXmlElementWrapper(localName = "items")
        @JacksonXmlProperty(localName = "item")
        private List<SeogwipoGoodPriceItem> items;
}

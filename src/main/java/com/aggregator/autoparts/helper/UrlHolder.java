package com.aggregator.autoparts.helper;

import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EqualsAndHashCode
public class UrlHolder {
    @Value("#{'${website.urls}'.split(',')}")
    private List<String> urls;
    public static final String NOT_FOUND = "not found URL";
    public String getUrl(String url){
        return urls.stream().filter(s -> s.contains(url)).findFirst().orElse(NOT_FOUND);
    }
}

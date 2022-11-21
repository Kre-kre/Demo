package com.example.demo.service.impl;

import com.example.demo.dto.Query;
import com.example.demo.entity.Response;
import com.example.demo.entity.SparePart;
import com.example.demo.exeptionhendler.BusinessException;
import com.example.demo.helper.PropertiesReader;
import com.example.demo.service.SparePartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
@AllArgsConstructor
@NoArgsConstructor
public class AvtoProServiceImp implements SparePartService {
    @Autowired
    private RestTemplate restTemplate;
    @Value("#{${pages}}")
    private int pages;
    @Value("#{'${website.urls}'.split(',')}")
    private List<String> urls;

    @Override
    public Response searchSparePartBySerialNumber(String serialNumber) {
        HttpEntity<String> response = callRemoteHost(serialNumber);
        return getResponseFromHttpEntity(response);
    }

    public HttpEntity<String> callRemoteHost(String serialNumber) {
        Query query = new Query();
        query.setQuery(serialNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<Query> requestBody = new HttpEntity<>(query, headers);

        return restTemplate.exchange(getUrl(), HttpMethod.PUT, requestBody, String.class);
    }


    public Response getResponseFromHttpEntity(HttpEntity<String> response) {
        try {
            Response result = new Response();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode arrayNode = root.path(PropertiesReader.getProperties("Suggestions"));
            if (arrayNode.isArray()) {
                extractJsonNode(result, arrayNode);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(PropertiesReader.getProperties("avtoProEx"),
                    "Exception occurred in getResponseFromHttpEntity(): " + e.getMessage(), e);
        }
    }

    public void extractJsonNode(Response result, JsonNode arrayNode) {
        try {
            for (JsonNode node : arrayNode) {
                String description = node.path(PropertiesReader.getProperties("FoundPart"))
                        .path(PropertiesReader.getProperties("Part"))
                        .path(PropertiesReader.getProperties("Crossgroup"))
                        .path(PropertiesReader.getProperties("Category"))
                        .path(PropertiesReader.getProperties("Name")).asText();
                SparePart sparePart = new SparePart();
                if (!node.path(PropertiesReader.getProperties("Uri")).asText().equals("null")) {
                    sparePart.setUrl(PropertiesReader.getProperties("UrlAvtoPro") +
                            node.path(PropertiesReader.getProperties("Uri")).asText());
                    sparePart.setDescription(node.path(PropertiesReader.getProperties("Title")).asText() +
                            PropertiesReader.getProperties("SpareList") + description);
                    result.getSparePartList().add(sparePart);
                    if (result.getSparePartList().size() == pages) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(PropertiesReader.getProperties("avtoProEx"),
                    "Exception occurred in extractJsonNode(): " + e.getMessage(), e);
        }
    }

    private String getUrl() {
        return urls.stream()
                .filter(s -> s.contains(PropertiesReader.getProperties("UrlAvtoPro"))).findFirst().get();
    }
}
package com.example.demo.service.impl;

import com.example.demo.entity.Response;
import com.example.demo.entity.SparePart;
import com.example.demo.exeptionhendler.BusinessException;
import com.example.demo.helper.BusinessNameHolder;
import com.example.demo.helper.PathHolder;
import com.example.demo.helper.PropertiesReader;
import com.example.demo.service.SparePartService;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Slf4j
public class AvtoPlusService implements SparePartService {
    @Autowired
    private Executor executor;
    @Autowired
    private RestTemplate restTemplate;
    @Value("#{${pages}}")
    private int pages;
    @Value("#{'${website.urls}'.split(',')}")
    private List<String> urls;
    private final static int FIRST_PAGE = 1;

    @Override
    public Response searchSparePartBySerialNumber(String serialNumber) {
        Response response = new Response();
        return callRemoteHost(serialNumber, response).join();
    }

    private CompletableFuture<Response> callRemoteHost(String serialNumber, Response response) {
        return CompletableFuture.supplyAsync(
                () -> {
                    log.info("find spare parts AVTO PLUS " + Thread.currentThread().getName());
                    try {
                        response.getSparePartList().addAll(extractDataOnAllPages(serialNumber));
                    } catch (BusinessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return response;
                }, executor);
    }

    private int getPagesQuantityInDocument(Document docWithPageQuantity) {
        int pagesQuantity = FIRST_PAGE;
        Element listPages = docWithPageQuantity.
                getElementsByClass(BusinessNameHolder.LOAD_MORE_SEARCH.getPath()).first();
        if (listPages != null) {
            Element elementWithQuantityPages = listPages
                    .getElementsByAttribute(BusinessNameHolder.DATA_TOTAL.getPath()).first();
            assert elementWithQuantityPages != null;
            pagesQuantity = Integer.parseInt(elementWithQuantityPages
                    .attr(BusinessNameHolder.DATA_TOTAL.getPath()));
            if (pagesQuantity > pages) {
                pagesQuantity = pages;
            }
        }
        return pagesQuantity;
    }

    private CompletableFuture<List<SparePart>> getSparePartOnPage(String serialNumber, int page) {
        return CompletableFuture.supplyAsync(() -> {
            List<SparePart> sparePartList = new ArrayList<>();
            try {
                String url = getUrl() + page + BusinessNameHolder.SEARCH.getPath() + serialNumber;
                Document document = getDocumentFromRemoteHost(url, BusinessNameHolder.APPLICATION.getPath(), restTemplate);
                List<Element> listElementInside = new ArrayList<>(document.
                        getElementsByClass(BusinessNameHolder.GOOD_ITEM.getPath()));
                for (Element element : listElementInside) {
                    sparePartList.addAll(extractDataOnPage(element));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessException(PropertiesReader.getProperties("avtoPlusEx"),
                        e.getMessage(), e);
            }
            return sparePartList;
        }, executor);
    }

    public static Document getDocumentFromRemoteHost(String url, String application, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, application);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        HttpEntity<String> body = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return Jsoup.parse(Objects.requireNonNull(body.getBody()));
    }

    private List<SparePart> extractDataOnAllPages(String serialNumber) {
        Document docWithPageQuantity = getDocumentFirstPage(serialNumber);
        int pagesQuantity = getPagesQuantityInDocument(docWithPageQuantity);
        List<CompletableFuture<List<SparePart>>> completableFutures = new ArrayList<>();
        for (int pageNumber = FIRST_PAGE; pageNumber <= pagesQuantity; pageNumber++) {
            completableFutures.add(getSparePartOnPage(serialNumber, pageNumber));
        }
        List<List<SparePart>> listList = completableFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        List<SparePart> sparePartList = new ArrayList<>();
        for (List<SparePart> list : listList) {
            sparePartList.addAll(list);
        }
        return sparePartList;
    }

    private List<SparePart> extractDataOnPage(Element element) {
        List<SparePart> sparePartList = new ArrayList<>();
        try {
            Element elementWithItem = element.getElementsByTag(BusinessNameHolder.A.getPath()).get(1);
            SparePart sparePart = new SparePart();
            if (StringUtils.hasText(elementWithItem.text())) {
                sparePart.setDescription(elementWithItem.text());
                String cost = element
                        .getElementsByClass(BusinessNameHolder.BASKET_BUTTON.getPath())
                        .text()
                        .replaceAll(BusinessNameHolder.REPLACE_TEXT.getPath(), "");
                sparePart.setCost(Integer.parseInt(cost));
                sparePart.setUrl(PropertiesReader.getProperties(PathHolder.URL_AVTO_PLUS.getPath()) +
                        elementWithItem.attr(BusinessNameHolder.HREF.getPath()));
                sparePartList.add(sparePart);
            }
        } catch (
                Exception ex) {
            throw new BusinessException(PropertiesReader.getProperties("avtoPlusEx"), ex.getMessage(), ex);
        }
        return sparePartList;
    }

    private Document getDocumentFirstPage(String serialNumber) {
        try {
            return Jsoup.connect(getUrl() + FIRST_PAGE + BusinessNameHolder.SEARCH.getPath() + serialNumber).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUrl() {
        return urls.stream().filter(s -> s.contains(PropertiesReader.getProperties("UrlAvtoPlus"))).findFirst().get();
    }
}
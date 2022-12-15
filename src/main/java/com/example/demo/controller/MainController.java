package com.example.demo.controller;

import com.example.demo.entity.Response;
import com.example.demo.entity.SerialNumber;
import com.example.demo.entity.SparePart;
import com.example.demo.helper.UserExcelExporter;
import com.example.demo.service.impl.MainService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


@Slf4j
@RestController
@AllArgsConstructor
@SessionAttributes("response")
@RequestMapping(path = "/api")
public class MainController {

    @Autowired
    private final MainService mainService;

    @GetMapping(value = "/search")
    @ApiOperation(value = "Get price and reference",
            notes = "Find spare part by SN. For best results, enter the serial number or spare part number")
    @ApiImplicitParam(name = "serialNumber", required = true,
            dataType = "String")
    @ApiResponses({
            @ApiResponse(code = 200, message = "ok"),
            @ApiResponse(code = 400, message = "bad request syntax"),
            @ApiResponse(code = 404, message = "not found")
    })
    public ModelAndView getSparePartBySerialNumber(@ModelAttribute("serialNumber") String serialNumber) {
        ModelAndView result = new ModelAndView("spare-part");
        Response response = mainService.searchSparePartBySerialNumber(serialNumber);
        result.addObject("spare", response.getSparePartList());
        result.addObject("response", response);
        return result;
    }

    @GetMapping(path = {"index", "/"})
    private ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        SerialNumber serialNumber = new SerialNumber();
        modelAndView.addObject("serialNumber", serialNumber);
        return modelAndView;
    }

    @GetMapping(path = "save")
    private ResponseEntity<InputStreamResource> saveResultClientSide(@RequestParam String cost,
                                                                     @RequestParam String description,
                                                                     @RequestParam String url) {
        return mainService.saveFileClientSide(cost, description, url);
    }

    @GetMapping(path = "saveToXlsx")
    private ResponseEntity<InputStreamResource> saveResultClientSideExel(@ModelAttribute Response response) {
        log.debug("MainService saveResultClientSideExel() response param: " + response);
        return mainService.saveDataToExelClientSide(response.getSparePartList());
    }
}

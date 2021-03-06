package com.harokad.goona.controller;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.harokad.goona.crawler.filesystem.FilesystemCrawler;
import com.harokad.goona.crawler.url.UrlCrawler;
import com.harokad.goona.domain.EdmDocumentFile;
import com.harokad.goona.service.EdmCrawlingService;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("fsriver")
@Slf4j
public class EdmFsRiverController {

    @Inject 
    private EdmCrawlingService edmCrawlingService;

    @Inject 
    private FilesystemCrawler filesystemCrawler;
    
    @RequestMapping(value = "/start", method = RequestMethod.GET, params = {"source"})
    @ResponseStatus(value=HttpStatus.OK)
    public void startCrawling(@RequestParam(value = "source") String source) {
        log.info("Begin crawling for source : {}", source);
        edmCrawlingService.snapshotCurrentDocumentsForSource(source);
    }

    @RequestMapping(value = "/stop", method = RequestMethod.GET, params = {"source"})
    @ResponseStatus(value=HttpStatus.OK)
    public void stopCrawling(@RequestParam(value = "source") String source) {
        log.info("End of crawling for source : {}", source);
        edmCrawlingService.deleteUnusedDocumentsBeforeSnapshotForSource(source);
    }

    @RequestMapping(value = "/filesystem", method = RequestMethod.GET, params = {"path"})
    @ResponseBody
    public String crawlFilesystem(
            @RequestParam(value = "path") String path,
            @RequestParam(value = "edmServerHttpAddress", defaultValue = "http://127.0.0.1:8080") String edmServerHttpAddress,
            @RequestParam(value = "sourceName", defaultValue = "unmanned source") String sourceName,
            @RequestParam(value = "categoryName", defaultValue = "unmanned category") String categoryName,
            @RequestParam(value = "exclusionRegex", defaultValue = "") String exclusionRegex
       ) {
        log.info("[crawlFilesystem] Starting crawling on path : '{}'  (exclusion = '{}')", path, exclusionRegex);
        try {
            filesystemCrawler.importFilesInDir(path, edmServerHttpAddress, sourceName, categoryName, exclusionRegex);
        } catch (IOException e) {
            log.error("[crawlFilesystem] Failed to crawl '{}' with embedded crawler", path, e);
        }

        return "OK";
    }

    @RequestMapping(value = "/url", method = RequestMethod.GET, params = {"url"})
    @ResponseBody
    public String crawlUrl(
            @RequestParam(value = "url") String url,
            @RequestParam(value = "edmServerHttpAddress", defaultValue = "http://127.0.0.1:8080") String edmServerHttpAddress,
            @RequestParam(value = "sourceName", defaultValue = "unmanned source") String sourceName,
            @RequestParam(value = "categoryName", defaultValue = "unmanned category") String categoryName,
            @RequestParam(value = "exclusionRegex", defaultValue = "") String exclusionRegex
       ) {
        log.info("[crawlUrl] Starting crawling on path : '{}'  (exclusion = '{}')", url, exclusionRegex);
        try {
            UrlCrawler.importFilesAtUrl(url, sourceName, categoryName, exclusionRegex);
        } catch (IOException e) {
            log.error("[crawlUrl] Failed to crawl '{}' with embedded crawler", url, e);
        }

        return "OK";
    }
    
    @RequestMapping(method=RequestMethod.POST, value="/document", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EdmDocumentFile create(@RequestBody EdmDocumentFile edmDocument) {
        return edmCrawlingService.save(edmDocument);
    }

}

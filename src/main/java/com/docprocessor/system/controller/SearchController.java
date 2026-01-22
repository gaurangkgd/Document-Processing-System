package com.docprocessor.system.controller;

import com.docprocessor.system.dto.SearchResultDTO;
import com.docprocessor.system.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultDTO>> search(@RequestParam String keyword) {
        List<SearchResultDTO> results = searchService.search(keyword);
        return ResponseEntity.ok(results);
    }
}

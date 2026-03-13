package se.smartairbase.mcpclient.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.service.AnalysisFeedService;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
/**
 * Exposes analysis-feed endpoints to the browser frontend.
 */
public class AnalysisController {

    private final AnalysisFeedService analysisFeedService;

    public AnalysisController(AnalysisFeedService analysisFeedService) {
        this.analysisFeedService = analysisFeedService;
    }

    @GetMapping("/games/{gameId}/analysis-feed")
    public AnalysisFeedResponseDTO getAnalysisFeed(@PathVariable String gameId) {
        return analysisFeedService.getFeed(gameId);
    }

    @PostMapping("/games/{gameId}/analysis/generate")
    public AnalysisFeedResponseDTO generateAnalysis(@PathVariable String gameId) {
        return analysisFeedService.generateRoundAnalysis(gameId);
    }
}

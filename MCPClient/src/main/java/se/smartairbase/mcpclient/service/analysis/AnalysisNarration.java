package se.smartairbase.mcpclient.service.analysis;

/**
 * Holds one generated narration snippet and its provenance.
 */
public record AnalysisNarration(String source, String summary, String details) {
}

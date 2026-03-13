package se.smartairbase.mcpclient.domain;

/**
 * Identifies the narrative voices used in the analysis feed.
 */
public enum AnalysisRole {
    PILOT("Captain Erik Holm (Pilot)"),
    GROUND_CREW("Sara Lind (Ground Crew Chief)"),
    MAINTENANCE_TECHNICIANS("Johan Berg (Lead Maintenance Technician)"),
    COMMAND_OPERATIONS("Colonel Anna Sjöberg (Command / Operations)");

    private final String displayName;

    AnalysisRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

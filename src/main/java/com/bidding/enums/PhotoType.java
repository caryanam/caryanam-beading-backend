package com.bidding.enums;

public enum PhotoType {
    // Exterior
    FRONT_VIEW,
    RIGHT_FRONT_VIEW,
    REAR_VIEW,
    LEFT_FRONT_VIEW,
    ROOF_VIEW,

    // Engine
    ENGINE_IMAGE,
    BATTERY_IMAGE,

    // Tyres
    FRONT_RIGHT_TYRE,
    REAR_RIGHT_TYRE,
    FRONT_LEFT_TYRE,
    REAR_LEFT_TYRE,
    SPARE_WHEEL,
    TYRES_OVERVIEW,

    // Interior
    ODOMETER_IMAGE,
    DASHBOARD_IMAGE,
    AC_CONTROL_IMAGE,
    INSTRUMENT_CLUSTER_IMAGE,
    MUSIC_SYSTEM_IMAGE;

    public String getDisplayName() {
        String[] words = this.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0)))
              .append(w.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}

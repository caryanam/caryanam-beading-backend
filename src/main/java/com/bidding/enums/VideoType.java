package com.bidding.enums;

public enum VideoType {
    VEHICLE_WALKAROUND,
    ENGINE_RUNNING,
    EXHAUST_SMOKE;

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

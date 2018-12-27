package de.lgohlke.ci.config.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BuildConfig {
    private Options options = new Options();
    private List<Step> steps = new ArrayList<>();
}

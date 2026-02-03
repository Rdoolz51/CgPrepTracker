package com.gauntletprep;
import lombok.Value;
@Value
public class RouteProfile {
    int initialShards;
    int initialResources;
    int mainShardsNoTele;
    int mainShardsWithTele;
    boolean requireDemiDrops;
    boolean requireT3Weapons;
}

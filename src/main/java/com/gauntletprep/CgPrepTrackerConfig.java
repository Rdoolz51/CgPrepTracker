package com.gauntletprep;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("cgpreptracker")
public interface CgPrepTrackerConfig extends Config
{
    @ConfigItem(
            keyName = "routePreset",
            name = "Route preset",
            description = "Choose which prep route to track"
    )
    default RoutePreset routePreset()
    {
        return RoutePreset.T2_ARMOR_TWO_T3_WEAPONS;
    }

    @ConfigItem(
            keyName = "autoAdvancePhases",
            name = "Auto-advance phases",
            description = "Automatically advance phases when requirements are met"
    )
    default boolean autoAdvancePhases()
    {
        return true;
    }

    @ConfigItem(
            keyName = "notifyInChat",
            name = "Chat notifications",
            description = "Send chat notifications when you should return"
    )
    default boolean notifyInChat()
    {
        return true;
    }

    @ConfigItem(
            keyName = "initialShardTarget",
            name = "Initial shard target",
            description = "Shards needed in the initial sweep"
    )
    default int initialShardTarget()
    {
        return 100;
    }

    @ConfigItem(
            keyName = "initialResourceTarget",
            name = "Initial resource target",
            description = "Total resources (ore+bark+cotton) needed in the initial sweep"
    )
    default int initialResourceTarget()
    {
        return 3;
    }

    @ConfigItem(
            keyName = "mainShardTargetNoTeleport",
            name = "Main shard target (no teleport)",
            description = "Shards needed in the main sweep if you did not find a teleport crystal"
    )
    default int mainShardTargetNoTeleport()
    {
        return 420;
    }

    @ConfigItem(
            keyName = "mainShardTargetWithTeleport",
            name = "Main shard target (with teleport)",
            description = "Shards needed in the main sweep if you found a teleport crystal"
    )
    default int mainShardTargetWithTeleport()
    {
        return 400;
    }

    @ConfigItem(
            keyName = "foodGoal",
            name = "Food goal",
            description = "How many raw paddlefish you want before cooking"
    )
    default int foodGoal()
    {
        return 16;
    }
}

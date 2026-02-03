package com.gauntletprep;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class CgPrepTrackerOverlay extends Overlay
{
    private final PanelComponent panel = new PanelComponent();
    private final CgPrepTrackerPlugin plugin;

    @Inject
    public CgPrepTrackerOverlay(CgPrepTrackerPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panel.getChildren().clear();

        panel.getChildren().add(LineComponent.builder()
                .left("CG Prep Tracker")
                .right(plugin.getRouteLabel())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Phase")
                .right(plugin.getPhase().name())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Shards")
                .right(plugin.getShards() + " / " + plugin.getShardTargetForPhase())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Ore/Bark/Cotton")
                .right(plugin.getOre() + "/" + plugin.getBark() + "/" + plugin.getCotton())
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Frame / Vials")
                .right(plugin.getWeaponFrames() + " / " + plugin.getVials())
                .build());

        if (plugin.getPhase() == PrepPhase.MAIN_SWEEP && plugin.isRequireDemiDrops())
        {
            panel.getChildren().add(LineComponent.builder()
                    .left("Orb / Bowstring")
                    .right((plugin.hasOrb() ? "✅" : "❌") + " / " + (plugin.hasBowstring() ? "✅" : "❌"))
                    .build());
        }

        panel.getChildren().add(LineComponent.builder()
                .left("Teleport crystal")
                .right(plugin.hasTeleportCrystal() ? "✅" : "❌")
                .build());

        panel.getChildren().add(LineComponent.builder()
                .left("Raw fish")
                .right(String.valueOf(plugin.getRawFish()))
                .build());

        if (plugin.isReturnNow())
        {
            panel.getChildren().add(LineComponent.builder()
                    .left("RETURN NOW")
                    .right("✅")
                    .build());
        }

        return panel.render(graphics);
    }
}

package com.gauntletprep;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.api.ChatMessageType;

@PluginDescriptor(
        name = "CG Prep Tracker",
        description = "Tracks Corrupted Gauntlet prep resources by phase and tells you when to return",
        tags = {"gauntlet", "corrupted", "hunllef", "cg", "prep"}
)
public class CgPrepTrackerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private CgPrepTrackerConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private CgPrepTrackerOverlay overlay;
    @Inject private ChatMessageManager chatMessageManager;

    @Getter private PrepPhase phase = PrepPhase.INITIAL_SWEEP;

    @Getter private int shards;
    @Getter private int ore;
    @Getter private int bark;
    @Getter private int cotton;
    @Getter private int weaponFrames;
    @Getter private int vials;
    @Getter private int rawFish;

    private boolean orb;
    private boolean bowstring;
    private boolean teleportCrystal;

    private boolean t2Staff;
    private boolean t3Staff;
    private boolean t3Bow;

    private boolean returnNow;
    private boolean lastReturnNow; // debounce chat

    @Provides
    CgPrepTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CgPrepTrackerConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        clientThread.invokeLater(this::recalcFromInventory);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        returnNow = false;
        lastReturnNow = false;
    }

    // ---- Overlay helpers ----

    public String getRouteLabel()
    {
        return config.routePreset() == RoutePreset.FULL_T2 ? "Full T2" : "T2 + 2×T3";
    }

    public boolean isReturnNow()
    {
        return returnNow;
    }

    public boolean isRequireDemiDrops()
    {
        return getProfile().isRequireDemiDrops();
    }

    public boolean hasOrb() { return orb; }
    public boolean hasBowstring() { return bowstring; }
    public boolean hasTeleportCrystal() { return teleportCrystal; }

    public int getShardTargetForPhase()
    {
        RouteProfile p = getProfile();
        if (phase == PrepPhase.INITIAL_SWEEP)
        {
            return p.getInitialShards();
        }
        if (phase == PrepPhase.MAIN_SWEEP)
        {
            return teleportCrystal ? p.getMainShardsWithTele() : p.getMainShardsNoTele();
        }
        return 0;
    }

    // ---- Events ----

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged e)
    {
        if (e.getContainerId() != net.runelite.api.InventoryID.INVENTORY.getId())
        {
            return;
        }
        recalcFromInventory();
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        returnNow = shouldReturnNow();

        if (config.notifyInChat() && returnNow && !lastReturnNow)
        {
            notifyChat("CG Prep Tracker: RETURN NOW (" + getRouteLabel() + ", " + phase.name() + ")");
        }
        lastReturnNow = returnNow;

        if (config.autoAdvancePhases())
        {
            autoAdvanceIfComplete();
        }
    }

    // ---- Core logic ----

    private RouteProfile getProfile()
    {
        if (config.routePreset() == RoutePreset.FULL_T2)
        {
            return new RouteProfile(
                    config.initialShardTarget(),
                    config.initialResourceTarget(),
                    config.mainShardTargetNoTeleport(),
                    config.mainShardTargetWithTeleport(),
                    false,  // no demi drops required
                    false   // no T3 weapons required
            );
        }

        // T2 armor, 2 perfected weapons (staff + bow)
        return new RouteProfile(
                config.initialShardTarget(),
                config.initialResourceTarget(),
                config.mainShardTargetNoTeleport(),
                config.mainShardTargetWithTeleport(),
                true,   // require orb + bowstring
                true    // require perfected staff + bow before fishing phase
        );
    }

    private boolean shouldReturnNow()
    {
        RouteProfile p = getProfile();

        switch (phase)
        {
            case INITIAL_SWEEP:
                return shards >= p.getInitialShards()
                        && (ore + bark + cotton) >= p.getInitialResources()
                        && weaponFrames >= 1;

            case MAIN_SWEEP:
                int shardTarget = teleportCrystal ? p.getMainShardsWithTele() : p.getMainShardsNoTele();
                boolean matsOk = ore >= 21 && bark >= 21 && cotton >= 21;
                boolean shardsOk = shards >= shardTarget;
                boolean demiOk = !p.isRequireDemiDrops() || (orb && bowstring);
                return matsOk && shardsOk && demiOk;

            default:
                return false;
        }
    }

    private void autoAdvanceIfComplete()
    {
        RouteProfile p = getProfile();

        switch (phase)
        {
            case INITIAL_SWEEP:
                if (shouldReturnNow())
                {
                    phase = PrepPhase.BOWL_SETUP;
                    notifyChat("CG Prep Tracker: Phase -> BOWL_SETUP (craft T2 staff, 2 vials, drop 3 mats)");
                }
                break;

            case BOWL_SETUP:
                // Minimal heuristic for now:
                // - You crafted an attuned staff (T2)
                // - You have 2 vials
                // We can later add a “confirm drop 3 resources” hotkey.
                if (t2Staff && vials >= 2)
                {
                    phase = PrepPhase.MAIN_SWEEP;
                    notifyChat("CG Prep Tracker: Phase -> MAIN_SWEEP (finish mats, shards, and demi drops if needed)");
                }
                break;

            case MAIN_SWEEP:
                if (shouldReturnNow())
                {
                    phase = PrepPhase.FINAL_CRAFT;
                    notifyChat("CG Prep Tracker: Phase -> FINAL_CRAFT (craft armor/weapons)");
                }
                break;

            case FINAL_CRAFT:
                boolean weaponsOk = !p.isRequireT3Weapons() || (t3Staff && t3Bow);
                if (weaponsOk)
                {
                    phase = PrepPhase.FISH_AND_COOK;
                    notifyChat("CG Prep Tracker: Phase -> FISH_AND_COOK (fish + cook)");
                }
                break;

            case FISH_AND_COOK:
                // Optional: you could show “done” when rawFish >= foodGoal
                break;
        }
    }

    private void recalcFromInventory()
    {
        ItemContainer inv = client.getItemContainer(net.runelite.api.InventoryID.INVENTORY);

        shards = ore = bark = cotton = weaponFrames = vials = rawFish = 0;
        orb = bowstring = teleportCrystal = false;
        t2Staff = t3Staff = t3Bow = false;

        if (inv == null) return;

        for (Item it : inv.getItems())
        {
            if (it == null || it.getId() <= 0 || it.getQuantity() <= 0) continue;

            int id = it.getId();
            int qty = it.getQuantity();

            if (CgIds.SHARDS.contains(id)) shards += qty;

            if (id == CgIds.CRYSTAL_ORE) ore += qty;
            if (id == CgIds.PHREN_BARK) bark += qty;
            if (id == CgIds.LINUM_TIRINUM) cotton += qty;

            if (id == CgIds.WEAPON_FRAME) weaponFrames += qty;
            if (id == CgIds.VIAL) vials += qty;

            if (id == CgIds.RAW_PADDLEFISH) rawFish += qty;

            if (CgIds.ORB_DROPS.contains(id)) orb = true;
            if (CgIds.BOWSTRING_DROPS.contains(id)) bowstring = true;

            if (id == CgIds.TELEPORT_CRYSTAL) teleportCrystal = true;

            if (id == CgIds.STAFF_T2) t2Staff = true;
            if (id == CgIds.STAFF_T3) t3Staff = true;
            if (id == CgIds.BOW_T3) t3Bow = true;
        }
    }

    private void notifyChat(String msg)
    {
        if (!config.notifyInChat())
        {
            return;
        }

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(msg)
                .build());
    }
}

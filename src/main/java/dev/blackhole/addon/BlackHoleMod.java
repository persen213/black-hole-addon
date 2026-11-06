package dev.blackhole.addon;

import com.mojang.logging.LogUtils;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.SkillRequirement;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Graviton / Black Hole Manipulation: an official add-on category for Re:AcademyCraft 1.5.0.
 */
@Mod(BlackHoleMod.MOD_ID)
public final class BlackHoleMod {
    public static final String MOD_ID = "blackhole";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    /** The item used in the Ability Developer to obtain the Graviton / Black Hole category. */
    public static final RegistryObject<Item> FACTOR = ITEMS.register("blackhole_factor",
            () -> new Item(new Item.Properties().stacksTo(16)));

    static AbilityId id(String path) { return AbilityId.parse(MOD_ID + ":" + path); }

    public static final AbilityId CATEGORY = id("blackhole"),
            GRAVITY_PULL = id("gravity_pull"),
            EVENT_HORIZON = id("event_horizon"),
            GRAVITY_REPULSION = id("gravity_repulsion"),
            MICRO_SINGULARITY = id("micro_singularity"),
            SUPERMASSIVE_BLACK_HOLE = id("supermassive_black_hole");

    public BlackHoleMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FACTOR);
        }
    }

    private static Presentation presentation(AbilityId skill) {
        return new Presentation(MOD_ID + ".skill." + skill.path(), MOD_ID + ".skill." + skill.path() + ".desc", null);
    }

    private static SkillDefinition skill(AbilityId id, int level, List<SkillRequirement> requires, ActivationKind kind, float x, float y, @Nullable AbilityId parent) {
        return new SkillDefinition(id, CATEGORY, level, requires, kind, presentation(id), new TreePosition(x, y, parent));
    }

    private static SkillRequirement req(AbilityId skill, float proficiency) {
        return new SkillRequirement(skill, proficiency);
    }

    private void registerAbilities(RegisterAbilitiesEvent event) {
        var abilities = event.registrar(MOD_ID);

        // Register Category: 1 skill per level across all 5 levels (1, 1, 1, 1, 1)
        abilities.category(new CategoryDefinition(
                CATEGORY,
                id("blackhole_factor"),
                List.of(1, 1, 1, 1, 1),
                new Presentation(MOD_ID + ".ability.blackhole", null, null)
        ));

        // Level 1: Gravitational Pull (Instant)
        abilities.skill(skill(GRAVITY_PULL, 1, List.of(), ActivationKind.INSTANT, 25, 55, null));

        // Level 2: Event Horizon Field (Toggle)
        abilities.skill(skill(EVENT_HORIZON, 2, List.of(req(GRAVITY_PULL, 0.3f)), ActivationKind.TOGGLE, 70, 30, GRAVITY_PULL));

        // Level 3: Gravitational Repulsion (Instant)
        abilities.skill(skill(GRAVITY_REPULSION, 3, List.of(req(GRAVITY_PULL, 0.3f)), ActivationKind.INSTANT, 70, 80, GRAVITY_PULL));

        // Level 4: Micro Singularity (Toggle)
        abilities.skill(skill(MICRO_SINGULARITY, 4, List.of(req(EVENT_HORIZON, 0.4f)), ActivationKind.TOGGLE, 125, 55, EVENT_HORIZON));

        // Level 5: Supermassive Black Hole (Toggle/Channel - Ultimate)
        abilities.skill(skill(SUPERMASSIVE_BLACK_HOLE, 5, List.of(req(MICRO_SINGULARITY, 0.5f), req(GRAVITY_REPULSION, 0.4f)), ActivationKind.TOGGLE, 190, 55, MICRO_SINGULARITY));

        // Register Behaviors
        var behaviors = event.behaviors(MOD_ID);
        behaviors.behavior(GRAVITY_PULL, new GravityPull());
        behaviors.behavior(EVENT_HORIZON, new EventHorizon());
        behaviors.behavior(GRAVITY_REPULSION, new GravityRepulsion());
        behaviors.behavior(MICRO_SINGULARITY, new MicroSingularity());
        behaviors.behavior(SUPERMASSIVE_BLACK_HOLE, new SupermassiveBlackHole());
    }
}

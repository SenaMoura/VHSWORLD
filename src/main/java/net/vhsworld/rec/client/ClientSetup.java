package net.vhsworld.rec.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.entity.AnomalyRenderer;
import net.vhsworld.rec.client.entity.CrawlerVoidModel;
import net.vhsworld.rec.client.entity.CrawlerVoidRenderer;
import net.vhsworld.rec.client.entity.InvertedSilhouetteModel;
import net.vhsworld.rec.client.entity.InvertedSilhouetteRenderer;
import net.vhsworld.rec.client.entity.ShadeSegmentModel;
import net.vhsworld.rec.client.entity.ShadeSegmentRenderer;
import net.vhsworld.rec.client.entity.StaticWatcherModel;
import net.vhsworld.rec.client.entity.StaticWatcherRenderer;
import net.vhsworld.rec.client.entity.StonemanModel;
import net.vhsworld.rec.client.entity.StonemanRenderer;
import net.vhsworld.rec.client.entity.geom.CrawlerVoidGeometry;
import net.vhsworld.rec.client.entity.geom.InvertedSilhouetteGeometry;
import net.vhsworld.rec.client.entity.geom.ShadeSegmentGeometry;
import net.vhsworld.rec.client.entity.geom.StaticWatcherGeometry;
import net.vhsworld.rec.client.screen.RFReceiverScreen;
import net.vhsworld.rec.init.ModEntities;
import net.vhsworld.rec.init.ModMenus;

/** Amarracoes de cliente que rodam no mod bus: hoje so ligar a tela ao menu do Receptor. */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenus.RF_RECEIVER.get(), RFReceiverScreen::new));
    }

    /** A geometria do Homem de Pedra. Uma so: ele nao tem mais variantes. */
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StonemanModel.BASE, StonemanModel::createBase);
        event.registerLayerDefinition(net.vhsworld.rec.client.entity.MirrorModel.LAYER,
                net.vhsworld.rec.client.entity.MirrorModel::createBodyLayer);

        // A leva das quatro. A geometria destas vem de client/entity/geom/, gerada do
        // .bbmodel por tools/bbmodel_to_geometry.py — arquivo gerado, nao editar.
        event.registerLayerDefinition(StaticWatcherModel.LAYER, StaticWatcherGeometry::create);
        event.registerLayerDefinition(ShadeSegmentModel.LAYER, ShadeSegmentGeometry::create);
        event.registerLayerDefinition(InvertedSilhouetteModel.LAYER,
                InvertedSilhouetteGeometry::create);
        event.registerLayerDefinition(CrawlerVoidModel.LAYER, CrawlerVoidGeometry::create);

        // ⚠️ O Escutador nao vem de geom/: a geometria dele e escrita a mao e provisoria,
        // ate a malha em pecas articuladas chegar. O que vale nele e o RIG, em
        // ListenerModel — e o rig nao muda quando os vertices mudarem.
        event.registerLayerDefinition(net.vhsworld.rec.client.entity.ListenerModel.LAYER,
                net.vhsworld.rec.client.entity.ListenerModel::createBody);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STONEMAN.get(), StonemanRenderer::new);
        event.registerEntityRenderer(ModEntities.ANOMALY.get(), AnomalyRenderer::new);
        event.registerEntityRenderer(ModEntities.MIRROR.get(),
                net.vhsworld.rec.client.entity.MirrorRenderer::new);

        event.registerEntityRenderer(ModEntities.STATIC_WATCHER.get(), StaticWatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADE_SEGMENT.get(), ShadeSegmentRenderer::new);
        event.registerEntityRenderer(ModEntities.INVERTED_SILHOUETTE.get(),
                InvertedSilhouetteRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAWLER_VOID.get(), CrawlerVoidRenderer::new);
        event.registerEntityRenderer(ModEntities.LISTENER.get(),
                net.vhsworld.rec.client.entity.ListenerRenderer::new);
    }

    private ClientSetup() {}
}

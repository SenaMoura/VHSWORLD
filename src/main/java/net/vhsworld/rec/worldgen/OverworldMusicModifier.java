package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Pendura uma trilha no campo `music` de biomas que NAO sao nossos.
 *
 * ============================ POR QUE ISTO EXISTE ============================
 *
 * As doze dimensoes do mod resolvem a trilha de graca: cada uma tem um bioma
 * nosso, em `data/recmod/worldgen/biome/`, e basta escrever `effects.music` la
 * dentro. O overworld nao tem essa saida.
 *
 * O overworld do mod roda no gerador alpha (ver AlphaChunkGenerator, plugado pelo
 * `world_preset/normal.json`), e o gerador alpha devolve biomas VANILLA —
 * `minecraft:taiga`, `minecraft:desert`, e por ai. Isso foi de proposito: e o que
 * faz o overworld parecer o Minecraft de 2010 em vez de parecer um mod. Mas tem
 * um preco: um datapack so pode reescrever arquivos do proprio namespace. Para
 * botar musica em `minecraft:taiga` eu teria que SOBRESCREVER o bioma taiga
 * inteiro dentro do nosso jar — e ai o mod passaria a ditar cor de grama, mob
 * spawn e feature de um bioma vanilla, quebrando qualquer outro mod que mexa
 * neles, e desatualizando junto a cada versao do jogo.
 *
 * O BiomeModifier do Forge existe exatamente para isso: ele nao substitui o
 * bioma, ele passa DEPOIS e altera um campo so. Os outros 42 campos da taiga
 * continuam sendo os da Mojang.
 *
 * ⚠️ Repare que o alvo e uma TAG (`#minecraft:is_overworld`) e nao uma lista de
 * biomas. Se fosse lista, todo bioma que o Pedro adicionasse ao gerador alpha
 * depois nasceria mudo, e o bug seria silencioso — o jogo nao reclama de bioma
 * sem musica, so fica quieto. Com a tag, bioma novo de overworld ja entra com
 * trilha.
 *
 * ============================ A FASE ============================
 *
 * `Phase.MODIFY`. As fases do Forge rodam em ordem, e MODIFY e a que o proprio
 * Forge documenta para "alteracao de valores como clima ou cores". Rodar em ADD
 * funcionaria hoje, mas ADD e onde os outros mods empilham feature e spawn: uma
 * trilha entrando no meio disso e a mesma classe de erro que arrumar a casa antes
 * de a mudanca chegar.
 */
public record OverworldMusicModifier(HolderSet<Biome> biomes,
                                     Holder<SoundEvent> music,
                                     int minDelay,
                                     int maxDelay,
                                     boolean replaceCurrentMusic) implements BiomeModifier {

    public static final Codec<OverworldMusicModifier> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                    Biome.LIST_CODEC.fieldOf("biomes")
                            .forGetter(OverworldMusicModifier::biomes),
                    SoundEvent.CODEC.fieldOf("music")
                            .forGetter(OverworldMusicModifier::music),
                    Codec.INT.optionalFieldOf("min_delay", 0)
                            .forGetter(OverworldMusicModifier::minDelay),
                    Codec.INT.optionalFieldOf("max_delay", 0)
                            .forGetter(OverworldMusicModifier::maxDelay),
                    Codec.BOOL.optionalFieldOf("replace_current_music", true)
                            .forGetter(OverworldMusicModifier::replaceCurrentMusic)
            ).apply(inst, OverworldMusicModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY || !this.biomes.contains(biome)) {
            return;
        }
        builder.getSpecialEffects().backgroundMusic(
                new Music(this.music, this.minDelay, this.maxDelay, this.replaceCurrentMusic));
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}

package net.vhsworld.rec.director;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.config.RECConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * A COLOCACAO: o Diretor poe a criatura onde ela pode virar encontro.
 *
 * ⚠️ POR QUE ISTO EXISTE — O NUMERO QUE MATOU O SPAWN COMO BATIDA. Medido no jogo em
 * 2026-08-02: as criaturas nasciam a 114, 124 e 116 blocos do jogador. Nao foi azar. O
 * motor de spawn do vanilla sorteia numa casca de ~24 a 128 blocos e, na pratica, quase
 * sempre usa o limite de fora. Isso quer dizer que a batida MAIS CARA do Diretor (0.55 de
 * pressao, piso de 90 s) gastava o compasso inteiro com uma coisa que o jogador nao tinha
 * como ver, ouvir nem suspeitar — e o bicho ainda tinha que caminhar cem blocos para
 * chegar, coisa que Homem de Pedra (que congela quando olhado) e anomalia (que nem anda)
 * nao fazem nunca.
 *
 * O SpawnGate podia NEGAR o spawn; nao podia PEDIR um. Enquanto so existisse a negativa, o
 * Diretor era um censor do vanilla, nao o dono do compasso: ele conseguia impedir o
 * encontro errado e nao conseguia produzir o certo.
 *
 * ⚠️ E A MESMA LICAO DA AUSENCIA, aplicada a batida pesada. A ausencia so passou no teste
 * quando o Diretor pode COLOCAR o acontecimento a uma distancia que o jogador percebe e
 * fora do campo de visao. Aqui e igual, palavra por palavra: perto o bastante para
 * importar, atras o bastante para nao ser visto nascendo. A pergunta que toda mecanica
 * deste mod tem que responder — <i>qual e o segundo em que o jogador percebe que isto
 * aconteceu?</i> — o spawn natural respondia com "nenhum".
 */
public final class Staging {

    private Staging() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private static final Random RANDOM = new Random();

    /** Quantos lugares tentar antes de desistir de um tipo. */
    private static final int TRIES = 24;

    /**
     * ⚠️ O RACIONAMENTO DO ELENCO — a parte que o Pedro pediu antes de eu escrever isto,
     * e ele estava certo.
     *
     * O mod tem seis criaturas colocaveis. Hoje isso nao aparece porque o defeito de cima
     * ESCONDE o elenco: bicho que nasce a 114 blocos e nunca chega nao repete, porque nao
     * acontece. No instante em que o Diretor passar a colocar a criatura a vinte blocos, o
     * mesmo elenco passa a ser consumido de verdade, e "poucas criaturas" deixa de ser
     * opiniao e vira sintoma. Construir a colocacao sem racionar seria trocar um defeito
     * invisivel por um visivel.
     *
     * ⚠️ E O RACIONAMENTO NAO DIMINUI A FREQUENCIA — ELE FORCA A VARIEDADE. Esta e a
     * distincao que faz a mecanica valer a pena. O Diretor continua colocando na mesma
     * cadencia; ele so nao pode repetir o mesmo bicho dentro da janela. Com seis tipos e
     * dez minutos de janela, cabem seis colocacoes nesses dez minutos — o teto do
     * racionamento fica praticamente em cima do piso da batida, entao ele quase nunca
     * silencia o mod, so redistribui.
     *
     * O que se ganha e o que nenhuma quantidade de criatura nova compra: a mesma criatura
     * nao vira fauna. Criatura que aparece toda noite deixa de ser aparicao e vira bicho do
     * bioma, e ai o problema passa a ser de combate — que o jogador sabe resolver.
     */
    private static final Map<UUID, Map<EntityType<?>, Long>> LAST = new HashMap<>();

    /**
     * Tenta colocar uma criatura para este jogador. Devolve true se colocou.
     *
     * ⚠️ Quem decide QUANDO nao e este arquivo — e o Diretor, pelo Beat.SPAWN. Aqui so se
     * responde "deu para colocar alguma coisa agora?".
     */
    static boolean tryPlace(ServerPlayer player) {
        if (!RECConfig.COMMON.directorStaging.get()) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        List<EntityType<?>> roster = roster(level, player);
        if (roster.isEmpty()) {
            noRoster(player);
            return false;
        }

        double min = RECConfig.COMMON.directorStagingMinDistance.get();
        double max = RECConfig.COMMON.directorStagingMaxDistance.get();

        // ⚠️ Percorre o elenco na ordem de QUEM ESTA HA MAIS TEMPO SEM APARECER, e nao
        // sorteado. Sorteio com poucos tipos produz repeticao vizinha por acaso — dois
        // Homens de Pedra seguidos e o unico resultado que este arquivo existe para
        // impedir, e ele e justamente o mais provavel num elenco de seis.
        roster.sort((a, b) -> Long.compare(lastPlaced(player, a), lastPlaced(player, b)));

        for (EntityType<?> type : roster) {
            BlockPos spot = findSpot(level, player, type, min, max);
            if (spot == null) continue;

            Mob mob = place(level, player, type, spot);
            if (mob == null) continue;

            LAST.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                    .put(type, level.getGameTime());

            double distance = Math.sqrt(mob.distanceToSqr(player));
            float perceived = Director.perceived(distance);

            Director.report(player, Beat.SPAWN, perceived);

            LOG.info("[DIRETOR] COLOCOU {} a {} blocos de {} (pressao x{}) em {}",
                    ForgeRegistries.ENTITY_TYPES.getKey(type),
                    String.format("%.0f", distance),
                    player.getGameProfile().getName(),
                    String.format("%.2f", perceived), spot);
            return true;
        }

        noSpot(player, roster.size());
        return false;
    }

    /**
     * QUEM PODE APARECER AQUI.
     *
     * ⚠️ LE A LISTA DE SPAWN DO BIOMA em vez de ter lista propria, e isso e de proposito.
     * O mod ja decidiu, nos biome_modifier, quem nasce em que lugar — e as quinze dimensoes
     * vao continuar decidindo, cada uma com a sua lei. Se este arquivo tivesse a propria
     * lista, existiriam duas verdades sobre onde cada criatura cabe, e a segunda ia
     * silenciosamente furar a primeira: um Anomalo da Sombra no escritorio, um Observador
     * dentro do labirinto. A colocacao muda ONDE e QUANDO, nunca O QUE E PERMITIDO.
     */
    private static List<EntityType<?>> roster(ServerLevel level, ServerPlayer player) {
        long cooldown = RECConfig.COMMON.directorStagingTypeCooldownSeconds.get() * 20L;
        long now = level.getGameTime();

        MobSpawnSettings settings = level.getBiome(player.blockPosition()).value().getMobSettings();

        List<EntityType<?>> out = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData data : settings.getMobs(MobCategory.MONSTER).unwrap()) {
            EntityType<?> type = data.type;

            var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key == null || !net.vhsworld.rec.RECMod.MOD_ID.equals(key.getNamespace())) continue;

            if (now - lastPlaced(player, type) < cooldown) continue;

            out.add(type);
        }
        return out;
    }

    private static long lastPlaced(ServerPlayer player, EntityType<?> type) {
        Map<EntityType<?>, Long> mine = LAST.get(player.getUUID());
        if (mine == null) return Long.MIN_VALUE / 4;
        return mine.getOrDefault(type, Long.MIN_VALUE / 4);
    }

    /**
     * ONDE. Atras do jogador, perto o bastante para importar, e num lugar em que aquela
     * criatura nasceria de qualquer jeito.
     *
     * ⚠️ AS REGRAS DE SPAWN CONTINUAM VALENDO (SpawnPlacements). Da para colocar um bicho
     * em qualquer buraco com dois blocos de ar, e seria errado: o Observador e a Silhueta
     * pedem CEU ABERTO e noite porque os dois dependem de ser vistos de longe na linha do
     * horizonte, e o Anomalo da Sombra pede escuro porque a regra dele e luz. Furar isso
     * aqui nao daria mais encontro, daria encontro sem sentido — o Observador espremido
     * numa caverna e a criatura inteira jogada fora.
     */
    private static BlockPos findSpot(ServerLevel level, ServerPlayer player,
                                     EntityType<?> type, double min, double max) {
        SpawnPlacements.Type placement = SpawnPlacements.getPlacementType(type);

        Vec3 look = player.getLookAngle();
        double lookAngle = Math.atan2(look.z, look.x);

        for (int i = 0; i < TRIES; i++) {
            // ⚠️ O ARCO DE TRAS, calculado direto em vez de sortear no circulo inteiro e
            // rejeitar. Meia volta a partir de para onde ele olha, com folga de noventa
            // graus para cada lado: nunca cai no cone de visao, e nao gasta tentativa.
            // O que se ve nascer e um bug; o que se descobre ali e uma aparicao.
            double angle = lookAngle + Math.PI + (RANDOM.nextDouble() - 0.5D) * Math.PI;
            double distance = min + RANDOM.nextDouble() * (max - min);

            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);

            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;

            // Duas alturas por tentativa e nao uma varredura: a superficie (para quem
            // precisa de ceu) e a altura do proprio jogador (para quem esta em caverna).
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            for (int y : new int[]{surface, player.getBlockY()}) {
                BlockPos pos = new BlockPos(x, y, z);

                if (!NaturalSpawner.isSpawnPositionOk(placement, level, pos, type)) continue;
                if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos,
                        level.getRandom())) continue;
                if (!level.noCollision(type.getAABB(x + 0.5D, y, z + 0.5D))) continue;

                return pos;
            }
        }
        return null;
    }

    /**
     * Poe o bicho no mundo, ja virado para o jogador.
     *
     * ⚠️ MobSpawnType.EVENT, e nao NATURAL. O SpawnGate so trava NATURAL e
     * CHUNK_GENERATION — se esta colocacao se declarasse natural, o Diretor pediria licenca
     * a si mesmo e seria negado pelo proprio teto que ele acabou de conferir.
     *
     * ⚠️ JA VIRADO PARA O JOGADOR porque encontrar a coisa de costas estraga o momento: o
     * susto e ela ja estar te encarando quando voce vira, nao ela reparar em voce depois.
     */
    private static Mob place(ServerLevel level, ServerPlayer player, EntityType<?> type, BlockPos pos) {
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) entity.discard();
            return null;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;

        float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
        mob.moveTo(x, y, z, yaw, 0.0F);

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);

        if (!level.addFreshEntity(mob)) {
            mob.discard();
            return null;
        }
        return mob;
    }

    // ------------------------------------------------------------------ diagnostico

    /**
     * ⚠️ COM FREIO, e separando as DUAS causas. Quando a colocacao nao acontece, "nao
     * aconteceu nada" e indistinguivel de "esta quebrado" — o modo de falha que este
     * projeto ja pagou tres vezes. Elenco vazio (todos em espera, ou o bioma nao tem
     * criatura nossa) e um conserto; elenco cheio e nenhum lugar servindo e outro
     * completamente diferente.
     */
    private static long lastRosterLog, lastSpotLog;

    private static void noRoster(ServerPlayer player) {
        long now = System.currentTimeMillis();
        if (now - lastRosterLog < 60_000L) return;
        lastRosterLog = now;
        LOG.info("[DIRETOR] colocacao sem elenco | {} (todos em espera, ou o bioma nao lista criatura nossa)",
                player.getGameProfile().getName());
    }

    private static void noSpot(ServerPlayer player, int roster) {
        long now = System.currentTimeMillis();
        if (now - lastSpotLog < 60_000L) return;
        lastSpotLog = now;
        LOG.info("[DIRETOR] colocacao sem lugar | elenco={} tentativas={} (regra de spawn nao bateu atras do jogador)",
                roster, TRIES);
    }

    static void clear(UUID player) {
        LAST.remove(player);
    }
}

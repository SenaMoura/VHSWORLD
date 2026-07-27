package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.init.ModEntities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UMA. Nunca duas.
 *
 * ⚠️ POR QUE ISTO EXISTE: a DATA nasceu usando o spawn natural do jogo, e isso
 * encheu a dimensao com mais de cem caçadoras e derrubou o jogo para 1 fps. O
 * defeito nao era a taxa de spawn — era o mecanismo. O teto de monstros do
 * Minecraft e proporcional aos chunks carregados (~70 x chunks / 289, o que da
 * facilmente 170), e ele e DIVIDIDO entre todos os monstros do bioma. No overworld
 * a anomalia disputa com zumbi, esqueleto e aranha e sai pouca; num bioma onde ela
 * e o UNICO monstro, ela sozinha enche o teto inteiro. Spawn natural nao sabe dizer
 * "uma"; ele so sabe encher.
 *
 * Entao a DATA nao usa spawn natural nenhum. Este Diretor mantem exatamente uma
 * caçadora viva, poe ela FORA DE VISTA, e recoloca quando ela se perde de todo
 * mundo. E tambem o que limpa os mundos que ja foram salvos com a multidao dentro.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class DataDirector {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** De quanto em quanto o Diretor confere a cena. Nao precisa ser todo tick. */
    private static final int CHECK_EVERY = 40;

    /**
     * A que distancia ANDANDO a caçadora nasce — nao em linha reta.
     *
     * Medindo a planta: escolher por linha reta e uma armadilha. Um ponto a 14 blocos
     * de distancia pode estar do outro lado de uma parede e custar 190 de caminho,
     * e ai a busca do bicho estoura o alcance e ele fica parado. Medido em cinco
     * sementes, o pior caso do anel em linha reta ia a 216 blocos de caminho, contra
     * uma mediana de 33 — ou seja, a media enganava e a cauda e que mandava.
     *
     * Escolhendo pelo caminho, o numero deixa de ter cauda: o que se pede E a
     * distancia real. E de graça vem o "fora de vista", porque 24 blocos de corredor
     * andado ja e sempre depois de uma esquina.
     */
    private static final int SPAWN_MIN_PATH = 24, SPAWN_MAX_PATH = 56;

    /** Teto da varredura. Sem ele, um corredor comprido viraria uma busca sem fim. */
    private static final int SCAN_CELLS = 4000;

    /**
     * Sem ver ninguem por tanto tempo, ela e recolocada.
     *
     * Sem isto a unica caçadora do mapa pode ficar encalhada num beco no outro canto
     * do predio, e a dimensao vira um passeio — que e o oposto do que ela e.
     */
    private static final int LOST_AFTER = 2400;   // 2 minutos

    /** Perto disto ela NUNCA e recolocada: sumir na frente do jogador e quebrar a ilusao. */
    private static final double SAFE_DISTANCE = 32.0D;

    /**
     * Quando a caçadora teve alvo pela ultima vez.
     *
     * ⚠️ Tem que ser "ha quanto tempo sem alvo", e nao a IDADE dela — com a idade,
     * passados os dois minutos ela passaria a ser recolocada a cada rodada em que
     * estivesse sem alvo, ou seja, de dois em dois segundos. Como existe exatamente
     * uma no mundo, um campo so da conta; ele reinicia junto com o servidor, e a
     * unica consequencia disso e a primeira recolocacao demorar mais.
     */
    private static long lastTargetAt = Long.MIN_VALUE;

    private DataDirector() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!isData(level)) return;
        if (level.getGameTime() % CHECK_EVERY != 0) return;

        List<AnomalyEntity> hunters = new ArrayList<>();
        for (AnomalyEntity anomaly : level.getEntities(ModEntities.ANOMALY.get(), a -> true)) {
            if (anomaly.hunts()) hunters.add(anomaly);
            else anomaly.discard();   // na DATA so existe a caçadora
        }

        // A LIMPEZA. Um mundo salvo antes desta correcao volta com a multidao dentro,
        // e ela nao sai sozinha: a anomalia e persistente, entao nem o descarregar de
        // chunk a leva.
        //
        // Sobra a MAIS NOVA, e nao a primeira que aparecer na lista. E o que faz
        // `/summon` se comportar como qualquer um espera: quem invoca uma quer ver
        // AQUELA, e nao ver a recem-invocada evaporar porque ja havia outra num
        // corredor qualquer do outro lado do predio. A regra de "so uma" continua
        // valendo — muda so quem e a uma.
        if (hunters.size() > 1) {
            hunters.sort(java.util.Comparator.comparingInt(a -> a.tickCount));
            LOGGER.info("[dimensao] {} caçadoras na DATA; fica a mais nova, {} descartadas",
                    hunters.size(), hunters.size() - 1);
            for (int i = 1; i < hunters.size(); i++) hunters.get(i).discard();
            hunters.subList(1, hunters.size()).clear();
        }

        // ⚠️ TODO MUNDO CONTA — criativo E espectador.
        //
        // Filtrar jogador por modo de jogo aqui ja custou dois defeitos seguidos, e
        // pelo mesmo motivo nas duas vezes: os modos que eu descartava sao exatamente
        // os modos em que a pessoa entra na dimensao para OLHAR.
        //   - criativo: a fita da DATA so existe no criativo;
        //   - espectador: e o modo de observar a criatura sem ser atacado.
        // Nos dois casos o Diretor concluia "nao ha ninguem aqui", apagava a caçadora
        // viva e ainda apagava a que fosse invocada em seguida. A dimensao ficava
        // vazia justamente para quem estava tentando ve-la.
        //
        // Esta lista responde "ha alguem nesta dimensao?", e nao "quem pode ser
        // atacado?". Quem nao pode ser atacado o jogo ja resolve sozinho: o
        // `canBeSeenAsEnemy` ignora espectador, e o criativo da imunidade a dano e nao
        // invisibilidade. Nao e trabalho daqui repetir isso.
        List<ServerPlayer> players = new ArrayList<>(level.players());
        if (players.isEmpty()) {
            // Ninguem para caçar: recolhe. Ela nao fica andando num predio vazio.
            hunters.forEach(AnomalyEntity::discard);
            return;
        }

        RandomSource random = level.getRandom();
        ServerPlayer prey = players.get(random.nextInt(players.size()));

        if (hunters.isEmpty()) {
            spawn(level, prey, random);
            return;
        }

        AnomalyEntity hunter = hunters.get(0);
        long now = level.getGameTime();
        // Caçadora que o Diretor nao pos (veio do save ou de /summon) chega sem
        // relogio. Sem esta linha, `now - Long.MIN_VALUE` estoura o long e a conta de
        // "ha quanto tempo sem alvo" vira um numero negativo gigante.
        if (lastTargetAt == Long.MIN_VALUE) lastTargetAt = now;
        if (hunter.getTarget() != null) lastTargetAt = now;

        double nearest = players.stream()
                .mapToDouble(p -> p.distanceToSqr(hunter)).min().orElse(Double.MAX_VALUE);

        // Encalhada: faz tempo que nao ve ninguem — mas so troca de lugar se estiver
        // longe, senao ela evaporaria na cara de quem esta olhando para ela.
        boolean lost = now - lastTargetAt > LOST_AFTER && nearest > SAFE_DISTANCE * SAFE_DISTANCE;
        // Largada no outro canto do predio: nao adianta esperar, o jogador foi embora.
        boolean abandoned = nearest > 160.0D * 160.0D;

        if (lost || abandoned) {
            hunter.discard();
            spawn(level, prey, random);
        }
    }

    /**
     * Este Diretor vale nesta dimensao?
     *
     * Pergunta a tabela, e nao ao nome: das 21 planejadas, varias sao predio de
     * corredor com uma caçadora so, e todas elas querem exatamente este Diretor.
     * Cada uma entra pela linha `Director.HUNTER` do perfil, sem tocar aqui.
     */
    private static boolean isData(Level level) {
        return DimensionProfile.isDirectedBy(level, DimensionProfile.Director.HUNTER);
    }

    /**
     * Poe a caçadora a tantos passos do jogador, andando pelos corredores.
     *
     * Uma varredura em largura pelo chao, a partir de onde o jogador esta, colhendo
     * as celulas cuja distancia ANDADA cai na faixa. E a mesma coisa que o bicho vai
     * fazer depois para vir atras dele — so que feita uma vez, na hora de escolher,
     * em vez de descoberta tarde demais pela busca de caminho dele.
     */
    private static void spawn(ServerLevel level, ServerPlayer prey, RandomSource random) {
        BlockPos start = footing(level, prey);
        if (start == null) return;

        List<BlockPos> candidates = new ArrayList<>();
        Map<Long, Integer> seen = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.put(start.asLong(), 0);
        queue.add(start);

        while (!queue.isEmpty() && seen.size() < SCAN_CELLS) {
            BlockPos cell = queue.poll();
            int walked = seen.get(cell.asLong());
            if (walked >= SPAWN_MAX_PATH) continue;

            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos next = cell.relative(side);
                if (seen.containsKey(next.asLong()) || !walkable(level, next)) continue;
                seen.put(next.asLong(), walked + 1);
                queue.add(next);
                if (walked + 1 >= SPAWN_MIN_PATH) candidates.add(next);
            }
        }
        if (candidates.isEmpty()) return;

        // Fora de vista. Depois de 24 blocos de corredor quase sempre ja esta, mas o
        // "quase" e justamente o corredor reto e comprido — que e onde ela apareceria
        // do nada bem na frente do jogador.
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos where = candidates.get(random.nextInt(candidates.size()));
            if (canSee(level, prey, where)) continue;

            AnomalyEntity hunter = ModEntities.ANOMALY.get().create(level);
            if (hunter == null) return;
            hunter.moveTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(where), MobSpawnType.EVENT, null, null);
            hunter.setType(AnomalyType.GREYFACE);
            level.addFreshEntity(hunter);
            lastTargetAt = level.getGameTime();   // o relogio da recolocacao zera aqui
            // Sai uma vez por recolocacao (raro). E o que permite conferir no log que
            // ela nasceu, e a quantos passos, sem ter que adivinhar olhando o corredor.
            LOGGER.info("[dimensao] caçadora posta em {} {} {}, a {} passos do jogador",
                    where.getX(), where.getY(), where.getZ(), seen.get(where.asLong()));
            return;
        }
    }

    /**
     * Onde a varredura comeca: o chao sob o jogador.
     *
     * A DATA e PLANA — toda peca tem o piso no mesmo Y — entao o corredor esta sempre
     * em FLOOR_Y+1, e nao ha altura nenhuma para procurar. Se ele estiver voando no
     * criativo ou dentro da piscina da hub, procura a celula andavel mais proxima.
     */
    private static BlockPos footing(ServerLevel level, ServerPlayer prey) {
        int floor = DimLayout.FLOOR_Y + 1;
        BlockPos under = new BlockPos(Mth.floor(prey.getX()), floor, Mth.floor(prey.getZ()));
        if (walkable(level, under)) return under;
        for (int radius = 1; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos near = under.offset(dx, 0, dz);
                    if (walkable(level, near)) return near;
                }
            }
        }
        return null;
    }

    /** Da para ficar de pe aqui? Piso solido e dois blocos de ar. */
    private static boolean walkable(ServerLevel level, BlockPos foot) {
        if (!level.isLoaded(foot)) return false;
        var floor = level.getBlockState(foot.below());
        if (floor.isAir() || floor.is(Blocks.WATER)) return false;
        return level.getBlockState(foot).isAir() && level.getBlockState(foot.above()).isAir();
    }

    /** O jogador enxerga aquele ponto daqui? (a parede entre os dois conta) */
    private static boolean canSee(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos.above());
        return level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}

package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.entity.OphanimGaze;
import net.vhsworld.rec.init.ModEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UM OFANIM NO CEU DA CHUNKS. Nunca dois, nunca nenhum enquanto houver gente la.
 *
 * ⚠️ ELE NAO USA SPAWN NATURAL, e isso nao e escolha de estilo — e a lição que a DATA
 * ja cobrou uma vez. O teto de monstros do Minecraft e proporcional aos chunks
 * carregados e DIVIDIDO entre os monstros do bioma; num bioma onde a anomalia e o unico
 * monstro, ela sozinha enche o teto e a dimensao vira uma multidao a 1 fps. Pior aqui
 * do que la: sao treze blocos de cartaz por individuo. Spawn natural nao sabe dizer
 * "um"; ele so sabe encher.
 *
 * O QUE ESTE DIRETOR FAZ DE DIFERENTE DO DA DATA: la a caçadora nasce fora de vista, e
 * a graca e nao saber de onde ela vem. Aqui e o contrario — o Ofanim nasce A VISTA, do
 * outro lado do vazio, porque a mecanica dele so comeca quando voce o enxerga (ver
 * OphanimGaze). Uma presenca de treze blocos escondida atras de uma coluna nao esta
 * assustando ninguem.
 *
 * O PISO DE SILENCIO: depois de um julgamento ele fica fora por alguns segundos. Sem
 * isso ele voltaria no tick seguinte, e o castigo perderia o peso de ter acabado.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class OphanimDirector {

    private OphanimDirector() {}

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** De quanto em quanto o Diretor confere a cena. Nao precisa ser todo tick. */
    private static final int CHECK_EVERY = 40;

    /**
     * A que distancia ele nasce do jogador.
     *
     * Longe o bastante para caber o vazio entre voce e ele (a coluna vizinha esta a 32
     * blocos, entao 64 ja poe pelo menos duas quebras de chao no meio), e perto o
     * bastante para nao virar um ponto no horizonte. Treze blocos de altura a 96 de
     * distancia ainda ocupam um bom pedaco da tela.
     */
    private static final double SPAWN_MIN = 64.0D, SPAWN_MAX = 128.0D;

    /**
     * A faixa do CERCO — mais curta que a do solitario.
     *
     * Um cerco tem que ser lido como cerco no primeiro instante. A 128 blocos os tres
     * seriam pontos no horizonte, e o jogador levaria meio minuto para entender que
     * desta vez e diferente; a 48 eles ja chegam ocupando lados da tela.
     */
    private static final double SWARM_MIN = 48.0D, SWARM_MAX = 96.0D;

    /** Rumo minimo entre dois do cerco, em radianos (40 graus). Ver tooClose. */
    private static final double MIN_SEPARATION = Math.toRadians(40.0D);

    /**
     * Quanto ele sobe acima do topo da coluna em que nasce.
     *
     * Nao e enfeite: plantado no chao ele seria um obstaculo que se contorna. La em
     * cima ele deixa de ser coisa no caminho e vira CEU — voce nao esbarra nele, voce
     * levanta a cabeca e ele esta ali. E na CHUNKS levantar a cabeca e o que se faz o
     * tempo todo, procurando para onde pular.
     */
    private static final int HOVER = 22;

    /**
     * Quando o ultimo julgamento aconteceu em cada dimensao (o piso de silencio).
     *
     * ⚠️ A chave e a ResourceKey e NAO o ServerLevel. Guardar o nivel num campo
     * estatico seguraria o mundo inteiro na memoria depois de fechado — em
     * singleplayer, cada mundo aberto na sessao ficaria pendurado aqui para sempre.
     * A chave e uma constante interna do jogo e nao segura nada.
     */
    private static final Map<ResourceKey<Level>, Long> lastJudgement = new HashMap<>();

    /**
     * QUANTOS o Diretor esta segurando agora nesta dimensao. 1 quase sempre; o tamanho
     * do bando quando calhou de sair um.
     *
     * ⚠️ Nao vai para o disco. Se o servidor cair no meio de um cerco, ele volta valendo
     * 1 e a limpeza recolhe os outros dois na primeira conferida — o bando acaba junto
     * com a sessao. Preferi isso a salvar estado: um numero errado gravado num save
     * antigo faria a dimensao segurar tres Ofanins para sempre, e ninguem descobriria
     * de onde veio.
     */
    private static final Map<ResourceKey<Level>, Integer> cast = new HashMap<>();

    /** Avisado pelo OphanimGaze: acabou de julgar alguem, segura a proxima entrada. */
    public static void judged(ServerLevel level) {
        lastJudgement.put(level.dimension(), level.getGameTime());
        // O CERCO ACABA COM O PRIMEIRO JULGAMENTO. Sem isto os outros dois continuariam
        // vindo e cobrariam de novo em seguida, e o castigo escalado (que ja sobe
        // sozinho a cada vez) chegaria ao terceiro estagio no mesmo minuto. O elenco
        // volta a um, e a limpeza recolhe os que sobraram na proxima conferida.
        cast.put(level.dimension(), 1);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!OphanimGaze.isChunks(level)) return;
        if (level.getGameTime() % CHECK_EVERY != 0) return;
        if (!RECConfig.COMMON.ophanimInChunks.get()) return;

        List<AnomalyEntity> found = new ArrayList<>();
        for (AnomalyEntity anomaly : level.getEntities(ModEntities.ANOMALY.get(), a -> true)) {
            if (anomaly.type() == AnomalyType.OPHANIM) found.add(anomaly);
            else anomaly.discard();   // na CHUNKS so existe o Ofanim
        }

        // A LIMPEZA, e ela e igual a da DATA pelo mesmo motivo: a anomalia e
        // persistente, entao um mundo que ja tenha juntado varias (por /summon, por um
        // save antigo, por um defeito futuro) nao se limpa sozinho nem descarregando
        // chunk. Sobram as MAIS NOVAS — quem invoca uma quer ver AQUELA.
        //
        // O teto nao e mais "um": e o tamanho do elenco de agora, que vale 3 enquanto
        // um cerco esta em pe. O que continua valendo e que o numero e SEMPRE decidido
        // aqui, e nunca pelo spawn natural do jogo.
        int wanted = Math.max(1, cast.getOrDefault(level.dimension(), 1));
        if (found.size() > wanted) {
            found.sort(java.util.Comparator.comparingInt(a -> a.tickCount));
            LOGGER.info("[ofanim] {} na CHUNKS; ficam os {} mais novos, {} descartados",
                    found.size(), wanted, found.size() - wanted);
            for (int i = wanted; i < found.size(); i++) found.get(i).discard();
            found.subList(wanted, found.size()).clear();
        }

        // ⚠️ TODO MUNDO CONTA, criativo E espectador. Esta lista responde "ha alguem
        // nesta dimensao?", e nao "quem pode ser atacado?" — filtrar por modo de jogo
        // aqui ja custou dois defeitos na DATA, sempre pelo mesmo motivo: os modos
        // descartados sao exatamente aqueles em que a pessoa entrou para OLHAR.
        List<ServerPlayer> players = new ArrayList<>(level.players());
        if (players.isEmpty()) {
            found.forEach(AnomalyEntity::discard);
            lastJudgement.remove(level.dimension());
            cast.remove(level.dimension());
            return;
        }

        if (!found.isEmpty()) {
            // Largado do outro lado do mapa: recolhe e poe de novo. Sem isto, um
            // Ofanim que o jogador deixou para tras fica boiando sozinho e a dimensao
            // vira um passeio.
            //
            // No bando a conta e do mais PERTO de todos: enquanto um dos tres ainda
            // esta em cima do jogador, o cerco nao foi abandonado — recolher a esta
            // altura desmancharia o cerco no meio dele.
            double abandon = SPAWN_MAX * 2.0D;
            double nearest = Double.MAX_VALUE;
            for (AnomalyEntity ophanim : found) {
                for (ServerPlayer player : players) {
                    nearest = Math.min(nearest, player.distanceToSqr(ophanim));
                }
            }
            if (nearest > abandon * abandon) {
                found.forEach(AnomalyEntity::discard);
                cast.put(level.dimension(), 1);
            }
            return;
        }

        // O silencio depois do julgamento.
        Long judged = lastJudgement.get(level.dimension());
        int rest = (int) Math.round(RECConfig.COMMON.ophanimReturnSeconds.get() * 20.0D);
        if (judged != null && level.getGameTime() - judged < rest) return;

        RandomSource random = level.getRandom();
        ServerPlayer prey = players.get(random.nextInt(players.size()));

        // O CERCO: de vez em quando nao vem um, vem o bando.
        double chance = RECConfig.COMMON.ophanimSwarmChance.get();
        if (chance > 0.0D && random.nextDouble() < chance) {
            int size = RECConfig.COMMON.ophanimSwarmSize.get();
            int placed = surround(level, prey, random, size);
            // Se a planta so deu conta de um, ele entra como Ofanim comum: um "bando"
            // de um so nao pode ficar com a regra do bando ligada, senao ele andaria
            // sem nunca ser olhado (nao ha irmao para olhar por ele).
            cast.put(level.dimension(), Math.max(1, placed));
            if (placed > 1) {
                LOGGER.info("[ofanim] CERCO: {} postos em volta de {}",
                        placed, prey.getGameProfile().getName());
            }
            return;
        }

        cast.put(level.dimension(), 1);
        place(level, prey, random);
    }

    /**
     * O CERCO. Tres Ofanins ao mesmo tempo, um em cada lado do jogador.
     *
     * Eles nao nascem juntos num canto: nascem ESPALHADOS, um por setor de 120 graus em
     * volta dele. E o que transforma tres criaturas em uma situacao — com todos do
     * mesmo lado bastaria virar as costas, e a regra do bando (olhar para um faz os
     * tres andarem) nao teria com o que cobrar. Cercado, olhar para qualquer lado
     * alimenta alguem, e a unica saida continua sendo a mesma de sempre: nao olhar
     * para nenhum, e andar assim mesmo.
     *
     * Mais perto que o Ofanim solitario, de proposito. Um cerco tem que ser lido como
     * cerco no primeiro instante; a 128 blocos eles seriam tres pontos no horizonte, e
     * o jogador levaria meio minuto para entender que desta vez e diferente.
     *
     * @return quantos realmente couberam na planta
     */
    private static int surround(ServerLevel level, ServerPlayer prey, RandomSource random, int size) {
        if (!(level.getChunkSource().getGenerator() instanceof ChunksChunkGenerator generator)) return 0;

        // So as colunas do alcance do cerco. A distancia em 3D nunca e menor que a
        // distancia no plano, entao o que este raio corta ja seria cortado adiante.
        List<BlockPos> columns = generator.layout()
                .columnsAround(prey.blockPosition(), (int) Math.ceil(SWARM_MAX));
        if (columns.isEmpty()) return 0;

        double base = random.nextDouble() * Math.PI * 2.0D;
        double sector = Math.PI * 2.0D / size;

        List<BlockPos> chosen = new ArrayList<>(size);
        List<Double> taken = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            // Dois candidatos por setor: o melhor que o jogador NAO enxerga, e o melhor
            // de todos. O escondido ganha sempre que existir — tres coisas nascendo no
            // meio do campo de visao leem como falha de render, e nao como cerco. Mas
            // um cerco em volta do jogador quase nunca cabe todo atras de coluna, e
            // desistir do setor por isso deixaria o bando torto; entao, sem escondido,
            // vale o melhor angulo.
            BlockPos hidden = null, any = null;
            double hiddenOffset = Double.MAX_VALUE, anyOffset = Double.MAX_VALUE;
            double wanted = base + sector * i;

            for (BlockPos column : columns) {
                BlockPos where = column.above(HOVER);
                double distance = Math.sqrt(
                        where.distToCenterSqr(prey.getX(), prey.getY(), prey.getZ()));
                if (distance < SWARM_MIN || distance > SWARM_MAX) continue;
                if (chosen.contains(where)) continue;

                double angle = Math.atan2(where.getZ() + 0.5D - prey.getZ(),
                        where.getX() + 0.5D - prey.getX());

                // ⚠️ ESTA TRAVA E O QUE FAZ O CERCO SER UM CERCO. Sem ela, um setor sem
                // coluna no alcance pegava emprestado o melhor angulo de FORA do setor
                // — e ele frequentemente calhava na mesma direcao de outro ja escolhido,
                // so que mais longe. Dois dos tres saiam empilhados de um lado so e o
                // jogador virava as costas para os dois de uma vez.
                //
                // Medido em 300 plantas simuladas antes de abrir o jogo: sem a trava, a
                // pior separacao entre dois deles era ZERO grau. Prender cada um ao seu
                // setor conserta isso mas custa caro (cai de 292 para 210 cercos
                // completos em 300). Exigindo so a separacao minima, ficam 268 completos
                // e a pior separacao vai a 44 graus.
                if (tooClose(taken, angle)) continue;

                // Diferenca angular pelo caminho curto: sem o wrap, um alvo a 359 graus
                // pareceria estar a 358 de distancia de um a 1 grau.
                double offset = Math.abs(Math.IEEEremainder(angle - wanted, Math.PI * 2.0D));

                if (offset < anyOffset) {
                    anyOffset = offset;
                    any = where;
                }
                if (offset < hiddenOffset && !visible(level, prey, where)) {
                    hiddenOffset = offset;
                    hidden = where;
                }
            }

            BlockPos best = hidden != null ? hidden : any;
            if (best != null) {
                chosen.add(best);
                taken.add(Math.atan2(best.getZ() + 0.5D - prey.getZ(),
                        best.getX() + 0.5D - prey.getX()));
            }
        }

        for (BlockPos where : chosen) {
            spawn(level, where, chosen.size() > 1);
        }
        return chosen.size();
    }

    /**
     * Poe o Ofanim no ceu, sobre uma coluna distante — e de preferencia fora de vista.
     *
     * Fora de vista NAO e para esconder: e para ele nao PIPOCAR na frente do jogador. A
     * criatura tem que ja estar ali quando o olho chega nela; aparecer do nada no meio
     * do campo de visao le como bug de render, e nao como assombracao.
     *
     * O lugar sai da lista de colunas da propria planta, e nao de um sorteio no vazio:
     * assim ele nasce sempre sobre chao de verdade, e a silhueta dele fica emoldurada
     * por uma coluna em vez de boiar no nada.
     */
    private static void place(ServerLevel level, ServerPlayer prey, RandomSource random) {
        if (!(level.getChunkSource().getGenerator() instanceof ChunksChunkGenerator generator)) return;

        List<BlockPos> columns = generator.layout()
                .columnsAround(prey.blockPosition(), (int) Math.ceil(SPAWN_MAX));
        if (columns.isEmpty()) return;

        BlockPos fallback = null;
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos column = columns.get(random.nextInt(columns.size()));
            BlockPos where = column.above(HOVER);

            double distance = Math.sqrt(where.distToCenterSqr(prey.getX(), prey.getY(), prey.getZ()));
            if (distance < SPAWN_MIN || distance > SPAWN_MAX) continue;

            // O primeiro que serve de distancia ja vale como reserva: numa planta em
            // que quase tudo esta a vista (que e o ponto da CHUNKS), exigir "fora de
            // vista" pode nao ter resposta nenhuma, e sem reserva o Ofanim nunca
            // entraria na dimensao.
            if (fallback == null) fallback = where;
            if (visible(level, prey, where)) continue;

            spawn(level, where, false);
            return;
        }
        if (fallback != null) spawn(level, fallback, false);
    }

    private static void spawn(ServerLevel level, BlockPos where, boolean swarm) {
        AnomalyEntity ophanim = ModEntities.ANOMALY.get().create(level);
        if (ophanim == null) return;

        ophanim.moveTo(where.getX() + 0.5D, where.getY(), where.getZ() + 0.5D, 0.0F, 0.0F);
        ophanim.finalizeSpawn(level, level.getCurrentDifficultyAt(where), MobSpawnType.EVENT, null, null);
        ophanim.setType(AnomalyType.OPHANIM);
        // ⚠️ Sem isto ele desaba: o voo padrao dele e medido a partir do CHAO, e sobre
        // o vazio da CHUNKS nao ha chao — o mapa de alturas devolveria o fundo do
        // mundo e a criatura iria parar a 30 blocos do nada. Quem escolheu a altura
        // aqui fui eu, e o applyHover tem que respeitar isso.
        ophanim.setAnchored(true);
        ophanim.setSwarm(swarm);
        level.addFreshEntity(ophanim);

        LOGGER.info("[ofanim] posto em {} {} {}{}", where.getX(), where.getY(), where.getZ(),
                swarm ? " (cerco)" : "");
    }

    /**
     * Este rumo esta perto demais de algum que ja foi escolhido para o cerco?
     *
     * 40 graus foi o numero que o teste devolveu: menos que isso ja deixa dois deles
     * lendo como "do mesmo lado", e mais que isso comeca a recusar setores bons e o
     * cerco sai de dois em vez de tres.
     */
    private static boolean tooClose(List<Double> taken, double angle) {
        for (double other : taken) {
            if (Math.abs(Math.IEEEremainder(angle - other, Math.PI * 2.0D)) < MIN_SEPARATION) {
                return true;
            }
        }
        return false;
    }

    /** O jogador enxerga aquele ponto daqui? (coluna no meio do caminho conta) */
    private static boolean visible(ServerLevel level, ServerPlayer player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos);
        return level.clip(new ClipContext(eye, target, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}

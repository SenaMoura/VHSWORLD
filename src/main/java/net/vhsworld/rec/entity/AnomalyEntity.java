package net.vhsworld.rec.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.RECMod;
import org.jetbrains.annotations.Nullable;

/**
 * UMA ANOMALIA. Cartaz 2D que fica parado te olhando e nao encosta em voce.
 *
 * Ela nao ataca, nao empurra, nao leva dano e nao faz barulho. Nao e um monstro
 * mal balanceado — e uma PRESENCA, e o unico verbo dela e estar ali. Quem cobra o
 * preco e a fotografia: revelar a foto de uma delas custa sanidade, e esse caminho
 * ja existia inteiro no mod (o filtro do PhotoCapture so aceita entidade nossa).
 *
 * ELA ESTA SEMPRE NO MUNDO. O que muda de uma para outra e se os seus olhos a
 * alcancam — ver AnomalyType.Visibility e client/entity/AnomalyVision. Por isso ela
 * e intangivel: uma coisa invisivel que empurrasse o jogador seria bug, nao medo.
 *
 * Ela se desfaz se voce chegar perto demais, e se apaga sozinha depois de um tempo.
 * Anomalia que espera parada de ser examinada vira estatua de museu.
 *
 * ⚠️ Ela era um `Mob` puro enquanto NENHUMA andava. Virou `PathfinderMob` quando o
 * Cara Cinza passou a cacar: o MeleeAttackGoal e a navegacao de chao so existem nessa
 * classe. As presencas nao perdem nada — elas continuam sem goal nenhum.
 */
public class AnomalyEntity extends PathfinderMob implements Enemy {

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(AnomalyEntity.class, EntityDataSerializers.INT);

    /**
     * "Estou atras de alguem AGORA" — quem calcula e o servidor, quem usa e a tela.
     *
     * O cliente nao tem acesso ao alvo de um Mob (getTarget nao e sincronizado), e o
     * mod nao tem canal de rede proprio. Entao a bandeira viaja pelo SynchedEntityData,
     * do mesmo jeito que o surto do Homem de Pedra — e de graca e chega sozinha.
     * E o que liga a trilha da cacada em client/entity/CreatureAudio.
     */
    private static final EntityDataAccessor<Boolean> DATA_HUNTING =
            SynchedEntityData.defineId(AnomalyEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * O MEDIDOR DE OLHAR do Ofanim, de 0 a 1 — quem enche e o servidor, quem sente e
     * a tela.
     *
     * Viaja sincronizado pelo mesmo motivo da bandeira de cacada logo acima: o cliente
     * precisa saber o quanto ele ja tem sobre voce para a camera ir ficando torta e o
     * zumbido ir subindo ANTES do julgamento. Se so o estouro chegasse (pelo pacote),
     * o castigo viria sem aviso — e aviso e o que transforma castigo em mecanica.
     */
    private static final EntityDataAccessor<Float> DATA_GAZE =
            SynchedEntityData.defineId(AnomalyEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Ticks de carencia antes de ela poder se desfazer por proximidade (3s).
     *
     * E o que permite invocar uma para olhar de perto, e o que deixa o spawn natural
     * colocar uma logo atras de voce sem ela evaporar antes de existir.
     */
    private static final int GRACE = 60;

    /**
     * Raio de busca de caminho dentro do labirinto — MEDIDO, nao chutado.
     *
     * O jogo corta a busca por `walkedDistance`: o que conta e a distancia ANDADA, e
     * nao a linha reta (ver PathFinder.findPath). Com o padrao de 64 a caçadora nao
     * chegava — no labirinto, cobrir 22 a 44 blocos de linha reta custa de 33 a 216
     * blocos de caminho, medido em cinco sementes.
     *
     * Nao virou 256 por causa daquele 216: quem resolveu a cauda foi o Diretor, que
     * agora escolhe o ponto de nascimento pela distancia ANDADA (no maximo 56). O que
     * sobra para este numero e a folga para o jogador correr depois — 128 e o dobro
     * do pior nascimento. Fugir longe o bastante para ela perder o rastro e uma saida
     * legitima; o Diretor a recoloca depois.
     */
    private static final double MAZE_FOLLOW_RANGE = 128.0D;

    /**
     * Teto de nos visitados por busca.
     *
     * O teto cru sai de FOLLOW_RANGE x 16 fixado no construtor da navegacao — e ali o
     * tipo desta anomalia ainda nem existe, entao sairia sempre 1024, que trunca. Num
     * corredor a busca gasta um no por bloco de passagem: na DATA, tudo que esta a ate
     * 128 blocos de caminho da entre 6 e 10 mil celulas (medido). Este numero e o
     * TETO e nao o custo — o A* so chega perto dele quando nao ha caminho, e no
     * labirinto sempre ha (a planta e validada conexa).
     */
    private static final int MAZE_NODES = 12000;

    /**
     * Quanto tempo ela continua atras de voce depois de te perder de vista, no labirinto.
     *
     * O padrao do jogo sao 60 ticks: TRES SEGUNDOS. Num campo aberto isso e generoso —
     * perdeu de vista, e porque voce ja esta longe. Num predio feito so de esquinas,
     * virar a primeira ja quebra a linha de visao, e tres segundos depois ela para de
     * andar e fica plantada no corredor. A perseguicao acabaria antes de comecar, e
     * todo o trabalho de caminho no labirinto nao serviria para nada.
     *
     * Vinte segundos e o que faz virar a esquina NAO resolver. E, de quebra, e o que a
     * trilha da caçada precisa para valer alguma coisa: ouvir a musica continuar
     * depois de perder o bicho de vista e a melhor coisa que essa criatura tem.
     */
    private static final int MAZE_MEMORY = 400;

    /** A perseguicao, guardada para dar mais memoria a ela dentro do labirinto. */
    private NearestAttackableTargetGoal<Player> chase;

    private int life;

    /** A velocidade desta criatura ja foi gravada no atributo? (ver applySpeed) */
    private boolean speedApplied;

    /** Trava do passo: sem ela, correndo rapido os passos viram metralhadora. */
    private int stepRest;

    /** Ticks que o Ofanim ainda passa cego pelo clarao do flash (ver OphanimGaze). */
    private int burnTicks;

    /**
     * Foi um Diretor que escolheu a altura dela? Entao nao recalcule o voo.
     *
     * ⚠️ Sem esta trava o Ofanim da CHUNKS afundaria: o applyHover mede a altura a
     * partir do CHAO, e sobre o vazio entre as colunas nao ha chao nenhum — o mapa de
     * alturas devolve o fundo do mundo e a criatura nasceria a 30 blocos do nada, umas
     * cento e quarenta abaixo de onde o jogador anda.
     */
    private boolean anchored;

    /**
     * Ele veio EM BANDO? (o trio da CHUNKS — ver OphanimDirector)
     *
     * Persistente porque a regra do bando muda o comportamento dele, e um Ofanim que
     * voltasse do save sem saber que tinha dois irmaos pararia de acompanhar os outros
     * no meio do cerco.
     */
    private boolean swarm;

    /**
     * Alguem estava olhando para ele no ultimo tick.
     *
     * Transiente e so para o bando: e assim que um irmao sabe que OUTRO esta sendo
     * olhado. Nao vai para o NBT nem para a rede — quem precisa disto e o servidor, e
     * so por um tick.
     */
    private boolean watched;

    public AnomalyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.noPhysics = false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                // Os atributos valem para o TIPO inteiro, nao por criatura — nao ha
                // como dar velocidade so a cacadora. Quem e presenca simplesmente nao
                // recebe goal nenhum, entao nunca usa a velocidade que tem.
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    /**
     * ⚠️ Os goals sao registrados no CONSTRUTOR, e o tipo so chega depois (pelo
     * finalizeSpawn ou pelo NBT). Nao da para escolher aqui quais goals a criatura
     * tem — entao todos entram e cada um se recusa a rodar se a criatura nao for
     * cacadora. O canUse() e consultado a cada tick, quando o tipo ja existe.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return hunts() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return hunts() && super.canContinueToUse();
            }
        });

        this.chase = new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return hunts() && super.canUse();
            }
        };
        this.targetSelector.addGoal(1, this.chase);
    }

    /** Esta criatura caca? (o tipo dela decide; presenca nunca sai do lugar) */
    public boolean hunts() {
        return type().hunts();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE, 0);
        this.entityData.define(DATA_HUNTING, false);
        this.entityData.define(DATA_GAZE, 0.0F);
    }

    /** Ela esta atras de alguem agora? (bandeira sincronizada; a tela le isto) */
    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
    }

    /** Quanto do medidor de olhar ele ja tem sobre alguem, de 0 a 1. */
    public float gaze() {
        return this.entityData.get(DATA_GAZE);
    }

    public void setGaze(float value) {
        // Sincronizado so quando muda de verdade: o medidor anda de 0.006 por tick, e
        // mandar isso a cada tick para todo cliente perto seria trafego a toa.
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        if (Math.abs(clamped - gaze()) > 0.004F || clamped == 0.0F || clamped == 1.0F) {
            this.entityData.set(DATA_GAZE, clamped);
        }
    }

    public int burnTicks() {
        return this.burnTicks;
    }

    public void setBurnTicks(int ticks) {
        this.burnTicks = Math.max(0, ticks);
    }

    /** Diz que um Diretor escolheu a altura desta criatura (ver o campo anchored). */
    public void setAnchored(boolean value) {
        this.anchored = value;
    }

    /** Ele faz parte de um bando? */
    public boolean isSwarm() {
        return this.swarm;
    }

    public void setSwarm(boolean value) {
        this.swarm = value;
    }

    /** Alguem olhou para ele no ultimo tick? (so o bando usa isto) */
    public boolean wasWatched() {
        return this.watched;
    }

    public void setWatched(boolean value) {
        this.watched = value;
    }

    /**
     * Grava no atributo a velocidade DESTA criatura.
     *
     * ⚠️ O atributo vale para o EntityType inteiro, e as anomalias todas sao o mesmo
     * tipo — nao ha como dar velocidade a uma so na hora do registro. Cada individuo
     * ajusta a propria base, e isso acontece no primeiro tick e nao no construtor:
     * lá o tipo dela ainda nao chegou (vem do finalizeSpawn ou do NBT, depois).
     */
    private void applySpeed() {
        var attribute = getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        // A cacadora tem knob proprio: e o numero que o Pedro vai querer mexer
        // depois de correr dela uma vez. As presencas usam o valor do catalogo,
        // que elas nunca chegam a gastar.
        double speed = hunts()
                ? RECConfig.COMMON.greyfaceSpeed.get()
                : type().speed();
        attribute.setBaseValue(speed);
    }

    /**
     * Poe no ar quem nasceu para ficar no ar (hoje, o Ofanim: 30 blocos acima do chao).
     *
     * Roda no mesmo tiro unico do applySpeed, e pelo mesmo motivo: no construtor o tipo
     * da anomalia ainda nao chegou (ele vem do finalizeSpawn ou do NBT), entao nao ha
     * como saber ali se esta e uma criatura que voa.
     *
     * A altura e contada a partir do CHAO, e nao de onde a invocacao caiu — senao
     * `/summon` no alto de uma montanha e `/summon` num vale dariam alturas de voo
     * diferentes, e ela nasceria enterrada ou no espaco conforme o terreno.
     *
     * ⚠️ O teste de meio bloco importa: sem ele, recarregar o mundo reposicionaria a
     * criatura toda vez (a bandeira e transiente, o `life` e que persiste), e ela subiria
     * de novo a partir de onde ja estava.
     */
    private void applyHover() {
        if (!type().hovers()) return;

        // Sem isto ela cai: PathfinderMob nasce com gravidade, e presenca nao tem goal
        // nenhum para se segurar no ar.
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;

        // Posta por um Diretor: a altura dela ja foi escolhida por quem sabia onde
        // havia mundo. Medir de novo aqui so poderia estragar.
        if (this.anchored) return;

        int ground = this.level()
                .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition())
                .getY();
        // Sobre o VAZIO nao ha chao para medir, e o mapa de alturas devolve o fundo do
        // mundo. Invocada sobre um buraco da CHUNKS, ela desabaria para y=30 e sumiria
        // do jogo sem deixar rastro. Sem chao, fica onde esta.
        if (ground <= this.level().getMinBuildHeight()) return;

        double target = ground + type().hoverBlocks();
        if (Math.abs(getY() - target) > 0.5D) {
            teleportTo(getX(), target, getZ());
        }
    }

    // ------------------------------------------------------------------ tipo

    public AnomalyType type() {
        return AnomalyType.byIndex(this.entityData.get(DATA_TYPE));
    }

    public void setType(AnomalyType type) {
        this.entityData.set(DATA_TYPE, type.ordinal());
        // Trocou de tipo, trocou de velocidade: o proximo tick regrava o atributo.
        this.speedApplied = false;
    }

    @Override
    public Component getName() {
        return Component.translatable(type().translationKey());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnomalyType", type().id());
        tag.putInt("Life", this.life);
        tag.putBoolean("Anchored", this.anchored);
        tag.putBoolean("Swarm", this.swarm);
        tag.putInt("Burn", this.burnTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        AnomalyType type = AnomalyType.byId(tag.getString("AnomalyType"));
        if (type != null) setType(type);
        this.life = tag.getInt("Life");
        this.anchored = tag.getBoolean("Anchored");
        this.swarm = tag.getBoolean("Swarm");
        this.burnTicks = tag.getInt("Burn");
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        // Cada dimensao tem a SUA criatura. Na DATA e o Cara Cinza, e so ele: um
        // corredor fechado com uma coisa que caca de verdade vale mais que um
        // corredor com cinco coisas, das quais quatro so ficam paradas olhando.
        // Sortear ali dentro daria "hoje deu Ofanim" — e a dimensao mudaria de
        // assunto sem o jogador ter feito nada.
        AnomalyType forced = forcedTypeFor(level);
        if (forced != null) {
            setType(forced);
            return super.finalizeSpawn(level, difficulty, reason, data, tag);
        }

        // O ovo de spawn e o spawn natural sorteiam; quem veio de /summon com
        // AnomalyType no NBT ja chega decidido e o readAdditionalSaveData corrige.
        setType(roll(reason));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /**
     * Qual anomalia sai deste sorteio — e o elenco muda conforme QUEM pediu.
     *
     * O mundo aberto sorteia so entre as que aparecem sozinhas (ver AnomalyType.wild):
     * hoje isso deixa o Ofanim de fora, porque ele e o assunto da CHUNKS e encontra-lo
     * a toa num campo esvaziaria a dimensao inteira.
     *
     * ⚠️ O OVO E O /summon CONTINUAM SORTEANDO ENTRE TODAS. Sao ato deliberado de quem
     * esta testando, e nao "aparecer" — tirar o Ofanim dali tiraria junto o unico jeito
     * de olhar para ele fora da dimensao dele.
     */
    private AnomalyType roll(MobSpawnType reason) {
        // ⚠️ EVENT NAO ENTRA AQUI de proposito, apesar de ser codigo nosso que o usa
        // (os dois Diretores e o Apocalipse). Os tres escolhem o tipo na mao logo
        // depois do finalizeSpawn, entao o sorteio deles nem e usado — e no dia em que
        // alguem acrescentar um evento que NAO escolha, o certo e ele cair na regra do
        // mundo aberto e nao poder sortear o Ofanim por acidente.
        boolean deliberate = reason == MobSpawnType.SPAWN_EGG
                || reason == MobSpawnType.COMMAND;
        if (deliberate) {
            return AnomalyType.byIndex(this.random.nextInt(AnomalyType.values().length));
        }

        var pool = AnomalyType.wildTypes();
        // Elenco vazio (alguem marcou todas como nao-selvagens): devolve a primeira do
        // catalogo em vez de estourar um nextInt(0) no meio de um spawn.
        if (pool.isEmpty()) return AnomalyType.byIndex(0);
        return pool.get(this.random.nextInt(pool.size()));
    }

    /** O perfil da dimensao em que ela esta, ou null se ela nao esta numa nossa. */
    @Nullable
    private net.vhsworld.rec.worldgen.dim.DimensionProfile profile() {
        return net.vhsworld.rec.worldgen.dim.DimensionProfile.of(this.level());
    }

    /**
     * Esta criatura esta dentro de uma dimensao nossa de corredor fechado?
     *
     * Pergunta pelo FEITIO, e nao pelo nome: o faro de labirinto vale em qualquer
     * predio de esquinas, e as 21 planejadas tem varios (Labirinto, Escritorio,
     * Instalation, Tunnels...). Amarrar em "data" faria cada um deles precisar de uma
     * linha nova aqui.
     */
    public boolean inMaze() {
        return net.vhsworld.rec.worldgen.dim.DimensionProfile.isMold(
                this.level(), net.vhsworld.rec.worldgen.dim.DimensionProfile.Mold.MAZE);
    }

    /** Esta criatura esta no mundo picado, onde o Ofanim e o assunto? */
    public boolean inChunks() {
        return net.vhsworld.rec.worldgen.dim.DimensionProfile.isMold(
                this.level(), net.vhsworld.rec.worldgen.dim.DimensionProfile.Mold.ISLANDS);
    }

    /**
     * Ela e a criatura QUE UM DIRETOR MANTEM, e nao um encontro solto no mundo?
     *
     * Quem e dirigida nao obedece ao cronometro de vida nem some quando o jogador se
     * aproxima: essas duas regras existem para a anomalia do mundo aberto, que e um
     * ENCONTRO — aparece, pesa e vai embora. Numa dimensao onde ela e o assunto e ha
     * exatamente uma, sumir por conta propria seria abandonar a cena no meio.
     */
    public boolean isDirected() {
        var profile = profile();
        if (profile == null) return false;
        return switch (profile.director()) {
            case HUNTER -> hunts();
            case OPHANIM -> type() == AnomalyType.OPHANIM;
            case NONE -> false;
        };
    }

    /**
     * O FARO DE LABIRINTO. Sem isto a caçadora nao chega em voce dentro da DATA.
     *
     * Dois numeros do jogo atrapalham num predio de corredores, e os dois saem do
     * mesmo lugar (a distancia em LINHA RETA):
     *
     * 1. O alcance de busca do caminho e o FOLLOW_RANGE. Num campo aberto 64 blocos
     *    de raio e generoso; num labirinto, dar a volta em duas esquinas para chegar
     *    a 20 blocos de distancia ja gasta mais de 64 de caminho, e a busca desiste
     *    antes de sair da sala.
     * 2. O orcamento de nos visitados. A busca em corredor gasta no em cada bloco de
     *    passagem; com o orcamento de campo aberto ela para no meio do trajeto e a
     *    criatura fica andando contra a parede.
     *
     * O raio entra aqui, porque so agora se sabe o tipo. O teto de nos NAO da para
     * ajustar aqui: ele e fixado quando a navegacao nasce, la no construtor — por
     * isso ele vem de `createNavigation`, logo abaixo.
     */
    private void applyMazeSense() {
        if (!inMaze() || !hunts()) return;
        var follow = getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(MAZE_FOLLOW_RANGE);
        if (this.chase != null) this.chase.setUnseenMemoryTicks(MAZE_MEMORY);
    }

    /**
     * Navegacao com teto de nos alto o bastante para atravessar um labirinto.
     *
     * ⚠️ Isto TEM que ser aqui. `createNavigation` roda dentro do construtor de Mob,
     * antes de a anomalia saber que tipo ela e e em que dimensao esta — nao da para
     * decidir na hora. Entao o teto sobe para todas, e nao custa nada: quem nao caca
     * nao tem goal nenhum e nunca pede um caminho, e quem caca no mundo aberto e
     * limitado pelo raio (64), que corta a busca muito antes do teto.
     */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level) {
            @Override
            protected net.minecraft.world.level.pathfinder.PathFinder createPathFinder(int rawBudget) {
                this.nodeEvaluator = new net.minecraft.world.level.pathfinder.WalkNodeEvaluator();
                this.nodeEvaluator.setCanPassDoors(true);
                return new net.minecraft.world.level.pathfinder.PathFinder(
                        this.nodeEvaluator, Math.max(rawBudget, MAZE_NODES));
            }
        };
    }

    /**
     * A criatura que aquela dimensao SEMPRE cria, ou null onde vale o sorteio.
     *
     * Ficar preso a DIMENSAO (e nao ao bioma) e de proposito: e a dimensao que e o
     * assunto. Sortear o tipo la dentro daria "hoje deu Ofanim" e a dimensao mudaria
     * de assunto sozinha. Quem responde e o `DimensionProfile` — uma linha de tabela
     * por dimensao, em vez de um ramo de switch aqui.
     */
    @Nullable
    private static AnomalyType forcedTypeFor(ServerLevelAccessor level) {
        var profile = net.vhsworld.rec.worldgen.dim.DimensionProfile.of(level);
        return profile == null ? null : profile.anomaly();
    }

    // ------------------------------------------------------------------ o comportamento

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        this.life++;

        if (!this.speedApplied) {
            applySpeed();
            applyHover();
            applyMazeSense();
            this.speedApplied = true;
        }

        if (this.stepRest > 0) this.stepRest--;

        // A bandeira que liga a trilha: ela tem alvo e o alvo esta perto o bastante
        // para a perseguicao estar acontecendo de verdade. Note que NAO exijo linha
        // de visao — perder o bicho de vista com a musica tocando e o melhor momento
        // que esta criatura tem para oferecer.
        if (hunts()) {
            double reach = RECConfig.COMMON.greyfaceChaseRange.get();
            boolean chasing = getTarget() instanceof Player prey
                    && prey.isAlive()
                    && distanceToSqr(prey) < reach * reach;
            if (chasing != isHunting()) this.entityData.set(DATA_HUNTING, chasing);
        }

        // O OLHAR RECIPROCO. Toda a regra do Ofanim mora em OphanimGaze; aqui so o
        // gancho. Ele roda onde quer que o Ofanim esteja, e nao so na CHUNKS: a regra
        // e da criatura, nao da dimensao — quem topar com ele no mundo aberto encontra
        // a mesma coisa.
        //
        // ⚠️ Depois desta chamada ele pode ter se descartado (o julgamento o desfaz).
        // Nada que mexa nele pode vir depois sem conferir isso.
        if (type() == AnomalyType.OPHANIM) {
            OphanimGaze.tick(this);
            if (isRemoved()) return;
        }

        Player near = this.level().getNearestPlayer(this, 64.0D);
        if (near != null) {
            // Quem e PRESENCA encara voce sempre — o trabalho dela e ser vista, e uma
            // presenca de costas nao e nada. Isso gasta so uma das oito vistas, e tudo
            // bem: as outras sete existem para a CACADORA, que mantem o rumo da propria
            // caminhada e por isso aparece de lado e de costas enquanto te contorna.
            if (!hunts()) {
                faceTowards(near);
            }

            // Chegou perto demais: ela nao esta mais ali. Nunca deixe o jogador
            // alcancar uma anomalia — examinada de perto, ela vira um cartaz mesmo.
            //
            // ⚠️ Sem o periodo de graca isto some com ela no PRIMEIRO tick sempre que
            // ela nasce perto: /summon ~3 e um spawn natural atras de voce morriam na
            // hora, e sem deixar rastro nenhum no log. O sumico e para quem AVANCA,
            // e nao ha como avancar no tick em que a coisa apareceu.
            // ⚠️ A cacadora NAO se desfaz de perto. O sumico e a regra das presencas —
            // elas existem para nunca serem alcancadas. Aplicar isso a quem persegue
            // faria a criatura evaporar exatamente no instante em que ela te pega, que
            // e o unico instante que importa nela.
            // ⚠️ Quem e DIRIGIDA tambem escapa desta regra, e nao so quem caca. O
            // Ofanim vem na sua direcao de proposito: sumir ao encostar apagaria a
            // criatura exatamente no instante do julgamento — o unico instante em que
            // ela cobra alguma coisa. Quem resolve o "chegou perto demais" nela e o
            // proprio julgamento, que a desfaz depois de cobrar.
            double vanish = RECConfig.COMMON.anomalyVanishRange.get();
            if (!hunts() && !isDirected() && this.life > GRACE
                    && this.distanceToSqr(near) < vanish * vanish) {
                discard();
                return;
            }
        }

        // Nas dimensoes do mod quem manda na criatura e o Diretor, nao o cronometro.
        //
        // A regra dos 3 minutos existe para o mundo aberto, onde a anomalia e um
        // ENCONTRO: ela aparece, pesa, e vai embora. Na DATA e na CHUNKS ela e o
        // assunto da dimensao inteira, e ha exatamente uma. Deixar o cronometro valer
        // ali faria ela sumir no meio da perseguicao — e sumir na hora em que quase te
        // pega e o pior momento possivel para uma criatura de terror desaparecer.
        if (isDirected()) return;

        int lifetime = RECConfig.COMMON.anomalyLifetimeSeconds.get() * 20;
        if (this.life > lifetime) discard();
    }

    private void faceTowards(Player player) {
        Vec3 delta = player.position().subtract(this.position());
        float yaw = (float) (Math.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    // ------------------------------------------------------------------ intangivel

    /** Nao se mata. Arma nenhuma atravessa a fronteira entre bicho e anomalia. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // nao empurra ninguem
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * Nem a cacadora empurra o jogador — ela ATRAVESSA e bate.
     *
     * Um cartaz 2D empurrando corpo entrega na hora que ali nao ha volume nenhum, e
     * ficar preso numa parede de nada e o tipo de coisa que vira reclamacao. O dano
     * chega pelo golpe, nao pelo encosto.
     */

    /** Sem som proprio: o silencio dela e o que faz o resto do mundo parecer alto. */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    // ------------------------------------------------------------------ o passo

    /**
     * O PASSO ALTO da cacadora — e o silencio de todas as outras.
     *
     * Aproveito a cadencia do vanilla (um passo por bloco andado, contado no
     * `moveDist`) em vez de inventar uma: ela ja acompanha a velocidade, entao quanto
     * mais rapido ela vem, mais rapido os passos vem — que e exatamente a informacao
     * que o jogador precisa ouvir. Nao chamo o super: cartaz nao pisa em grama, e o
     * "toc" de bloco do vanilla entregaria que ali tem um bicho comum.
     *
     * Sai do SERVIDOR (playSound com jogador nulo) de proposito: passo pesado e coisa
     * que todo mundo na sala ouve, ao contrario do som de avistamento, que e do olho
     * de quem viu. E o volume passa de 1.0 porque em Minecraft volume alto tambem
     * significa alcance maior — e para ouvir de longe.
     */
    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState state) {
        // ⚠️ Preso ao Cara Cinza, e nao a "quem caca". O Vazio tambem caca agora, e este
        // passo e a assinatura sonora DELE — emprestar sairia como duas criaturas com o
        // mesmo pe. Fica mudo ate ter som proprio.
        if (type() != AnomalyType.GREYFACE || this.level().isClientSide) return;
        if (!RECConfig.COMMON.greyfaceSteps.get()) return;
        if (this.stepRest > 0) return;

        this.stepRest = 5;
        float volume = RECConfig.COMMON.greyfaceStepVolume.get().floatValue();
        this.level().playSound(null, getX(), getY(), getZ(), ModSounds.GREYFACE_STEP.get(),
                SoundSource.HOSTILE, volume,
                0.72F + this.random.nextFloat() * 0.10F);
    }

    /**
     * Neve, poo de neve e afins entram por outro caminho no vanilla e nao passam pelo
     * playStepSound. Sem isto, a cacadora andaria calada no gelo e "tec-tec" na neve.
     */
    @Override
    protected void playCombinationStepSounds(net.minecraft.world.level.block.state.BlockState primary,
                                             net.minecraft.world.level.block.state.BlockState secondary,
                                             net.minecraft.core.BlockPos primaryPos,
                                             net.minecraft.core.BlockPos secondaryPos) {
        playStepSound(primaryPos, primary);
    }

    @Override
    protected void playMuffledStepSound(net.minecraft.world.level.block.state.BlockState state,
                                        net.minecraft.core.BlockPos pos) {
        playStepSound(pos, state);
    }

    /**
     * A caixa que o jogo usa para decidir se a criatura cabe na tela.
     *
     * A caixa de COLISAO e pequena e continua pequena (1.0 x 2.6 — ver ModEntities).
     * Mas o Ofanim tem treze blocos de cartaz: com a caixa de dois e meio, ele
     * desapareceria da tela no instante em que o jogador olhasse para o proprio pe,
     * porque o centro dele sairia do campo de visao. Aqui a caixa cresce com o
     * DESENHO, e so para esse fim.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        AnomalyType type = type();
        double half = Math.max(0.5D, type.width() * 0.5D);
        return new net.minecraft.world.phys.AABB(
                getX() - half, getY(), getZ() - half,
                getX() + half, getY() + type.height(), getZ() + half);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }
}

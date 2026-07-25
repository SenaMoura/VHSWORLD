package net.vhsworld.rec.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.vhsworld.rec.menu.RFReceiverMenu;

/**
 * O Receptor de Frequencia: a bancada do mod.
 *
 * Feito no molde da mesa de trabalho do vanilla — sem BlockEntity, a grade e
 * transiente e vive dentro do menu. Botao direito abre a tela; olha para a direcao
 * em que foi colocado (a "frente" do aparelho, com a tela).
 */
public class RFReceiverBlock extends HorizontalDirectionalBlock {

    private static final Component TITLE = Component.translatable("container.recmod.rf_receiver");

    public RFReceiverBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (id, inv, p) -> new RFReceiverMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                TITLE);
    }
}

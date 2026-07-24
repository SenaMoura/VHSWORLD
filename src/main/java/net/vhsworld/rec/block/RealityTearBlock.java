package net.vhsworld.rec.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * O Rasgo da Realidade.
 *
 * Ele esta la o tempo todo e nao aparece nunca: nao tem modelo, nao tem colisao e
 * nao acende o contorno branco de mira. A unica coisa que o revela e o flash — e
 * so por alguns segundos.
 *
 * A CAIXA DE MIRA CONTINUA INTEIRA de proposito, mesmo sem colisao. Sem ela o
 * jogador nao teria como quebrar o bloco depois de descobrir onde ele esta, e o
 * rasgo viraria decoracao. Quem esconde o contorno e o lado do cliente, nao o
 * bloco: assim o servidor continua sabendo que ha algo ali para minerar.
 *
 * Nasce em cavernas, no ar — nunca dentro da pedra. Bloco invisivel encravado em
 * rocha vira um buraco visivel no meio da parede, que e o oposto do que ele e.
 */
public class RealityTearBlock extends Block {

    public RealityTearBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

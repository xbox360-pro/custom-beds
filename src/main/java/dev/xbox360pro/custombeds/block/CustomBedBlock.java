package dev.xbox360pro.custombeds.block;

import dev.xbox360pro.custombeds.data.DesignRef;
import dev.xbox360pro.custombeds.item.CustomBedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A real bed (you can sleep in it and set your spawn) whose look comes from an uploaded image.
 * It is its own block, so vanilla beds are never touched.
 */
public class CustomBedBlock extends BedBlock {

    public CustomBedBlock(Properties properties) {
        super(DyeColor.WHITE, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomBedBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack); // places the head half
        if (level.isClientSide) return;
        DesignRef design = CustomBedItem.getDesign(stack);
        if (design == null) return;
        BlockPos headPos = pos.relative(state.getValue(FACING));
        applyDesign(level, pos, design);
        applyDesign(level, headPos, design);
    }

    private static void applyDesign(Level level, BlockPos pos, DesignRef design) {
        if (level.getBlockEntity(pos) instanceof CustomBedBlockEntity be) {
            be.setDesign(design);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CustomBedBlockEntity be && be.getDesign() != null) {
            return CustomBedItem.create(be.getDesign());
        }
        return super.getCloneItemStack(level, pos, state);
    }
}

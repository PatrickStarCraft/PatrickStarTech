package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.common.block.LaserPipeBlock;


import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LaserPipeBlockItem extends PipeBlockItem {

    public LaserPipeBlockItem(PipeBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public LaserPipeBlock getBlock() {
        return (LaserPipeBlock) super.getBlock();
    }

}

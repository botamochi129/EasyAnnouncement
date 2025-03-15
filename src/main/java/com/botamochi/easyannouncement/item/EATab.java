package com.botamochi.easyannouncement.item;

import com.botamochi.easyannouncement.Easyannouncement;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class EATab {

    public static ItemGroup EA = FabricItemGroupBuilder.build(Easyannouncement.id("ea_tab"), () -> new ItemStack(Easyannouncement.EA_BLOCKITEM));

    public static void init() {

    }
}

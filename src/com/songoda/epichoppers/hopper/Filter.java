package com.songoda.epichoppers.hopper;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Filter {

    private List<ItemStack> whiteList = new ArrayList<>();
    private List<ItemStack> BlackList = new ArrayList<>();
    private List<ItemStack> voidList = new ArrayList<>();

    private Block endPoint;

    public List<ItemStack> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<ItemStack> whiteList) {
        this.whiteList = whiteList;
    }

    public List<ItemStack> getBlackList() {
        return BlackList;
    }

    public void setBlackList(List<ItemStack> blackList) {
        BlackList = blackList;
    }

    public List<ItemStack> getVoidList() {
        return voidList;
    }

    public void setVoidList(List<ItemStack> voidList) {
        this.voidList = voidList;
    }

    public Block getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Block endPoint) {
        this.endPoint = endPoint;
    }
}

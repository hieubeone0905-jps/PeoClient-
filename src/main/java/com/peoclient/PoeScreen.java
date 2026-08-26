
package com.peoclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class PoeScreen extends Screen {
    private int page=0;
    private TextFieldWidget blacklist;
    public PoeScreen(){super(Text.literal("PeoClient 1.21.4"));}

    @Override protected void init(){
        rebuild();
    }
    private void rebuild(){
        clearChildren();
        int x=width/2-150,y=35;
        addDrawableChild(ButtonWidget.builder(Text.literal("Xray: "+on(PeoClient.CFG.xray)),b->{PeoClient.CFG.xray=!PeoClient.CFG.xray;PeoClient.reload(client);rebuild();}).dimensions(x,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Nuker: "+on(PeoClient.CFG.nuker)),b->{PeoClient.CFG.nuker=!PeoClient.CFG.nuker;rebuild();}).dimensions(x+155,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fullbright: "+on(PeoClient.CFG.fullbright)),b->{PeoClient.CFG.fullbright=!PeoClient.CFG.fullbright;rebuild();}).dimensions(x,y+25,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("InventoryCleaner: "+on(PeoClient.CFG.cleaner)),b->{PeoClient.CFG.cleaner=!PeoClient.CFG.cleaner;rebuild();}).dimensions(x+155,y+25,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Xray settings"),b->{page=1;rebuild();}).dimensions(x,y+55,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Nuker settings"),b->{page=2;rebuild();}).dimensions(x+155,y+55,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fullbright settings"),b->{page=3;rebuild();}).dimensions(x,y+80,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cleaner settings"),b->{page=4;rebuild();}).dimensions(x+155,y+80,145,20).build());
        if(page==1)xrayPage(x,y+115);
        if(page==2)nukerPage(x,y+115);
        if(page==3)brightPage(x,y+115);
        if(page==4)cleanerPage(x,y+115);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save / Back"),b->{PeoClient.CFG.save();page=0;rebuild();}).dimensions(x,y+260,300,20).build());
    }
    private String on(boolean b){return b?"ON":"OFF";}
    private void xrayPage(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Fluids: "+on(PeoClient.CFG.xrayFluids)),b->{PeoClient.CFG.xrayFluids=!PeoClient.CFG.xrayFluids;PeoClient.reload(client);rebuild();}).dimensions(x,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Opacity: "+on(PeoClient.CFG.xrayOpacity)),b->{PeoClient.CFG.xrayOpacity=!PeoClient.CFG.xrayOpacity;PeoClient.reload(client);rebuild();}).dimensions(x+155,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Alpha: "+PeoClient.CFG.xrayAlpha),b->{PeoClient.CFG.xrayAlpha=PeoClient.CFG.xrayAlpha>=255?0:PeoClient.CFG.xrayAlpha+16;PeoClient.reload(client);rebuild();}).dimensions(x,y+25,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("HideSurface: "+on(PeoClient.CFG.xrayHideSurface)),b->{PeoClient.CFG.xrayHideSurface=!PeoClient.CFG.xrayHideSurface;PeoClient.reload(client);rebuild();}).dimensions(x+155,y+25,145,20).build());
    }
    private void nukerPage(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Mode: "+new String[]{"Normal","SurvMulti","Multi","Instant"}[PeoClient.CFG.nukerMode]),b->{PeoClient.CFG.nukerMode=(PeoClient.CFG.nukerMode+1)%4;rebuild();}).dimensions(x,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Multi: "+PeoClient.CFG.nukerMulti),b->{PeoClient.CFG.nukerMulti=PeoClient.CFG.nukerMulti>=10?1:PeoClient.CFG.nukerMulti+1;rebuild();}).dimensions(x+155,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Range: "+String.format("%.1f",PeoClient.CFG.nukerRange)),b->{PeoClient.CFG.nukerRange=PeoClient.CFG.nukerRange>=6?1:PeoClient.CFG.nukerRange+.5;rebuild();}).dimensions(x,y+25,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Shape: "+(PeoClient.CFG.nukerShape==0?"Cube":"Sphere")),b->{PeoClient.CFG.nukerShape^=1;rebuild();}).dimensions(x+155,y+25,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sort: "+new String[]{"Closest","Furthest","Softest","Hardest","None"}[PeoClient.CFG.nukerSort]),b->{PeoClient.CFG.nukerSort=(PeoClient.CFG.nukerSort+1)%5;rebuild();}).dimensions(x,y+50,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Filter: "+on(PeoClient.CFG.nukerFilter)),b->{PeoClient.CFG.nukerFilter=!PeoClient.CFG.nukerFilter;rebuild();}).dimensions(x+155,y+50,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Whitelist: "+on(PeoClient.CFG.nukerWhitelist)),b->{PeoClient.CFG.nukerWhitelist=!PeoClient.CFG.nukerWhitelist;rebuild();}).dimensions(x,y+75,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Raycast: "+on(PeoClient.CFG.nukerRaycast)),b->{PeoClient.CFG.nukerRaycast=!PeoClient.CFG.nukerRaycast;rebuild();}).dimensions(x+155,y+75,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Flatten: "+on(PeoClient.CFG.nukerFlatten)),b->{PeoClient.CFG.nukerFlatten=!PeoClient.CFG.nukerFlatten;rebuild();}).dimensions(x,y+100,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("NoParticles: "+on(PeoClient.CFG.nukerNoParticles)),b->{PeoClient.CFG.nukerNoParticles=!PeoClient.CFG.nukerNoParticles;rebuild();}).dimensions(x+155,y+100,145,20).build());
    }
    private void brightPage(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Mode: "+new String[]{"Table","Gamma","Potion"}[PeoClient.CFG.fullbrightMode]),b->{PeoClient.CFG.fullbrightMode=(PeoClient.CFG.fullbrightMode+1)%3;rebuild();}).dimensions(x,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Gamma: "+String.format("%.1f",PeoClient.CFG.fullbrightGamma)),b->{PeoClient.CFG.fullbrightGamma=PeoClient.CFG.fullbrightGamma>=12?1:PeoClient.CFG.fullbrightGamma+1;rebuild();}).dimensions(x+155,y,145,20).build());
    }
    private void cleanerPage(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Greedy: "+on(PeoClient.CFG.cleanerGreedy)),b->{PeoClient.CFG.cleanerGreedy=!PeoClient.CFG.cleanerGreedy;rebuild();}).dimensions(x,y,145,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Max Blocks: "+PeoClient.CFG.maxBlocks),b->{PeoClient.CFG.maxBlocks=PeoClient.CFG.maxBlocks>=2500?0:PeoClient.CFG.maxBlocks+64;rebuild();}).dimensions(x+155,y,145,20).build());
        blacklist=new TextFieldWidget(textRenderer,x,y+28,300,20,Text.literal("Blacklist item IDs"));
        blacklist.setText(PeoClient.CFG.itemsBlacklist); addDrawableChild(blacklist);
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply blacklist"),b->{PeoClient.CFG.itemsBlacklist=blacklist.getText();PeoClient.CFG.save();}).dimensions(x,y+53,300,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Hotbar defaults: Weapon/Bow/Pickaxe/Axe/None/Potion/Food/Block/Block"),b->{PeoClient.CFG.slotItems=new String[]{"WEAPON","BOW","PICKAXE","AXE","NONE","POTION","FOOD","BLOCK","BLOCK"};rebuild();}).dimensions(x,y+78,300,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Offhand: "+PeoClient.CFG.offHandItem),b->{PeoClient.CFG.offHandItem=PeoClient.CFG.offHandItem.equals("SHIELD")?"WEAPON":"SHIELD";rebuild();}).dimensions(x,y+103,145,20).build());
    }
    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){c.drawCenteredTextWithShadow(textRenderer,title,getWidth()/2,12,0xffffff);super.render(c,mouseX,mouseY,delta);}
    @Override public boolean shouldPause(){return false;}
}

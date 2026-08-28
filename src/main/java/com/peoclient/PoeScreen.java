package com.peoclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public class PoeScreen extends Screen {
    private TextFieldWidget search;
    private int tab=0;

    public PoeScreen(){super(Text.literal("PeoClient Hub"));}

    @Override protected void init(){
        clearChildren();
        search=new TextFieldWidget(textRenderer,20,18,220,20,Text.literal("Search hacks"));
        search.setMaxLength(40);
        search.setChangedListener(s->{});
        addDrawableChild(search);

        addDrawableChild(ButtonWidget.builder(Text.literal("Combat"),b->{tab=0;clearAndInit();}).dimensions(250,18,75,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Render"),b->{tab=1;clearAndInit();}).dimensions(330,18,75,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Player"),b->{tab=2;clearAndInit();}).dimensions(410,18,75,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),b->{tab=3;clearAndInit();}).dimensions(490,18,75,20).build());

        if(tab==3) settings();
        else modules();
    }

    protected void clearAndInit(){if(client!=null)client.setScreen(new PoeScreen());}

    private boolean match(String name){
        return search==null || search.getText().isBlank() ||
                name.toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT));
    }

    private void modules(){
        int x=20,y=55;
        if(tab==0 && match("Nuker")) module("Nuker",PeoClient.CFG.nuker,x,y,true);
        if(tab==1 && match("Xray")) module("Xray",PeoClient.CFG.xray,x,y,true);
        if(tab==1 && match("Fullbright")) module("Fullbright",PeoClient.CFG.fullbright,x,y+30,true);
        if(tab==2 && match("Inventory Cleaner")) module("Inventory Cleaner",PeoClient.CFG.cleaner,x,y,true);

        if(tab==0 && match("Nuker settings")) nukerSettings(x,y+65);
        if(tab==1 && match("Xray settings")) xraySettings(x,y+65);
        if(tab==1 && match("Fullbright settings")) brightSettings(x,y+120);
        if(tab==2 && match("Cleaner settings")) cleanerSettings(x,y+65);
    }

    private void module(String name,boolean state,int x,int y,boolean dummy){
        addDrawableChild(ButtonWidget.builder(Text.literal(name+"  ["+(state?"ON":"OFF")+"]"),
                b->{toggle(name);clearAndInit();}).dimensions(x,y,230,22).build());
    }
    private void toggle(String name){
        switch(name){
            case "Xray" -> {PeoClient.CFG.xray=!PeoClient.CFG.xray;PeoClient.reload(client);}
            case "Nuker" -> PeoClient.CFG.nuker=!PeoClient.CFG.nuker;
            case "Fullbright" -> PeoClient.CFG.fullbright=!PeoClient.CFG.fullbright;
            case "Inventory Cleaner" -> PeoClient.CFG.cleaner=!PeoClient.CFG.cleaner;
        }
        PeoClient.CFG.save();
    }

    private void xraySettings(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Hide fluids: "+PeoClient.CFG.xrayFluids),
                b->{PeoClient.CFG.xrayFluids=!PeoClient.CFG.xrayFluids;PeoClient.reload(client);clearAndInit();}).dimensions(x,y,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Hide surface: "+PeoClient.CFG.xrayHideSurface),
                b->{PeoClient.CFG.xrayHideSurface=!PeoClient.CFG.xrayHideSurface;clearAndInit();}).dimensions(x,y+25,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ore list: "+PeoClient.CFG.xrayBlocks.size()+" blocks"),
                b->{PeoClient.CFG.xrayBlocks.add("minecraft:trial_spawner");PeoClient.CFG.save();clearAndInit();}).dimensions(x,y+50,230,20).build());
    }

    private void nukerSettings(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Range: "+PeoClient.CFG.nukerRange),
                b->{PeoClient.CFG.nukerRange=PeoClient.CFG.nukerRange>=6?1:PeoClient.CFG.nukerRange+1;clearAndInit();}).dimensions(x,y,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Blocks/tick: "+PeoClient.CFG.nukerBlocksPerTick),
                b->{PeoClient.CFG.nukerBlocksPerTick=PeoClient.CFG.nukerBlocksPerTick>=4?1:PeoClient.CFG.nukerBlocksPerTick+1;clearAndInit();}).dimensions(x,y+25,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Raycast: "+PeoClient.CFG.nukerRaycast),
                b->{PeoClient.CFG.nukerRaycast=!PeoClient.CFG.nukerRaycast;clearAndInit();}).dimensions(x,y+50,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Auto rotate: "+PeoClient.CFG.nukerRotate),
                b->{PeoClient.CFG.nukerRotate=!PeoClient.CFG.nukerRotate;clearAndInit();}).dimensions(x,y+75,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Tool required: "+PeoClient.CFG.nukerOnlyWhenHoldingTool),
                b->{PeoClient.CFG.nukerOnlyWhenHoldingTool=!PeoClient.CFG.nukerOnlyWhenHoldingTool;clearAndInit();}).dimensions(x,y+100,230,20).build());
    }

    private void brightSettings(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Night vision: "+PeoClient.CFG.fullbrightNightVision),
                b->{PeoClient.CFG.fullbrightNightVision=!PeoClient.CFG.fullbrightNightVision;clearAndInit();}).dimensions(x,y,230,20).build());
    }

    private void cleanerSettings(int x,int y){
        addDrawableChild(ButtonWidget.builder(Text.literal("Action delay: "+PeoClient.CFG.cleanerActionDelay+" ticks"),
                b->{PeoClient.CFG.cleanerActionDelay=PeoClient.CFG.cleanerActionDelay>=10?1:PeoClient.CFG.cleanerActionDelay+1;clearAndInit();}).dimensions(x,y,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ack timeout: "+PeoClient.CFG.cleanerAckTimeout),
                b->{PeoClient.CFG.cleanerAckTimeout=PeoClient.CFG.cleanerAckTimeout>=20?6:PeoClient.CFG.cleanerAckTimeout+2;clearAndInit();}).dimensions(x,y+25,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Merge stacks: "+PeoClient.CFG.cleanerMergeStacks),
                b->{PeoClient.CFG.cleanerMergeStacks=!PeoClient.CFG.cleanerMergeStacks;clearAndInit();}).dimensions(x,y+50,230,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Touch hotbar: "+PeoClient.CFG.cleanerTouchHotbar),
                b->{PeoClient.CFG.cleanerTouchHotbar=!PeoClient.CFG.cleanerTouchHotbar;clearAndInit();}).dimensions(x,y+75,230,20).build());
    }

    private void settings(){
        addDrawableChild(ButtonWidget.builder(Text.literal("Xray key: "+PeoClient.xrayKey.getBoundKeyLocalizedText().getString()),b->openControls()).dimensions(20,55,260,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Nuker key: "+PeoClient.nukerKey.getBoundKeyLocalizedText().getString()),b->openControls()).dimensions(20,82,260,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fullbright key: "+PeoClient.fullbrightKey.getBoundKeyLocalizedText().getString()),b->openControls()).dimensions(20,109,260,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cleaner key: "+PeoClient.cleanerKey.getBoundKeyLocalizedText().getString()),b->openControls()).dimensions(20,136,260,22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Hub key: "+PeoClient.menuKey.getBoundKeyLocalizedText().getString()),b->openControls()).dimensions(20,163,260,22).build());
    }

    private void openControls(){
        if(client!=null)client.setScreen(new net.minecraft.client.gui.screen.option.ControlsOptionsScreen(this,client.options));
    }

    @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
        c.fill(0,0,width,height,0xB0101010);
        c.drawTextWithShadow(textRenderer,"PeoClient  •  1.21.4",20,5,0xFFFFFF);
        super.render(c,mouseX,mouseY,delta);
        c.drawTextWithShadow(textRenderer,"Right Shift = Hub",width-130,height-14,0xAAAAAA);
    }
    @Override public boolean shouldPause(){return false;}
}

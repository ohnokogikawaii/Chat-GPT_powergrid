package org.patryk3211.powergrid.electricity.converter;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.StabilizedPowerSupplyC2SPacket;

public class StabilizedPowerSupplyScreen
        extends AbstractContainerScreen<StabilizedPowerSupplyMenu> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 165;

    private EditBox voltageBox;
    private EditBox currentBox;

    private Button mpptButton;
    private Button applyButton;

    public StabilizedPowerSupplyScreen(
            StabilizedPowerSupplyMenu menu,
            Inventory inventory,
            Component title
    ) {

        super(
                menu,
                inventory,
                title
        );

        imageWidth =
                WIDTH;

        imageHeight =
                HEIGHT;
    }

    @Override
    protected void init() {

        super.init();

        int x =
                leftPos;

        int y =
                topPos;

        /*
         * 出力電圧
         */
        voltageBox =
                new EditBox(
                        font,
                        x + 115,
                        y + 30,
                        80,
                        18,
                        Component.literal(
                                "Output Voltage"
                        )
                );

        voltageBox.setValue(
                String.format(
                        "%.2f",
                        menu.contentHolder
                                .getTargetOutputVoltage()
                )
        );

        addRenderableWidget(
                voltageBox
        );

        /*
         * 電流制限
         */
        currentBox =
                new EditBox(
                        font,
                        x + 115,
                        y + 55,
                        80,
                        18,
                        Component.literal(
                                "Current Limit"
                        )
                );

        currentBox.setValue(
                String.format(
                        "%.2f",
                        menu.contentHolder
                                .getCurrentLimit()
                )
        );

        addRenderableWidget(
                currentBox
        );

        /*
         * MPPT ON/OFF
         */
        mpptButton =
                Button.builder(
                        getMpptText(),
                        button -> toggleMppt()
                ).bounds(
                        x + 115,
                        y + 5,
                        80,
                        20
                ).build();

        addRenderableWidget(
                mpptButton
        );

        /*
         * Apply
         */
        applyButton =
                Button.builder(
                        Component.literal(
                                "Apply"
                        ),
                        button -> applySettings()
                ).bounds(
                        x + 115,
                        y + 80,
                        80,
                        20
                ).build();

        addRenderableWidget(
                applyButton
        );
    }

    private Component getMpptText() {

        return Component.literal(
                "MPPT: "
                        +
                        (
                                menu.contentHolder
                                        .isMpptEnabled()
                                        ? "ON"
                                        : "OFF"
                        )
        );
    }

    private void toggleMppt() {

        boolean newState =
                !menu.contentHolder
                        .isMpptEnabled();

        menu.contentHolder
                .setMpptEnabled(
                        newState
                );

        mpptButton.setMessage(
                getMpptText()
        );

        sendSettings();
    }

    private void applySettings() {

        sendSettings();
    }

    private void sendSettings() {

        double voltage;

        double current;

        try {

            voltage =
                    Double.parseDouble(
                            voltageBox
                                    .getValue()
                    );

        } catch (NumberFormatException e) {

            voltage =
                    menu.contentHolder
                            .getTargetOutputVoltage();
        }

        try {

            current =
                    Double.parseDouble(
                            currentBox
                                    .getValue()
                    );

        } catch (NumberFormatException e) {

            current =
                    menu.contentHolder
                            .getCurrentLimit();
        }

        ModdedPackets.getChannel()
                .sendToServer(
                        new StabilizedPowerSupplyC2SPacket(
                                menu.contentHolder
                                        .getBlockPos(),
                                menu.contentHolder
                                        .isMpptEnabled(),
                                voltage,
                                current
                        )
                );
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {

        /*
         * 背景
         */
        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF202020
        );

        /*
         * タイトル
         */
        graphics.drawString(
                font,
                "Stabilized Power Supply",
                leftPos + 8,
                topPos + 7,
                0xFFFFFF
        );

        /*
         * 設定項目
         */
        graphics.drawString(
                font,
                "MPPT",
                leftPos + 8,
                topPos + 35,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                "Output Voltage",
                leftPos + 8,
                topPos + 35,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                "Current Limit",
                leftPos + 8,
                topPos + 60,
                0xFFFFFF
        );

        /*
         * 入力
         */
        graphics.drawString(
                font,
                String.format(
                        "INPUT  %.2f V  %.2f A  %.1f W",
                        menu.contentHolder
                                .getInputVoltage(),
                        menu.contentHolder
                                .getInputCurrent(),
                        menu.contentHolder
                                .getInputPower()
                ),
                leftPos + 8,
                topPos + 115,
                0xFFFFFF
        );

        /*
         * 出力
         */
        graphics.drawString(
                font,
                String.format(
                        "OUTPUT %.2f V  %.2f A  %.1f W",
                        menu.contentHolder
                                .getOutputVoltage(),
                        menu.contentHolder
                                .getOutputCurrent(),
                        menu.contentHolder
                                .getOutputPower()
                ),
                leftPos + 8,
                topPos + 130,
                0xFFFFFF
        );

        /*
         * MPPT最大電力
         */
        if (
                menu.contentHolder
                        .isMpptEnabled()
        ) {

            graphics.drawString(
                    font,
                    String.format(
                            "MPPT MAX %.1f W",
                            menu.contentHolder
                                    .getMpptMaximumPower()
                    ),
                    leftPos + 8,
                    topPos + 145,
                    0xFFFFFF
            );
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        renderBackground(
                graphics
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderTooltip(
                graphics,
                mouseX,
                mouseY
        );
    }
}
package org.patryk3211.powergrid.electricity.converter;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.network.packets.CommercialPowerConditionerPacket;

public class CommercialPowerConditionerScreen
        extends AbstractContainerScreen<CommercialPowerConditionerMenu> {

    private static final int WIDTH = 430;
    private static final int HEIGHT = 270;

    private Button gridTieButton;

    private EditBox outputVoltageBox;

    public CommercialPowerConditionerScreen(
            CommercialPowerConditionerMenu menu,
            Inventory inventory,
            Component title
    ) {

        super(
                menu,
                inventory,
                title
        );

        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {

        super.init();

        /*
         * =====================================================
         * 系統連系 ON/OFF
         * =====================================================
         */

        gridTieButton =
                Button.builder(
                                getGridTieButtonText(),
                                button -> {

                                    boolean enabled =
                                            !menu.isGridTieEnabled();

                                    sendSettings(
                                            enabled,
                                            menu.getManualOutputVoltage()
                                    );

                                    button.setMessage(
                                            enabled
                                                    ? Component.literal("GRID TIE: ON")
                                                    : Component.literal("GRID TIE: OFF")
                                    );
                                }
                        )
                        .bounds(
                                leftPos + 330,
                                topPos + 175,
                                90,
                                20
                        )
                        .build();

        addRenderableWidget(
                gridTieButton
        );


        /*
         * =====================================================
         * 手動出力電圧
         * =====================================================
         */

        outputVoltageBox =
                new EditBox(
                        font,
                        leftPos + 330,
                        topPos + 215,
                        90,
                        20,
                        Component.literal(
                                "Output Voltage"
                        )
                );

        outputVoltageBox.setValue(
                String.format(
                        "%.0f",
                        menu.getManualOutputVoltage()
                )
        );

        outputVoltageBox.setFilter(
                value -> {

                    if (value.isEmpty())
                        return true;

                    try {

                        double voltage =
                                Double.parseDouble(
                                        value
                                );

                        return voltage >=
                                CommercialPowerConditionerMenu.MIN_OUTPUT_VOLTAGE
                                &&
                                voltage <=
                                        CommercialPowerConditionerMenu.MAX_OUTPUT_VOLTAGE;

                    } catch (
                            NumberFormatException ignored
                    ) {

                        return false;
                    }
                }
        );

        addRenderableWidget(
                outputVoltageBox
        );
    }


    /*
     * =========================================================
     * GUI設定送信
     * =========================================================
     */

    private void sendSettings(
            boolean gridTieEnabled,
            double outputVoltage
    ) {

        if (menu.getBlockEntity() == null)
            return;

        double voltage =
                CommercialPowerConditionerMenu.clampOutputVoltage(
                        outputVoltage
                );

        CommercialPowerConditionerPacket packet =
                new CommercialPowerConditionerPacket(
                        menu.getBlockEntity().getBlockPos(),
                        gridTieEnabled,
                        voltage
                );

        ModdedPackets.sendToServer(
                packet
        );
    }


    /*
     * =========================================================
     * 系統連系ボタン文字
     * =========================================================
     */

    private Component getGridTieButtonText() {

        return menu.isGridTieEnabled()
                ? Component.literal("GRID TIE: ON")
                : Component.literal("GRID TIE: OFF");
    }


    /*
     * =========================================================
     * 背景
     * =========================================================
     */

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {

        /*
         * =====================================================
         * 背景
         * =====================================================
         */

        graphics.fill(
                leftPos,
                topPos,
                leftPos + imageWidth,
                topPos + imageHeight,
                0xFF202020
        );


        /*
         * =====================================================
         * タイトル
         * =====================================================
         */

        graphics.drawString(
                font,
                "Commercial Power Conditioner",
                leftPos + 8,
                topPos + 8,
                0xFFFFFF
        );


        /*
         * =====================================================
         * MPPT
         * =====================================================
         */

        int startX =
                leftPos + 8;

        int startY =
                topPos + 30;

        int columnWidth =
                82;

        for (int i = 0; i < 5; i++) {

            int x =
                    startX
                            +
                            i * columnWidth;

            renderMppt(
                    graphics,
                    i,
                    x,
                    startY
            );
        }


        /*
         * =====================================================
         * DC LINK
         * =====================================================
         */

        int dcLinkY =
                topPos + 165;

        graphics.drawString(
                font,
                "DC LINK",
                leftPos + 8,
                dcLinkY,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Voltage: %.2f V",
                        menu.getDcLinkVoltage()
                ),
                leftPos + 8,
                dcLinkY + 15,
                0xFFFFFF
        );


        /*
         * =====================================================
         * GRID
         * =====================================================
         */

        int gridY =
                topPos + 200;

        graphics.drawString(
                font,
                "GRID",
                leftPos + 8,
                gridY,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Voltage: %.2f V",
                        menu.getGridVoltage()
                ),
                leftPos + 8,
                gridY + 15,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Current: %.2f A",
                        menu.getGridCurrent()
                ),
                leftPos + 145,
                gridY + 15,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Power: %.1f W",
                        menu.getGridPower()
                ),
                leftPos + 280,
                gridY + 15,
                0xFFFFFF
        );


        /*
         * =====================================================
         * GRID STATUS
         * =====================================================
         */

        graphics.drawString(
                font,
                "Status: "
                        +
                        (
                                menu.isGridConnected()
                                        ? "CONNECTED"
                                        : "OFF"
                        ),
                leftPos + 8,
                gridY + 32,
                0xFFFFFF
        );


        /*
         * =====================================================
         * OUTPUT CONTROL
         * =====================================================
         */

        graphics.drawString(
                font,
                "OUTPUT CONTROL",
                leftPos + 250,
                topPos + 165,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Mode: %s",
                        menu.isGridTieEnabled()
                                ? "GRID TIE"
                                : "MANUAL"
                ),
                leftPos + 250,
                topPos + 190,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                String.format(
                        "Target: %.1f V",
                        menu.getTargetOutputVoltage()
                ),
                leftPos + 250,
                topPos + 240,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                "100 - 6600 V",
                leftPos + 250,
                topPos + 255,
                0xAAAAAA
        );
    }


    /*
     * =========================================================
     * MPPT表示
     * =========================================================
     */

    private void renderMppt(
            GuiGraphics graphics,
            int index,
            int x,
            int y
    ) {

        int textColor =
                0xFFFFFF;

        int currentY =
                y;


        /*
         * =====================================================
         * タイトル
         * =====================================================
         */

        graphics.drawString(
                font,
                "MPPT " + (index + 1),
                x,
                currentY,
                textColor
        );

        currentY += 15;


        /*
         * =====================================================
         * MAX
         * =====================================================
         */

        graphics.drawString(
                font,
                "MAX",
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.1f W",
                        menu.getMpptMaximumPower(index)
                ),
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.2f V",
                        menu.getMpptMaximumVoltage(index)
                ),
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.2f A",
                        menu.getMpptMaximumCurrent(index)
                ),
                x,
                currentY,
                textColor
        );

        currentY += 18;


        /*
         * =====================================================
         * NOW
         * =====================================================
         */

        graphics.drawString(
                font,
                "NOW",
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.1f W",
                        menu.getMpptPower(index)
                ),
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.2f V",
                        menu.getMpptVoltage(index)
                ),
                x,
                currentY,
                textColor
        );

        currentY += 12;

        graphics.drawString(
                font,
                String.format(
                        "%.2f A",
                        menu.getMpptCurrent(index)
                ),
                x,
                currentY,
                textColor
        );
    }


    /*
     * =========================================================
     * キー入力
     * =========================================================
     *
     * Enterを押したときに手動電圧を送信。
     */

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {

        if (
                keyCode == GLFW.GLFW_KEY_ENTER
                        ||
                        keyCode == InputConstants.KEY_NUMPADENTER
        ) {

            sendManualVoltage();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }


    /*
     * =========================================================
     * 手動電圧送信
     * =========================================================
     */

    private void sendManualVoltage() {

        if (outputVoltageBox == null)
            return;

        String value =
                outputVoltageBox.getValue();

        if (value.isEmpty())
            return;

        try {

            double voltage =
                    Double.parseDouble(
                            value
                    );

            voltage =
                    CommercialPowerConditionerMenu
                            .clampOutputVoltage(
                                    voltage
                            );

            outputVoltageBox.setValue(
                    String.format(
                            "%.0f",
                            voltage
                    )
            );

            sendSettings(
                    menu.isGridTieEnabled(),
                    voltage
            );

        } catch (
                NumberFormatException ignored
        ) {
            outputVoltageBox.setValue(
                    String.format(
                            "%.0f",
                            menu.getManualOutputVoltage()
                    )
            );
        }
    }


    /*
     * =========================================================
     * マウスクリック
     * =========================================================
     */

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {

        boolean result =
                super.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                );

        /*
         * 左クリックで電圧入力欄から
         * フォーカスが外れた場合にも送信。
         */

        if (
                outputVoltageBox != null
                        &&
                        outputVoltageBox.isFocused()
                        &&
                        !outputVoltageBox.isMouseOver(
                                mouseX,
                                mouseY
                        )
        ) {

            sendManualVoltage();
        }

        return result;
    }


    /*
     * =========================================================
     * Render
     * =========================================================
     */

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

